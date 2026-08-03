package com.mipel.setfinder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Aísla el símbolo principal de una máscara y lo centra en un cuadrado normalizado.
 */
public final class SymbolAnalyzer {

    public static final int TAMANO_SIMBOLO = 96;

    private SymbolAnalyzer() {
    }

    /** Recorta el blob más grande, lo centra en un canvas cuadrado y devuelve silueta binaria. */
    public static Mat siluetaSimbolo(Mat mascara) {
        Mat region = aislarRegionSimbolo(mascara);
        if (region == null || region.empty()) {
            return CardImageUtils.aSiluetaCuadrada(mascara, TAMANO_SIMBOLO);
        }
        return CardImageUtils.aSiluetaCuadrada(region, TAMANO_SIMBOLO);
    }

    public static CardImageUtils.RasgosRelleno rasgosDelSimbolo(Mat mascara) {
        Mat region = aislarRegionSimbolo(mascara);
        if (region == null || region.empty()) {
            return CardImageUtils.calcularRasgosRelleno(mascara);
        }
        return CardImageUtils.calcularRasgosRelleno(region);
    }

    public static Mat aislarRegionSimbolo(Mat mascara) {
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(mascara.clone(), contornos, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint mayor = null;
        double maxArea = mascara.rows() * mascara.cols() * 0.002;
        for (MatOfPoint contorno : contornos) {
            double area = Imgproc.contourArea(contorno);
            if (area > maxArea && (mayor == null || area > Imgproc.contourArea(mayor))) {
                mayor = contorno;
            }
        }

        if (mayor == null) {
            return null;
        }

        Rect rect = Imgproc.boundingRect(mayor);
        int padX = Math.max(2, rect.width / 8);
        int padY = Math.max(2, rect.height / 8);
        int x1 = Math.max(0, rect.x - padX);
        int y1 = Math.max(0, rect.y - padY);
        int x2 = Math.min(mascara.cols(), rect.x + rect.width + padX);
        int y2 = Math.min(mascara.rows(), rect.y + rect.height + padY);
        if (x2 <= x1 || y2 <= y1) {
            return null;
        }
        return mascara.submat(y1, y2, x1, x2).clone();
    }

    /** Rasgos geométricos del contorno del símbolo aislado. */
    public static class RasgosForma {
        public final int defectosConvexos;
        public final int vertices;
        public final int verticesFinas;
        public final double circularidad;
        public final double relacionAspecto;
        public final double solidez;
        /** Área del contorno / área del rectángulo envolvente (rombo ~0.5, óvalo ~0.6+). */
        public final double extent;

        public RasgosForma(int defectosConvexos, int vertices, int verticesFinas,
                           double circularidad, double relacionAspecto, double solidez,
                           double extent) {
            this.defectosConvexos = defectosConvexos;
            this.vertices = vertices;
            this.verticesFinas = verticesFinas;
            this.circularidad = circularidad;
            this.relacionAspecto = relacionAspecto;
            this.solidez = solidez;
            this.extent = extent;
        }
    }

    public static RasgosForma analizarForma(Mat siluetaSimbolo) {
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(siluetaSimbolo.clone(), contornos, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contornos.isEmpty()) {
            return new RasgosForma(0, 0, 0, 0, 1, 0, 0);
        }

        MatOfPoint mayor = contornos.get(0);
        double maxArea = Imgproc.contourArea(mayor);
        for (MatOfPoint c : contornos) {
            double area = Imgproc.contourArea(c);
            if (area > maxArea) {
                maxArea = area;
                mayor = c;
            }
        }

        double area = Imgproc.contourArea(mayor);
        MatOfPoint2f curva = new MatOfPoint2f(mayor.toArray());
        double perimetro = Imgproc.arcLength(curva, true);
        Rect rect = Imgproc.boundingRect(mayor);

        double circularidad = perimetro > 0
                ? 4 * Math.PI * area / (perimetro * perimetro) : 0;
        double relacionAspecto = rect.width > 0
                ? rect.height / (double) rect.width : 1;

        MatOfInt hullIdx = new MatOfInt();
        Imgproc.convexHull(mayor, hullIdx, false);
        Point[] puntos = mayor.toArray();
        int[] indices = hullIdx.toArray();
        Point[] hullPts = new Point[indices.length];
        for (int i = 0; i < indices.length; i++) {
            hullPts[i] = puntos[indices[i]];
        }
        double areaHull = Imgproc.contourArea(new MatOfPoint(hullPts));
        double solidez = areaHull > 0 ? area / areaHull : 0;

        MatOfPoint2f aprox = new MatOfPoint2f();
        Imgproc.approxPolyDP(curva, aprox, 0.04 * perimetro, true);
        int vertices = (int) aprox.total();

        MatOfPoint2f aproxFina = new MatOfPoint2f();
        Imgproc.approxPolyDP(curva, aproxFina, 0.02 * perimetro, true);
        int verticesFinas = (int) aproxFina.total();

        double extent = rect.width > 0 && rect.height > 0
                ? area / (rect.width * (double) rect.height) : 0;

        int defectos = contarDefectosConvexos(mayor);

        return new RasgosForma(defectos, vertices, verticesFinas, circularidad,
                relacionAspecto, solidez, extent);
    }

    private static int contarDefectosConvexos(MatOfPoint contorno) {
        if (contorno.total() < 4) {
            return 0;
        }
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contorno, hull, false);
        if (hull.total() < 3) {
            return 0;
        }
        try {
            MatOfInt4 defects = new MatOfInt4();
            Imgproc.convexityDefects(contorno, hull, defects);
            int[] arr = defects.toArray();
            int profundos = 0;
            for (int i = 0; i < arr.length; i += 4) {
                if (arr[i + 3] > 600) {
                    profundos++;
                }
            }
            return profundos;
        } catch (Exception e) {
            return 0;
        }
    }
}
