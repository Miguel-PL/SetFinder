package com.mipel.setfinder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Cuenta símbolos (1 / 2 / 3) apilados verticalmente en la carta.
 */
public final class CardNumberDetector {

    private CardNumberDetector() {
    }

    public static int detectar(Mat mascaraRaw) {
        int porBandas = contarPorBandasCentroides(mascaraRaw);
        int porPicos = contarPorPicos(mascaraRaw);
        int porContornos = contarPorContornos(mascaraRaw);

        Mat mascaraSuave = prepararMascaraSuave(mascaraRaw);
        int porComponentes = contarPorComponentes(mascaraSuave);
        int porValles = contarPorValles(mascaraSuave);

        return combinarEstimaciones(porBandas, porPicos, porContornos, porComponentes, porValles);
    }

    /** Cierre vertical ligero: une franjas sin pegar ondas distintas. */
    private static Mat prepararMascaraSuave(Mat mascaraRaw) {
        Mat mascara = mascaraRaw.clone();
        Mat kernelV = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 7));
        Imgproc.morphologyEx(mascara, mascara, Imgproc.MORPH_CLOSE, kernelV);
        return mascara;
    }

    /**
     * Histograma de centroides de fragmentos en máscara cruda.
     * Funciona con ondas rayadas (muchos fragmentos → 2 o 3 bandas verticales).
     */
    private static int contarPorBandasCentroides(Mat mascaraRaw) {
        int filas = mascaraRaw.rows();
        if (filas < 4) {
            return 1;
        }

        int[] hist = new int[filas];
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();

        int nLabels = Imgproc.connectedComponentsWithStats(mascaraRaw, labels, stats, centroids);
        double areaMin = filas * mascaraRaw.cols() * 0.00006;

        for (int i = 1; i < nLabels; i++) {
            double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
            if (area >= areaMin) {
                int top = (int) stats.get(i, Imgproc.CC_STAT_TOP)[0];
                int h = (int) stats.get(i, Imgproc.CC_STAT_HEIGHT)[0];
                int cy = Math.min(filas - 1, Math.max(0, top + h / 2));
                hist[cy]++;
            }
        }

        int ventana = Math.max(5, filas / 8);
        int[] suave = suavizar(hist, ventana);
        return contarPicosEnSerie(suave, filas, 0.18, 5);
    }

    private static int combinarEstimaciones(int bandas, int picos, int contornos,
                                            int componentes, int valles) {
        int[] votos = new int[4];
        votos[bandas] += 4;
        votos[picos] += 2;
        votos[contornos] += 2;
        votos[componentes]++;
        votos[valles]++;

        int mejor = 1;
        int maxVotos = 0;
        for (int n = 1; n <= 3; n++) {
            if (votos[n] > maxVotos) {
                maxVotos = votos[n];
                mejor = n;
            }
        }

        if (bandas >= 2 && componentes <= 1 && contornos <= 1) {
            return CardImageUtils.clamp(bandas, 1, 3);
        }

        if (maxVotos >= 4) {
            return mejor;
        }

        List<Integer> vals = new ArrayList<>();
        agregarSiValido(vals, bandas);
        agregarSiValido(vals, picos);
        agregarSiValido(vals, contornos);
        agregarSiValido(vals, componentes);
        agregarSiValido(vals, valles);
        Collections.sort(vals);

        if (maxVotos >= 2) {
            return mejor;
        }
        return vals.get(vals.size() / 2);
    }

    private static void agregarSiValido(List<Integer> vals, int valor) {
        if (valor >= 1 && valor <= 3) {
            vals.add(valor);
        }
    }

    private static int contarPicosEnSerie(int[] serie, int filas, double umbralRatio, int sepDivisor) {
        int max = maximo(serie);
        if (max == 0) {
            return 1;
        }

        int umbral = Math.max(1, (int) (max * umbralRatio));
        int separacionMin = Math.max(filas / sepDivisor, 4);
        int picos = 0;
        int ultimoPico = -separacionMin;

        for (int i = 1; i < serie.length - 1; i++) {
            if (serie[i] >= umbral
                    && serie[i] >= serie[i - 1]
                    && serie[i] >= serie[i + 1]
                    && i - ultimoPico >= separacionMin) {
                picos++;
                ultimoPico = i;
            }
        }

        if (picos == 0) {
            return 1;
        }
        return CardImageUtils.clamp(picos, 1, 3);
    }

    private static int contarPorComponentes(Mat mascara) {
        List<Integer> centrosY = centrosComponentes(mascara);
        return agruparCentros(centrosY, mascara.rows());
    }

    private static int agruparCentros(List<Integer> centrosY, int filas) {
        if (centrosY.size() <= 1) {
            return Math.max(1, centrosY.size());
        }

        Collections.sort(centrosY);
        int fusionFrag = Math.max(2, filas / 16);
        int gapSimbolo = Math.max(3, filas / 12);

        int grupos = 1;
        int ultimo = centrosY.get(0);

        for (int i = 1; i < centrosY.size(); i++) {
            int centro = centrosY.get(i);
            int gap = centro - ultimo;
            if (gap < fusionFrag) {
                ultimo = (ultimo + centro) / 2;
            } else {
                grupos++;
                ultimo = centro;
            }
        }
        if (grupos > 1 && centrosY.get(centrosY.size() - 1) - centrosY.get(0) < gapSimbolo) {
            return 1;
        }
        return CardImageUtils.clamp(grupos, 1, 3);
    }

    private static List<Integer> centrosComponentes(Mat mascara) {
        List<Integer> centros = new ArrayList<>();
        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();

        int nLabels = Imgproc.connectedComponentsWithStats(mascara, labels, stats, centroids);

        double areaMin = mascara.rows() * mascara.cols() * 0.0007;
        int alturaMin = Math.max(2, mascara.rows() / 16);

        for (int i = 1; i < nLabels; i++) {
            double area = stats.get(i, Imgproc.CC_STAT_AREA)[0];
            double alto = stats.get(i, Imgproc.CC_STAT_HEIGHT)[0];
            if (area >= areaMin && alto >= alturaMin) {
                int top = (int) stats.get(i, Imgproc.CC_STAT_TOP)[0];
                int h = (int) stats.get(i, Imgproc.CC_STAT_HEIGHT)[0];
                centros.add(top + h / 2);
            }
        }
        return centros;
    }

    private static int contarPorPicos(Mat mascara) {
        int filas = mascara.rows();
        int[] proyeccion = suavizar(proyeccionVertical(mascara), Math.max(5, filas / 10));
        return contarPicosEnSerie(proyeccion, filas, 0.10, 6);
    }

    private static int contarPorValles(Mat mascara) {
        int filas = mascara.rows();
        int[] proyeccion = suavizar(proyeccionVertical(mascara), 7);
        int max = maximo(proyeccion);
        if (max == 0) {
            return 1;
        }

        int umbralValle = Math.max(1, (int) (max * 0.06));
        int anchoValleMin = Math.max(2, filas / 12);

        int valles = 0;
        int filasValle = 0;
        boolean enValle = false;

        for (int y = 0; y < filas; y++) {
            if (proyeccion[y] <= umbralValle) {
                filasValle = enValle ? filasValle + 1 : 1;
                enValle = true;
            } else {
                if (enValle && filasValle >= anchoValleMin) {
                    valles++;
                }
                enValle = false;
                filasValle = 0;
            }
        }
        if (enValle && filasValle >= anchoValleMin) {
            valles++;
        }
        return CardImageUtils.clamp(valles + 1, 1, 3);
    }

    private static int contarPorContornos(Mat mascara) {
        List<Rect> rects = rectsSignificativos(mascara);
        if (rects.size() <= 1) {
            return Math.max(1, rects.size());
        }

        Collections.sort(rects, Comparator.comparingInt(r -> r.y + r.height / 2));
        int filas = mascara.rows();
        int gapMin = Math.max(2, filas / 12);

        int grupos = 1;
        Rect anterior = rects.get(0);

        for (int i = 1; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            if (!mismaColumna(anterior, rect)) {
                continue;
            }
            int gap = rect.y - (anterior.y + anterior.height);
            if (gap >= gapMin) {
                grupos++;
            }
            anterior = rect;
        }
        return CardImageUtils.clamp(grupos, 1, 3);
    }

    private static List<Rect> rectsSignificativos(Mat mascara) {
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(mascara.clone(), contornos, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double areaMin = mascara.rows() * mascara.cols() * 0.0007;
        int alturaMin = Math.max(2, mascara.rows() / 16);

        List<Rect> rects = new ArrayList<>();
        for (MatOfPoint contorno : contornos) {
            if (Imgproc.contourArea(contorno) >= areaMin) {
                Rect rect = Imgproc.boundingRect(contorno);
                if (rect.height >= alturaMin) {
                    rects.add(rect);
                }
            }
        }
        return rects;
    }

    private static int[] suavizar(int[] datos, int ventana) {
        int[] suavizado = new int[datos.length];
        int radio = Math.max(1, ventana / 2);
        for (int i = 0; i < datos.length; i++) {
            int suma = 0;
            int n = 0;
            for (int j = Math.max(0, i - radio); j <= Math.min(datos.length - 1, i + radio); j++) {
                suma += datos[j];
                n++;
            }
            suavizado[i] = n > 0 ? suma / n : datos[i];
        }
        return suavizado;
    }

    private static int[] proyeccionVertical(Mat mascara) {
        int[] proyeccion = new int[mascara.rows()];
        for (int y = 0; y < mascara.rows(); y++) {
            proyeccion[y] = Core.countNonZero(mascara.row(y));
        }
        return proyeccion;
    }

    private static int maximo(int[] valores) {
        int max = 0;
        for (int v : valores) {
            max = Math.max(max, v);
        }
        return max;
    }

    private static boolean mismaColumna(Rect a, Rect b) {
        int izq = Math.max(a.x, b.x);
        int der = Math.min(a.x + a.width, b.x + b.width);
        return der - izq > Math.min(a.width, b.width) * 0.15;
    }
}
