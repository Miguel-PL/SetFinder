package com.mipel.setfinder;

import org.opencv.core.Mat;

/**
 * Detecta forma mezclando plantillas (símbolo aislado) y análisis de contorno.
 */
public final class ShapeAnalyzer {

    public static final int OVAL = 0;
    public static final int ROMBO = 1;
    public static final int ONDA = 2;

    private ShapeAnalyzer() {
    }

    public static int detectar(Mat siluetaSimbolo, Mat[] plantillasForma) {
        SymbolAnalyzer.RasgosForma rasgos = SymbolAnalyzer.analizarForma(siluetaSimbolo);

        if (esProbableOnda(rasgos)) {
            return ONDA;
        }
        if (esProbableRombo(rasgos)) {
            return ROMBO;
        }
        if (esProbableOval(rasgos)) {
            return OVAL;
        }

        int porPlantilla = TemplateMatchUtils.mejorIndice(siluetaSimbolo, plantillasForma);
        int porContorno = detectarPorContorno(rasgos);

        if (porPlantilla == porContorno) {
            return porPlantilla;
        }
        if (porPlantilla == ROMBO || porContorno == ROMBO) {
            return ROMBO;
        }
        return porContorno;
    }

    private static boolean esProbableOnda(SymbolAnalyzer.RasgosForma r) {
        return r.defectosConvexos >= 2
                || (r.vertices >= 8 && r.circularidad < 0.45)
                || (r.solidez < 0.82 && r.defectosConvexos >= 1 && r.circularidad < 0.42);
    }

    private static boolean esProbableRombo(SymbolAnalyzer.RasgosForma r) {
        if (r.defectosConvexos >= 2) {
            return false;
        }
        if (r.verticesFinas >= 4 && r.verticesFinas <= 5) {
            return true;
        }
        return r.extent >= 0.42 && r.extent <= 0.58
                && r.circularidad >= 0.32 && r.circularidad <= 0.72;
    }

    private static boolean esProbableOval(SymbolAnalyzer.RasgosForma r) {
        return r.extent >= 0.58 && r.circularidad >= 0.46 && r.defectosConvexos <= 1;
    }

    private static int detectarPorContorno(SymbolAnalyzer.RasgosForma r) {
        int puntajeOval = 0;
        int puntajeRombo = 0;
        int puntajeOnda = 0;

        if (r.defectosConvexos >= 2) {
            puntajeOnda += 4;
        }
        if (r.vertices >= 8) {
            puntajeOnda += 3;
        }
        if (r.circularidad < 0.40) {
            puntajeOnda += 2;
        }
        if (r.solidez < 0.82) {
            puntajeOnda += 2;
        }

        if (r.verticesFinas >= 4 && r.verticesFinas <= 5) {
            puntajeRombo += 5;
        }
        if (r.extent >= 0.42 && r.extent <= 0.58) {
            puntajeRombo += 4;
        }
        if (r.vertices >= 4 && r.vertices <= 7 && r.defectosConvexos <= 2) {
            puntajeRombo += 2;
        }
        if (r.relacionAspecto >= 0.75 && r.relacionAspecto <= 1.8) {
            puntajeRombo += 1;
        }

        if (r.extent >= 0.58) {
            puntajeOval += 4;
        }
        if (r.circularidad >= 0.48) {
            puntajeOval += 3;
        }
        if (r.solidez >= 0.88) {
            puntajeOval += 2;
        }
        if (r.vertices <= 7 && r.defectosConvexos <= 1) {
            puntajeOval += 1;
        }

        if (puntajeOnda >= puntajeRombo && puntajeOnda >= puntajeOval) {
            return ONDA;
        }
        if (puntajeRombo >= puntajeOval) {
            return ROMBO;
        }
        return OVAL;
    }
}
