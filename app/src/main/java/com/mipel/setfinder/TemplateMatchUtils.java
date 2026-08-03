package com.mipel.setfinder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Compara siluetas binarias con IoU (más fiable que correlación cuando hay fondos distintos).
 */
public final class TemplateMatchUtils {

    private TemplateMatchUtils() {
    }

    public static int mejorIndice(Mat entrada, Mat[] plantillas) {
        int mejorIndice = 0;
        double mejorPuntuacion = -1.0;

        for (int i = 0; i < plantillas.length; i++) {
            double puntuacion = similitudSilueta(entrada, plantillas[i]);
            if (puntuacion > mejorPuntuacion) {
                mejorPuntuacion = puntuacion;
                mejorIndice = i;
            }
        }
        return mejorIndice;
    }

    public static int mejorIndiceRelleno(
            CardImageUtils.RasgosRelleno entrada,
            CardImageUtils.RasgosRelleno[] plantillas) {
        int mejorIndice = 0;
        double menorDistancia = Double.MAX_VALUE;

        for (int i = 0; i < plantillas.length; i++) {
            if (plantillas[i] == null) {
                continue;
            }
            double distancia = distanciaRelleno(entrada, plantillas[i]);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                mejorIndice = i;
            }
        }
        return mejorIndice;
    }

    /** IoU entre siluetas; prueba normal e invertida por si la polaridad difiere. */
    public static double similitudSilueta(Mat entrada, Mat plantilla) {
        if (entrada == null || plantilla == null || entrada.empty() || plantilla.empty()) {
            return -1.0;
        }

        Mat entradaAjustada = new Mat();
        Imgproc.resize(entrada, entradaAjustada, new Size(plantilla.cols(), plantilla.rows()));
        Imgproc.threshold(entradaAjustada, entradaAjustada, 127, 255, Imgproc.THRESH_BINARY);

        Mat plantillaBin = new Mat();
        Imgproc.threshold(plantilla.clone(), plantillaBin, 127, 255, Imgproc.THRESH_BINARY);

        double iouNormal = calcularIou(entradaAjustada, plantillaBin);

        Mat invertida = new Mat();
        Core.bitwise_not(plantillaBin, invertida);
        double iouInvertida = calcularIou(entradaAjustada, invertida);

        return Math.max(iouNormal, iouInvertida);
    }

    private static double calcularIou(Mat a, Mat b) {
        Mat interseccion = new Mat();
        Mat union = new Mat();
        Core.bitwise_and(a, b, interseccion);
        Core.bitwise_or(a, b, union);
        int pixelesInterseccion = Core.countNonZero(interseccion);
        int pixelesUnion = Core.countNonZero(union);
        if (pixelesUnion == 0) {
            return 0.0;
        }
        return pixelesInterseccion / (double) pixelesUnion;
    }

    private static double distanciaRelleno(
            CardImageUtils.RasgosRelleno a,
            CardImageUtils.RasgosRelleno b) {
        return Math.abs(a.densidadCentro - b.densidadCentro) * 4.0
                + Math.abs(a.densidadTotal - b.densidadTotal) * 2.0
                + Math.abs(a.densidadErosion - b.densidadErosion) * 3.0
                + Math.abs(a.oscilacionesFranjas - b.oscilacionesFranjas) * 0.6
                + Math.abs(a.picosFranjas - b.picosFranjas) * 2.5
                + Math.abs(a.variacionFilas - b.variacionFilas) * 12.0
                + Math.abs(a.ratioCierre - b.ratioCierre) * 1.5;
    }
}
