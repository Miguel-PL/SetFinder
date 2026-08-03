package com.mipel.setfinder;

import org.opencv.core.Core;
import org.opencv.core.Mat;

/**
 * Detecta el color dominante (rojo / verde / morado) en HSV.
 */
public final class CardColorDetector {

    private CardColorDetector() {
    }

    public static int detectar(Mat hsv) {
        int[] pixeles = new int[3];
        pixeles[CardImageUtils.COLOR_ROJO] = Core.countNonZero(
                CardImageUtils.crearMascaraPorColor(hsv, CardImageUtils.COLOR_ROJO));
        pixeles[CardImageUtils.COLOR_VERDE] = Core.countNonZero(
                CardImageUtils.crearMascaraPorColor(hsv, CardImageUtils.COLOR_VERDE));
        pixeles[CardImageUtils.COLOR_MORADO] = Core.countNonZero(
                CardImageUtils.crearMascaraPorColor(hsv, CardImageUtils.COLOR_MORADO));

        int mejor = CardImageUtils.COLOR_ROJO;
        for (int i = 1; i < 3; i++) {
            if (pixeles[i] > pixeles[mejor]) {
                mejor = i;
            }
        }
        return mejor;
    }
}
