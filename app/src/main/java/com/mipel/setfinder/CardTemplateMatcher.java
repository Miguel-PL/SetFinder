package com.mipel.setfinder;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * Reconocimiento por plantillas: color y forma automáticos; cantidad y relleno manual.
 */
public final class CardTemplateMatcher {

    private CardTemplateMatcher() {
    }

    public static List<Carta> reconocerTodas(List<Bitmap> recortes, TemplateLibrary biblioteca) {
        List<Carta> cartas = new ArrayList<>();
        for (Bitmap recorte : recortes) {
            cartas.add(reconocer(recorte, biblioteca));
        }
        return cartas;
    }

    public static Carta reconocer(Bitmap recorte, TemplateLibrary biblioteca) {
        Bitmap normalizado = CardImageUtils.normalizar(recorte);
        Mat hsv = CardImageUtils.aHsv(normalizado);

        int color = CardColorDetector.detectar(hsv);
        Mat mascara = CardImageUtils.mascaraColor(hsv, color);
        Mat siluetaSimbolo = SymbolAnalyzer.siluetaSimbolo(mascara);

        int forma = ShapeAnalyzer.detectar(siluetaSimbolo, biblioteca.getFormas());
        int numero = 1;
        int relleno = 0;

        return new Carta(
                CardImageUtils.clamp(numero, 1, 3),
                color,
                CardImageUtils.clamp(forma, 0, 2),
                CardImageUtils.clamp(relleno, 0, 2)
        );
    }
}
