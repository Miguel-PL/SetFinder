package com.mipel.setfinder;

/**
 * Clasifica relleno comparando con las 3 plantillas de referencia (umbrales calibrados).
 */
public final class FillCalibrator {

    public static final int SOLIDO = 0;
    public static final int RAYADO = 1;
    public static final int VACIO = 2;

    private final CardImageUtils.RasgosRelleno refSolido;
    private final CardImageUtils.RasgosRelleno refRayado;
    private final CardImageUtils.RasgosRelleno refVacio;

    private final double umbralSolidoRayado;
    private final double umbralRayadoVacio;

    public FillCalibrator(CardImageUtils.RasgosRelleno[] referencias) {
        refSolido = referencias[SOLIDO];
        refRayado = referencias[RAYADO];
        refVacio = referencias[VACIO];

        umbralSolidoRayado = (refSolido.densidadCentro + refRayado.densidadCentro) / 2.0;
        umbralRayadoVacio = (refRayado.densidadCentro + refVacio.densidadCentro) / 2.0;
    }

    public int clasificar(CardImageUtils.RasgosRelleno entrada) {
        if (esSolido(entrada)) {
            return SOLIDO;
        }
        if (esRayado(entrada)) {
            return RAYADO;
        }
        if (esVacio(entrada)) {
            return VACIO;
        }
        return vecinoMasCercano(entrada);
    }

    private boolean esSolido(CardImageUtils.RasgosRelleno e) {
        return e.densidadCentro >= umbralSolidoRayado
                && e.densidadErosion >= refRayado.densidadErosion * 0.8
                && e.picosFranjas <= refRayado.picosFranjas + 1;
    }

    private boolean esRayado(CardImageUtils.RasgosRelleno e) {
        int picosMin = Math.max(2, refVacio.picosFranjas + 1);
        double varMin = refVacio.variacionFilas * 1.2;

        if (e.picosFranjas >= picosMin && e.variacionFilas >= varMin) {
            return true;
        }
        if (e.picosFranjas >= Math.max(2, (int) (refRayado.picosFranjas * 0.45))
                && e.variacionFilas >= refRayado.variacionFilas * 0.35) {
            return true;
        }
        if (e.oscilacionesFranjas >= Math.max(4, refVacio.oscilacionesFranjas + 3)) {
            return true;
        }
        // Más relleno que vacío pero sin densidad de sólido; patrón interno visible
        if (e.densidadTotal >= refVacio.densidadTotal * 1.2
                && e.densidadCentro < umbralSolidoRayado
                && e.picosFranjas >= 2
                && e.variacionFilas > refVacio.variacionFilas) {
            return true;
        }
        if (e.ratioCierre >= 1.12 && e.ratioCierre <= 2.8
                && e.picosFranjas >= 2 && e.densidadCentro < umbralSolidoRayado) {
            return true;
        }
        return distancia(e, refRayado) < distancia(e, refVacio) * 0.85
                && e.picosFranjas >= refVacio.picosFranjas;
    }

    private boolean esVacio(CardImageUtils.RasgosRelleno e) {
        if (e.picosFranjas >= 2 && e.variacionFilas > refVacio.variacionFilas * 1.1) {
            return false;
        }
        return e.densidadCentro <= umbralRayadoVacio
                || (e.densidadCentro < refRayado.densidadCentro * 0.75
                && e.picosFranjas <= refRayado.picosFranjas * 0.5);
    }

    private int vecinoMasCercano(CardImageUtils.RasgosRelleno entrada) {
        double dSolido = distancia(entrada, refSolido);
        double dRayado = distancia(entrada, refRayado);
        double dVacio = distancia(entrada, refVacio);

        if (dSolido <= dRayado && dSolido <= dVacio) {
            return SOLIDO;
        }
        if (dRayado <= dVacio) {
            return RAYADO;
        }
        return VACIO;
    }

    private double distancia(CardImageUtils.RasgosRelleno a, CardImageUtils.RasgosRelleno b) {
        return Math.abs(a.densidadCentro - b.densidadCentro) * 5.0
                + Math.abs(a.densidadErosion - b.densidadErosion) * 3.0
                + Math.abs(a.densidadTotal - b.densidadTotal) * 1.5
                + Math.abs(a.oscilacionesFranjas - b.oscilacionesFranjas) * 0.6
                + Math.abs(a.picosFranjas - b.picosFranjas) * 2.5
                + Math.abs(a.variacionFilas - b.variacionFilas) * 12.0
                + Math.abs(a.ratioCierre - b.ratioCierre) * 1.5;
    }
}
