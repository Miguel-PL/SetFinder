package com.mipel.setfinder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Detecta relleno en tres fases: vacío (solo contorno) → sólido → rayado.
 * La clave es la transformada de distancia: el contorno vacío no tiene píxeles
 * lejos del borde; rayado y sólido sí.
 */
public final class FillDetector {

    public static final int SOLIDO = 0;
    public static final int RAYADO = 1;
    public static final int VACIO = 2;

    private static final double UMBRAL_DIST_VACIO = 2.6;
    private static final double UMBRAL_FRACCION_VACIO = 0.10;

    private FillDetector() {
    }

    public static int detectar(Mat mascara) {
        Mat roi = SymbolAnalyzer.aislarRegionSimbolo(mascara);
        if (roi == null || roi.empty()) {
            roi = mascara;
        }
        return detectarEnRegion(roi);
    }

    private static int detectarEnRegion(Mat roi) {
        if (roi.cols() < 4 || roi.rows() < 4) {
            return SOLIDO;
        }

        double densidadCentro = densidadCentro(roi);
        double densidadErosion = densidadErosionada(roi);
        double maxDist = maxDistancia(roi);
        double fraccionProfunda = fraccionPixelesProfundos(roi, 2.0);

        // Fase 1: vacío = solo contorno fino, sin masa interior
        if (esVacio(maxDist, fraccionProfunda, densidadCentro, densidadErosion)) {
            return VACIO;
        }

        // Fase 2: sólido = interior denso y resiste erosión
        if (densidadCentro > 0.26 && densidadErosion > 0.11) {
            return SOLIDO;
        }
        if (fraccionProfunda > 0.55 && densidadErosion > 0.09) {
            return SOLIDO;
        }

        // Fase 3: rayado = hay relleno interior pero no es sólido
        if (esRayado(roi, maxDist, fraccionProfunda, densidadCentro)) {
            return RAYADO;
        }

        if (densidadCentro < 0.18) {
            return RAYADO;
        }
        return SOLIDO;
    }

    private static boolean esVacio(double maxDist, double fraccionProfunda,
                                     double densidadCentro, double densidadErosion) {
        if (maxDist < UMBRAL_DIST_VACIO && fraccionProfunda < UMBRAL_FRACCION_VACIO) {
            return true;
        }
        return densidadCentro < 0.07 && densidadErosion < 0.04 && fraccionProfunda < 0.12;
    }

    private static boolean esRayado(Mat roi, double maxDist, double fraccionProfunda,
                                    double densidadCentro) {
        if (maxDist < UMBRAL_DIST_VACIO) {
            return false;
        }
        if (CardImageUtils.tieneFranjas(roi)) {
            return true;
        }
        if (picosEnFilasTrasCierre(roi) >= 2) {
            return true;
        }
        return fraccionProfunda >= UMBRAL_FRACCION_VACIO
                && fraccionProfunda < 0.50
                && densidadCentro < 0.26;
    }

    private static double maxDistancia(Mat roi) {
        Mat dist = new Mat();
        Imgproc.distanceTransform(roi, dist, Imgproc.DIST_L2, 3);
        Core.MinMaxLocResult mm = Core.minMaxLoc(dist);
        return mm.maxVal;
    }

    /** Fracción de píxeles del símbolo alejados del borde (dist >= umbral). */
    private static double fraccionPixelesProfundos(Mat roi, double umbralDist) {
        int total = Core.countNonZero(roi);
        if (total == 0) {
            return 0;
        }
        Mat dist = new Mat();
        Imgproc.distanceTransform(roi, dist, Imgproc.DIST_L2, 3);
        Mat profundos = new Mat();
        Core.compare(dist, new Scalar(umbralDist), profundos, Core.CMP_GE);
        Core.bitwise_and(profundos, roi, profundos);
        return Core.countNonZero(profundos) / (double) total;
    }

    private static int picosEnFilasTrasCierre(Mat roi) {
        Mat cerrada = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 3));
        Imgproc.morphologyEx(roi, cerrada, Imgproc.MORPH_CLOSE, kernel);
        return picosEnFilas(cerrada);
    }

    private static int picosEnFilas(Mat region) {
        if (region.rows() < 3) {
            return 0;
        }
        double[] densidades = new double[region.rows()];
        double max = 0;
        for (int y = 0; y < region.rows(); y++) {
            densidades[y] = Core.countNonZero(region.row(y)) / (double) region.cols();
            max = Math.max(max, densidades[y]);
        }
        if (max < 0.04) {
            return 0;
        }
        double umbral = max * 0.22;
        int picos = 0;
        int sep = Math.max(2, region.rows() / 12);
        int ultimo = -sep;
        for (int y = 1; y < densidades.length - 1; y++) {
            if (densidades[y] >= umbral
                    && densidades[y] >= densidades[y - 1]
                    && densidades[y] >= densidades[y + 1]
                    && y - ultimo >= sep) {
                picos++;
                ultimo = y;
            }
        }
        return picos;
    }

    private static double densidadCentro(Mat roi) {
        int mx = Math.max(1, roi.cols() / 6);
        int my = Math.max(1, roi.rows() / 6);
        Mat centro = roi.submat(my, roi.rows() - my, mx, roi.cols() - mx);
        if (centro.empty()) {
            return 0;
        }
        return Core.countNonZero(centro) / (double) (centro.rows() * centro.cols());
    }

    private static double densidadErosionada(Mat roi) {
        Mat erosionada = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.erode(roi, erosionada, kernel, new Point(-1, -1), 2);
        return Core.countNonZero(erosionada) / (double) (roi.rows() * roi.cols());
    }
}
