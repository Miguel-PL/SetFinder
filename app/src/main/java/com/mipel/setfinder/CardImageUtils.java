package com.mipel.setfinder;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades compartidas: normalizar recortes y obtener máscaras de color.
 */
public final class CardImageUtils {

    public static final int ANCHO_NORMALIZADO = 180;
    public static final int MARGEN_INTERIOR_PCT = 10;

    public static final int COLOR_ROJO = 0;
    public static final int COLOR_VERDE = 1;
    public static final int COLOR_MORADO = 2;

    public static final int ANCHO_SILUETA = 120;
    public static final int ALTO_SILUETA = 160;

    private CardImageUtils() {
    }

    public static Bitmap normalizar(Bitmap recorte) {
        int alto = Math.round(recorte.getHeight() * ((float) ANCHO_NORMALIZADO / recorte.getWidth()));
        alto = Math.max(alto, 1);
        return Bitmap.createScaledBitmap(recorte, ANCHO_NORMALIZADO, alto, true);
    }

    public static Mat aHsv(Bitmap bitmap) {
        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);
        Mat bgr = new Mat();
        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV);
        return hsv;
    }

    public static Mat mascaraColor(Mat hsv, int color) {
        Mat mascara = crearMascaraPorColor(hsv, color);
        mascara = recortarMargenInterior(mascara, MARGEN_INTERIOR_PCT);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(mascara, mascara, Imgproc.MORPH_OPEN, kernel);
        return mascara;
    }

    /** Convierte la máscara de símbolos a silueta binaria con tamaño fijo para comparar plantillas. */
    public static Mat aSiluetaNormalizada(Mat mascara) {
        Mat silueta = new Mat();
        Imgproc.resize(mascara, silueta, new Size(ANCHO_SILUETA, ALTO_SILUETA));
        Imgproc.threshold(silueta, silueta, 127, 255, Imgproc.THRESH_BINARY);
        return silueta;
    }

    /** Centra el contenido en un canvas cuadrado y devuelve silueta binaria. */
    public static Mat aSiluetaCuadrada(Mat mascara, int tamano) {
        Mat bin = new Mat();
        Imgproc.threshold(mascara.clone(), bin, 127, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(bin.clone(), contornos, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat canvas = Mat.zeros(new Size(tamano, tamano), bin.type());
        if (contornos.isEmpty()) {
            Imgproc.resize(bin, canvas, new Size(tamano, tamano));
            return canvas;
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

        Rect rect = Imgproc.boundingRect(mayor);
        int padX = Math.max(1, rect.width / 10);
        int padY = Math.max(1, rect.height / 10);
        int x1 = Math.max(0, rect.x - padX);
        int y1 = Math.max(0, rect.y - padY);
        int x2 = Math.min(bin.cols(), rect.x + rect.width + padX);
        int y2 = Math.min(bin.rows(), rect.y + rect.height + padY);
        Mat recorte = bin.submat(y1, y2, x1, x2).clone();

        int lado = Math.max(recorte.cols(), recorte.rows());
        if (lado < 1) {
            lado = 1;
        }
        Mat cuadrado = Mat.zeros(new Size(lado, lado), bin.type());
        int offX = (lado - recorte.cols()) / 2;
        int offY = (lado - recorte.rows()) / 2;
        recorte.copyTo(cuadrado.submat(offY, offY + recorte.rows(), offX, offX + recorte.cols()));

        Mat resultado = new Mat();
        Imgproc.resize(cuadrado, resultado, new Size(tamano, tamano));
        Imgproc.threshold(resultado, resultado, 127, 255, Imgproc.THRESH_BINARY);
        return resultado;
    }

    public static Mat crearMascaraPorColor(Mat hsv, int color) {
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

    public static Mat recortarMargenInterior(Mat mascara, int margenPct) {
        int mx = mascara.cols() * margenPct / 100;
        int my = mascara.rows() * margenPct / 100;
        if (mx * 2 >= mascara.cols() || my * 2 >= mascara.rows()) {
            return mascara;
        }
        return mascara.submat(my, mascara.rows() - my, mx, mascara.cols() - mx).clone();
    }

    public static int clamp(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }

    /** Métricas de densidad para comparar relleno (sólido / rayado / vacío). */
    public static class RasgosRelleno {
        public final double densidadTotal;
        public final double densidadCentro;
        public final double densidadErosion;
        /** Transiciones binarias en proyecciones fila/columna. */
        public final int oscilacionesFranjas;
        /** Picos locales en la densidad por fila (rayado tiene varios). */
        public final int picosFranjas;
        /** Desviación típica de la densidad por fila. */
        public final double variacionFilas;
        /** Densidad tras cierre horizontal / densidad original. */
        public final double ratioCierre;

        public RasgosRelleno(double densidadTotal, double densidadCentro,
                             double densidadErosion, int oscilacionesFranjas,
                             int picosFranjas, double variacionFilas, double ratioCierre) {
            this.densidadTotal = densidadTotal;
            this.densidadCentro = densidadCentro;
            this.densidadErosion = densidadErosion;
            this.oscilacionesFranjas = oscilacionesFranjas;
            this.picosFranjas = picosFranjas;
            this.variacionFilas = variacionFilas;
            this.ratioCierre = ratioCierre;
        }
    }

    /** Calcula rasgos de relleno a partir de la máscara de color del símbolo. */
    public static RasgosRelleno calcularRasgosRelleno(Mat mascara) {
        double areaTotal = mascara.rows() * (double) mascara.cols();
        double densidadTotal = Core.countNonZero(mascara) / areaTotal;

        int mx = Math.max(1, mascara.cols() / 6);
        int my = Math.max(1, mascara.rows() / 6);
        Mat centro = mascara.submat(my, mascara.rows() - my, mx, mascara.cols() - mx);
        double areaCentro = centro.rows() * (double) centro.cols();
        double densidadCentro = areaCentro > 0 ? Core.countNonZero(centro) / areaCentro : 0;

        Mat erosionada = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.erode(mascara, erosionada, kernel, new Point(-1, -1), 2);
        double densidadErosion = Core.countNonZero(erosionada) / areaTotal;

        int oscilaciones = Math.max(oscilacionesColumnas(mascara), oscilacionesFilas(mascara));
        int picos = Math.max(contarPicosFilas(mascara), contarPicosColumnas(mascara));
        double variacion = variacionFilas(mascara);
        double ratioCierre = calcularRatioCierre(mascara, areaTotal, densidadTotal);

        return new RasgosRelleno(densidadTotal, densidadCentro, densidadErosion,
                oscilaciones, picos, variacion, ratioCierre);
    }

    /** Detecta franjas horizontales o verticales (rayado SET). */
    public static boolean tieneFranjas(Mat region) {
        if (contarPicosFilas(region) >= 2 || contarPicosColumnas(region) >= 2) {
            return true;
        }
        if (oscilacionesColumnas(region) >= 4 || oscilacionesFilas(region) >= 4) {
            return true;
        }
        return variacionFilas(region) > 0.06 && densidadInteriorRapida(region) >= 0.03;
    }

    private static double densidadInteriorRapida(Mat region) {
        int mx = Math.max(1, region.cols() / 4);
        int my = Math.max(1, region.rows() / 4);
        if (mx * 2 >= region.cols() || my * 2 >= region.rows()) {
            return 0;
        }
        Mat interior = region.submat(my, region.rows() - my, mx, region.cols() - mx);
        return Core.countNonZero(interior) / (double) (interior.rows() * interior.cols());
    }

    private static double calcularRatioCierre(Mat mascara, double areaTotal, double densidadTotal) {
        if (densidadTotal < 0.02) {
            return 1.0;
        }
        Mat cerrada = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(11, 3));
        Imgproc.morphologyEx(mascara, cerrada, Imgproc.MORPH_CLOSE, kernel);
        double densidadCierre = Core.countNonZero(cerrada) / areaTotal;
        return densidadCierre / densidadTotal;
    }

    private static int contarPicosFilas(Mat region) {
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
        double umbralPico = max * 0.22;
        int picos = 0;
        int separacionMin = Math.max(2, region.rows() / 12);
        int ultimoPico = -separacionMin;
        for (int y = 1; y < densidades.length - 1; y++) {
            if (densidades[y] >= umbralPico
                    && densidades[y] >= densidades[y - 1]
                    && densidades[y] >= densidades[y + 1]
                    && y - ultimoPico >= separacionMin) {
                picos++;
                ultimoPico = y;
            }
        }
        return picos;
    }

    private static int contarPicosColumnas(Mat region) {
        if (region.cols() < 3) {
            return 0;
        }
        double[] densidades = new double[region.cols()];
        double max = 0;
        for (int x = 0; x < region.cols(); x++) {
            densidades[x] = Core.countNonZero(region.col(x)) / (double) region.rows();
            max = Math.max(max, densidades[x]);
        }
        if (max < 0.04) {
            return 0;
        }
        double umbralPico = max * 0.22;
        int picos = 0;
        int separacionMin = Math.max(2, region.cols() / 12);
        int ultimoPico = -separacionMin;
        for (int x = 1; x < densidades.length - 1; x++) {
            if (densidades[x] >= umbralPico
                    && densidades[x] >= densidades[x - 1]
                    && densidades[x] >= densidades[x + 1]
                    && x - ultimoPico >= separacionMin) {
                picos++;
                ultimoPico = x;
            }
        }
        return picos;
    }

    private static double variacionFilas(Mat region) {
        if (region.rows() < 2) {
            return 0;
        }
        double suma = 0;
        double[] densidades = new double[region.rows()];
        for (int y = 0; y < region.rows(); y++) {
            densidades[y] = Core.countNonZero(region.row(y)) / (double) region.cols();
            suma += densidades[y];
        }
        double media = suma / densidades.length;
        double varianza = 0;
        for (double d : densidades) {
            double diff = d - media;
            varianza += diff * diff;
        }
        return Math.sqrt(varianza / densidades.length);
    }

    private static int oscilacionesColumnas(Mat region) {
        int cambios = 0;
        boolean anterior = false;
        for (int x = 0; x < region.cols(); x++) {
            boolean activa = Core.countNonZero(region.col(x)) > region.rows() * 0.05;
            if (x > 0 && activa != anterior) {
                cambios++;
            }
            anterior = activa;
        }
        return cambios;
    }

    private static int oscilacionesFilas(Mat region) {
        int cambios = 0;
        boolean anterior = false;
        for (int y = 0; y < region.rows(); y++) {
            boolean activa = Core.countNonZero(region.row(y)) > region.cols() * 0.05;
            if (y > 0 && activa != anterior) {
                cambios++;
            }
            anterior = activa;
        }
        return cambios;
    }

    /** Silueta del símbolo aislado a partir de un Bitmap (pipeline Fase 2b). */
    public static Mat siluetaSimboloDesdeBitmap(Bitmap bitmap) {
        Mat hsv = aHsv(bitmap);
        int color = CardColorDetector.detectar(hsv);
        Mat mascara = mascaraColor(hsv, color);
        return SymbolAnalyzer.siluetaSimbolo(mascara);
    }

    public static RasgosRelleno rasgosRellenoDesdeBitmap(Bitmap bitmap) {
        Mat hsv = aHsv(bitmap);
        int color = CardColorDetector.detectar(hsv);
        Mat mascara = mascaraColor(hsv, color);
        return calcularRasgosRelleno(mascara);
    }
}
