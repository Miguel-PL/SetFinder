package com.mipel.setfinder;

import android.graphics.Bitmap;

import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Recorta regiones rectangulares de una imagen y las ordena en lectura (filas, izquierda a derecha).
 */
public class CardCropper {

    /**
     * Ordena las regiones como se leen en la foto: primero por fila (arriba → abajo),
     * luego por columna (izquierda → derecha).
     */
    public static List<Rect> ordenarRegiones(List<Rect> regiones) {
        if (regiones.isEmpty()) {
            return regiones;
        }

        List<Rect> ordenadas = new ArrayList<>(regiones);
        int alturaMedia = 0;
        for (Rect rect : ordenadas) {
            alturaMedia += rect.height;
        }
        alturaMedia /= ordenadas.size();
        final int umbralFila = Math.max(alturaMedia / 2, 1);

        Collections.sort(ordenadas, new Comparator<Rect>() {
            @Override
            public int compare(Rect a, Rect b) {
                int centroYa = a.y + a.height / 2;
                int centroYb = b.y + b.height / 2;
                if (Math.abs(centroYa - centroYb) > umbralFila) {
                    return Integer.compare(centroYa, centroYb);
                }
                return Integer.compare(a.x, b.x);
            }
        });

        return ordenadas;
    }

    /** Recorta cada región de la imagen fuente y devuelve un Bitmap independiente por carta. */
    public static List<Bitmap> recortar(Bitmap fuente, List<Rect> regiones) {
        List<Bitmap> recortes = new ArrayList<>();
        for (Rect region : regiones) {
            Bitmap recorte = recortarUno(fuente, region);
            if (recorte != null) {
                recortes.add(recorte);
            }
        }
        return recortes;
    }

    private static Bitmap recortarUno(Bitmap fuente, Rect region) {
        // Recorte ligeramente interior para evitar mesa/beige en los bordes del contorno
        int recorteBordeX = Math.max(1, region.width / 12);
        int recorteBordeY = Math.max(1, region.height / 12);
        int x = Math.max(0, region.x + recorteBordeX);
        int y = Math.max(0, region.y + recorteBordeY);
        int ancho = Math.min(region.width - 2 * recorteBordeX, fuente.getWidth() - x);
        int alto = Math.min(region.height - 2 * recorteBordeY, fuente.getHeight() - y);

        if (ancho <= 0 || alto <= 0) {
            return null;
        }

        return Bitmap.createBitmap(fuente, x, y, ancho, alto);
    }
}
