package com.mipel.setfinder;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Detección sencilla de cartas: busca contornos cuadrangulares grandes en la foto.
 */
public class CardDetector {

    private static final int ANCHO_MAXIMO = 1200;

    public static class Resultado {
        public final Bitmap imagenMarcada;
        public final List<Bitmap> cartasRecortadas;

        public Resultado(Bitmap imagenMarcada, List<Bitmap> cartasRecortadas) {
            this.imagenMarcada = imagenMarcada;
            this.cartasRecortadas = cartasRecortadas;
        }

        public int getCartasDetectadas() {
            return cartasRecortadas.size();
        }
    }

    public static Resultado detectar(Bitmap bitmapOriginal) {
        Bitmap entrada = escalarSiEsNecesario(bitmapOriginal, ANCHO_MAXIMO);

        Mat imagenRgba = new Mat();
        Utils.bitmapToMat(entrada, imagenRgba);

        Mat imagenBgr = new Mat();
        Imgproc.cvtColor(imagenRgba, imagenBgr, Imgproc.COLOR_RGBA2BGR);

        Mat gris = new Mat();
        Imgproc.cvtColor(imagenBgr, gris, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gris, gris, new Size(5, 5), 0);

        Mat bordes = new Mat();
        Imgproc.Canny(gris, bordes, 50, 150);

        List<MatOfPoint> contornos = new ArrayList<>();
        Mat jerarquia = new Mat();
        Imgproc.findContours(bordes, contornos, jerarquia,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double areaImagen = imagenBgr.rows() * imagenBgr.cols();
        double areaMin = areaImagen * 0.003;
        double areaMax = areaImagen * 0.20;

        List<Rect> regiones = new ArrayList<>();
        Mat resultado = imagenBgr.clone();

        for (MatOfPoint contorno : contornos) {
            double area = Imgproc.contourArea(contorno);
            if (area < areaMin || area > areaMax) {
                continue;
            }

            MatOfPoint2f curva = new MatOfPoint2f(contorno.toArray());
            MatOfPoint2f aproximado = new MatOfPoint2f();
            double perimetro = Imgproc.arcLength(curva, true);
            Imgproc.approxPolyDP(curva, aproximado, 0.02 * perimetro, true);

            if (aproximado.total() != 4) {
                continue;
            }

            Rect rect = Imgproc.boundingRect(contorno);
            double relacion = (double) rect.width / rect.height;
            if (relacion < 0.4 || relacion > 2.5) {
                continue;
            }

            Imgproc.rectangle(resultado, rect, new Scalar(0, 255, 0), 3);
            regiones.add(rect);
        }

        Bitmap salida = Bitmap.createBitmap(entrada.getWidth(), entrada.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultado, salida);

        List<Rect> regionesOrdenadas = CardCropper.ordenarRegiones(regiones);
        List<Bitmap> recortes = CardCropper.recortar(entrada, regionesOrdenadas);

        return new Resultado(salida, recortes);
    }

    private static Bitmap escalarSiEsNecesario(Bitmap bitmap, int maxAncho) {
        if (bitmap.getWidth() <= maxAncho) {
            return bitmap;
        }
        float ratio = (float) maxAncho / bitmap.getWidth();
        int nuevoAlto = Math.round(bitmap.getHeight() * ratio);
        return Bitmap.createScaledBitmap(bitmap, maxAncho, nuevoAlto, true);
    }
}
