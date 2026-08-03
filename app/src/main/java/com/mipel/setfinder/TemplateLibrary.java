package com.mipel.setfinder;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga plantillas PNG desde assets/templates/.
 * Usa el mismo pipeline HSV que los recortes en tiempo real (no OTSU en gris).
 */
public class TemplateLibrary {

    public static final String[] RUTAS_FORMA = {
            "templates/forma/forma_oval.png",
            "templates/forma/forma_rombo.png",
            "templates/forma/forma_onda.png"
    };

    public static final String[] RUTAS_RELLENO = {
            "templates/relleno/relleno_solido.png",
            "templates/relleno/relleno_rayado.png",
            "templates/relleno/relleno_vacio.png"
    };

    public static final String[] NOMBRES_FORMA = {"Óvalo", "Rombo", "Onda"};
    public static final String[] NOMBRES_RELLENO = {"Sólido", "Rayado", "Vacío"};

    private final Mat[] formas = new Mat[3];
    private final Mat[] rellenos = new Mat[3];
    private final CardImageUtils.RasgosRelleno[] rasgosRelleno = new CardImageUtils.RasgosRelleno[3];
    private FillCalibrator calibradorRelleno;
    private final List<String> faltantes = new ArrayList<>();

    public void cargar(Context context) {
        faltantes.clear();
        calibradorRelleno = null;
        AssetManager assets = context.getAssets();

        for (int i = 0; i < RUTAS_FORMA.length; i++) {
            Bitmap bitmap = cargarBitmap(assets, RUTAS_FORMA[i]);
            if (bitmap == null) {
                formas[i] = null;
                faltantes.add(RUTAS_FORMA[i]);
            } else {
                formas[i] = CardImageUtils.siluetaSimboloDesdeBitmap(bitmap);
            }
        }

        for (int i = 0; i < RUTAS_RELLENO.length; i++) {
            Bitmap bitmap = cargarBitmap(assets, RUTAS_RELLENO[i]);
            if (bitmap == null) {
                rellenos[i] = null;
                rasgosRelleno[i] = null;
                faltantes.add(RUTAS_RELLENO[i]);
            } else {
                rellenos[i] = CardImageUtils.siluetaSimboloDesdeBitmap(bitmap);
                rasgosRelleno[i] = CardImageUtils.rasgosRellenoDesdeBitmap(bitmap);
            }
        }

        if (rasgosRelleno[0] != null && rasgosRelleno[1] != null && rasgosRelleno[2] != null) {
            calibradorRelleno = new FillCalibrator(rasgosRelleno);
        }
    }

    public boolean estaCompleta() {
        return faltantes.isEmpty();
    }

    public List<String> getFaltantes() {
        return new ArrayList<>(faltantes);
    }

    public Mat[] getFormas() {
        return formas;
    }

    public Mat[] getRellenos() {
        return rellenos;
    }

    public CardImageUtils.RasgosRelleno[] getRasgosRelleno() {
        return rasgosRelleno;
    }

    public FillCalibrator getCalibradorRelleno() {
        return calibradorRelleno;
    }

    public int contarFormasCargadas() {
        return contarCargadas(formas);
    }

    public int contarRellenosCargados() {
        return contarCargadas(rellenos);
    }

    private int contarCargadas(Mat[] mats) {
        int n = 0;
        for (Mat mat : mats) {
            if (mat != null && !mat.empty()) {
                n++;
            }
        }
        return n;
    }

    private Bitmap cargarBitmap(AssetManager assets, String ruta) {
        try (InputStream stream = assets.open(ruta)) {
            return BitmapFactory.decodeStream(stream);
        } catch (IOException e) {
            return null;
        }
    }
}
