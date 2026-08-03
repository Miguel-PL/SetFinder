package com.mipel.setfinder;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reconocimiento heurístico: color y forma automáticos; cantidad y relleno manual.
 */
public class CardRecognizer {

    private static final int ANCHO_NORMALIZADO = 180;
    private static final int MARGEN_INTERIOR_PCT = 10;

    private static final int COLOR_ROJO = 0;
    private static final int COLOR_VERDE = 1;
    private static final int COLOR_MORADO = 2;

    public static List<Carta> reconocerTodas(List<Bitmap> recortes) {
        List<Carta> cartas = new ArrayList<>();
        for (Bitmap recorte : recortes) {
            cartas.add(reconocer(recorte));
        }
        return cartas;
    }

    public static Carta reconocer(Bitmap recorte) {
        Bitmap normalizado = normalizar(recorte);

        Mat rgba = new Mat();
        Utils.bitmapToMat(normalizado, rgba);

        Mat bgr = new Mat();
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);

        int color = detectarColorDominante(hsv);

        // Máscara cruda: conserva contorno fino (vacío) y franjas (rayado)
        Mat mascaraRaw = crearMascaraPorColor(hsv, color);
        mascaraRaw = recortarMargenInterior(mascaraRaw, MARGEN_INTERIOR_PCT);
        Mat kernelOpen = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(mascaraRaw, mascaraRaw, Imgproc.MORPH_OPEN, kernelOpen);

