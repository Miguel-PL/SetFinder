package com.mipel.setfinder;

import android.graphics.Bitmap;
import android.net.Uri;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de la partida. Vive más allá de la Activity y sobrevive a los cambios
 * de configuración (por ejemplo, rotar la pantalla), para no perder los datos
 * introducidos ni los resultados.
 */
public class MainViewModel extends ViewModel {

    public enum EstadoResultado {
        OCULTO,
        HAY_SETS,
        SIN_SETS
    }

    private static final int CANTIDAD_CARTAS_INICIAL = 15;

    /** Las 15 cartas posibles; solo se muestran las de cantidadCartas. */
    public final List<Carta> cartas = new ArrayList<>();
    public final List<Bitmap> cartasRecortadas = new ArrayList<>();

    public int cantidadCartas = 12;
    public Uri fotoUri;
    public Bitmap imagenMarcada;
    public List<List<Carta>> setsEncontrados = new ArrayList<>();
    public EstadoResultado estadoResultado = EstadoResultado.OCULTO;
    public boolean detalleVisible = false;
    public String textoDetalle = "";
    public String textoDeteccion = "";

    public MainViewModel() {
        for (int i = 0; i < CANTIDAD_CARTAS_INICIAL; i++) {
            cartas.add(new Carta(1, 0, 0, 0));
        }
    }
}