        // Máscara solo para contar símbolos: cierra franjas verticales dentro de cada símbolo
        Mat mascaraConteo = mascaraRaw.clone();
        Mat kernelRayas = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 3));
        Imgproc.morphologyEx(mascaraConteo, mascaraConteo, Imgproc.MORPH_CLOSE, kernelRayas);

        List<Rect> simbolosAgrupados = agruparSimbolosVerticales(mascaraConteo);

        int forma = 0;
        if (!simbolosAgrupados.isEmpty()) {
            forma = votarForma(mascaraRaw, simbolosAgrupados);
        }

        return new Carta(
                1,
                color,
                clamp(forma, 0, 2),
                0
        );
    }

    private static Bitmap normalizar(Bitmap recorte) {
        int alto = Math.round(recorte.getHeight() * ((float) ANCHO_NORMALIZADO / recorte.getWidth()));
        alto = Math.max(alto, 1);
        return Bitmap.createScaledBitmap(recorte, ANCHO_NORMALIZADO, alto, true);
    }

    private static Mat recortarMargenInterior(Mat mascara, int margenPct) {
        int mx = mascara.cols() * margenPct / 100;
        int my = mascara.rows() * margenPct / 100;
        if (mx * 2 >= mascara.cols() || my * 2 >= mascara.rows()) {
            return mascara;
        }
        return mascara.submat(my, mascara.rows() - my, mx, mascara.cols() - mx).clone();
    }

    private static int detectarColorDominante(Mat hsv) {
        int[] pixeles = new int[3];
        pixeles[COLOR_ROJO] = Core.countNonZero(crearMascaraPorColor(hsv, COLOR_ROJO));
        pixeles[COLOR_VERDE] = Core.countNonZero(crearMascaraPorColor(hsv, COLOR_VERDE));
        pixeles[COLOR_MORADO] = Core.countNonZero(crearMascaraPorColor(hsv, COLOR_MORADO));

        int mejor = COLOR_ROJO;
        for (int i = 1; i < 3; i++) {
            if (pixeles[i] > pixeles[mejor]) {
                mejor = i;
            }
        }
        return mejor;
    }

    private static Mat crearMascaraPorColor(Mat hsv, int color) {
        Mat mascara = new Mat();
        switch (color) {
            case COLOR_ROJO: {
                Mat rojo1 = new Mat();
                Mat rojo2 = new Mat();
                Core.inRange(hsv, new Scalar(0, 50, 60), new Scalar(14, 255, 255), rojo1);
                Core.inRange(hsv, new Scalar(165, 50, 60), new Scalar(180, 255, 255), rojo2);
                Core.bitwise_or(rojo1, rojo2, mascara);
                break;
            }
            case COLOR_VERDE:
                Core.inRange(hsv, new Scalar(35, 40, 40), new Scalar(90, 255, 255), mascara);
                break;
            case COLOR_MORADO:
                Core.inRange(hsv, new Scalar(108, 25, 30), new Scalar(155, 255, 255), mascara);
                break;
            default:
                Core.inRange(hsv, new Scalar(0, 0, 0), new Scalar(180, 255, 255), mascara);
                break;
        }
        return mascara;
    }

    /**
     * Agrupa blobs en la misma columna vertical (símbolos SET apilados).
     * Devuelve un Rect por símbolo lógico.
     */
    private static List<Rect> agruparSimbolosVerticales(Mat mascara) {
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(mascara.clone(), contornos, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double areaMin = mascara.rows() * mascara.cols() * 0.003;
        List<Rect> rects = new ArrayList<>();
        for (MatOfPoint contorno : contornos) {
            if (Imgproc.contourArea(contorno) >= areaMin) {
                rects.add(Imgproc.boundingRect(contorno));
            }
        }

        Collections.sort(rects, Comparator.comparingInt(r -> r.y));

        List<Rect> grupos = new ArrayList<>();
        Rect actual = null;
        for (Rect rect : rects) {
            if (actual == null || !mismaColumna(actual, rect)) {
                grupos.add(rect);
                actual = rect;
            } else {
                actual = unirRects(actual, rect);
                grupos.set(grupos.size() - 1, actual);
            }
        }
        return grupos;
    }

    private static boolean mismaColumna(Rect a, Rect b) {
        int izq = Math.max(a.x, b.x);
        int der = Math.min(a.x + a.width, b.x + b.width);
        int solape = der - izq;
        return solape > Math.min(a.width, b.width) * 0.30;
    }

    /**
     * Forma con contorno crudo (sin cierre morfológico):
     * - Onda: muchos defectos de convexidad (curvas de la ola).
     * - Rombo: ~4 esquinas y relación de aspecto moderada.
     * - Óvalo: pocas esquinas y contorno muy compacto.
     */
    private static int detectarFormaEnRegion(Mat mascaraRaw, Rect region) {
        Mat roi = recortarRegionSegura(mascaraRaw, region);
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(roi.clone(), contornos, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint mayor = mayorContorno(contornos);
        if (mayor == null) {
            return 0;
        }

        double area = Imgproc.contourArea(mayor);
        MatOfPoint2f curva = new MatOfPoint2f(mayor.toArray());
        double perimetro = Imgproc.arcLength(curva, true);
        if (perimetro <= 0) {
            return 0;
        }

        Rect rect = Imgproc.boundingRect(mayor);
        double aspecto = rect.height / (double) Math.max(rect.width, 1);
        double circularidad = 4 * Math.PI * area / (perimetro * perimetro);

        MatOfPoint2f aprox = new MatOfPoint2f();
        Imgproc.approxPolyDP(curva, aprox, 0.04 * perimetro, true);
        int vertices = (int) aprox.total();

        int defectos = contarDefectosConvexos(mayor);

        int puntajeOnda = 0;
        int puntajeRombo = 0;
        int puntajeOval = 0;

        if (defectos >= 2) {
            puntajeOnda += 4;
        }
        if (vertices >= 7) {
            puntajeOnda += 3;
        }
        if (circularidad < 0.42) {
            puntajeOnda += 2;
        }

        if (vertices >= 4 && vertices <= 6 && defectos <= 2) {
            puntajeRombo += 3;
        }
        if (aspecto >= 0.75 && aspecto <= 1.8 && circularidad >= 0.38 && circularidad <= 0.72) {
            puntajeRombo += 2;
        }

        if (circularidad >= 0.50 && defectos <= 1) {
            puntajeOval += 3;
        }
        if (vertices <= 6 && aspecto >= 1.0) {
            puntajeOval += 2;
        }

        if (puntajeOnda > puntajeRombo && puntajeOnda > puntajeOval) {
            return 2;
        }
        if (puntajeRombo > puntajeOval) {
            return 1;
        }
        return 0;
    }

    private static int contarDefectosConvexos(MatOfPoint contorno) {
        if (contorno.rows() < 4) {
            return 0;
        }
        MatOfInt hull = new MatOfInt();
        Imgproc.convexHull(contorno, hull, false);
        if (hull.rows() < 3) {
            return 0;
        }

        MatOfInt4 defects = new MatOfInt4();
        try {
            Imgproc.convexityDefects(contorno, hull, defects);
        } catch (Exception e) {
            return 0;
        }

        int[] arr = defects.toArray();
        int profundos = 0;
        for (int i = 0; i < arr.length; i += 4) {
            if (arr[i + 3] > 800) {
                profundos++;
            }
        }
        return profundos;
    }

    private static int votarForma(Mat mascaraRaw, List<Rect> simbolos) {
        int[] votos = new int[3];
        for (Rect region : simbolos) {
            votos[detectarFormaEnRegion(mascaraRaw, region)]++;
        }
        return indiceMaximo(votos);
    }

    private static int indiceMaximo(int[] votos) {
        int mejor = 0;
        for (int i = 1; i < votos.length; i++) {
            if (votos[i] > votos[mejor]) {
                mejor = i;
            }
        }
        return mejor;
    }

    private static Mat recortarRegionSegura(Mat mascara, Rect region) {
        int x1 = clamp(region.x, 0, mascara.cols() - 1);
        int y1 = clamp(region.y, 0, mascara.rows() - 1);
        int x2 = clamp(region.x + region.width, x1 + 1, mascara.cols());
        int y2 = clamp(region.y + region.height, y1 + 1, mascara.rows());
        return mascara.submat(y1, y2, x1, x2).clone();
    }

    private static MatOfPoint mayorContorno(List<MatOfPoint> contornos) {
        MatOfPoint mayor = null;
        double maxArea = 0;
        for (MatOfPoint c : contornos) {
            double area = Imgproc.contourArea(c);
            if (area > maxArea) {
                maxArea = area;
                mayor = c;
            }
        }
        return mayor;
    }

    private static Rect unirRects(Rect a, Rect b) {
        int x1 = Math.min(a.x, b.x);
        int y1 = Math.min(a.y, b.y);
        int x2 = Math.max(a.x + a.width, b.x + b.width);
        int y2 = Math.max(a.y + a.height, b.y + b.height);
        return new Rect(x1, y1, x2 - x1, y2 - y1);
    }

    private static int clamp(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }
}
