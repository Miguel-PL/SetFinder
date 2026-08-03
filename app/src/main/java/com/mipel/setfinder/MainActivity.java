package com.mipel.setfinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CANTIDAD_12 = 12;
    private static final int CANTIDAD_15 = 15;

    private MainViewModel viewModel;

    private final List<View> celdasCarta = new ArrayList<>();

    private GridLayout gridCartas;
    private Button buttonComprobar;
    private TextView textResultado;
    private Button buttonVerSets;
    private TextView textDetalleSets;
    private Button buttonHacerFoto;
    private ImageView imageFoto;
    private TextView textDeteccion;
    private TextView labelMiniaturas;
    private GridLayout gridMiniaturas;
    private MaterialButtonToggleGroup grupoCantidad;
    private LinearLayout layoutResultado;

    private boolean openCvListo = false;
    private TemplateLibrary templateLibrary;

    /*
     * ActivityResultLauncher sustituye al antiguo onActivityResult.
     * Este contrato pide el permiso CAMERA y devuelve true/false en el callback.
     */
    private final ActivityResultLauncher<String> pedirPermisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    abrirCamara();
                } else {
                    Toast.makeText(this, R.string.permiso_camara_denegado, Toast.LENGTH_SHORT).show();
                }
            });

    /*
     * Contrato TakePicture: abre la cámara, guarda la imagen en la URI que le pasamos
     * y devuelve true si el usuario confirmó la foto.
     */
    private final ActivityResultLauncher<Uri> tomarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), guardada -> {
                if (guardada && viewModel.fotoUri != null) {
                    procesarFotoConOpenCv();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        gridCartas = findViewById(R.id.grid_cartas);
        buttonComprobar = findViewById(R.id.button_comprobar);
        textResultado = findViewById(R.id.text_resultado);
        buttonVerSets = findViewById(R.id.button_ver_sets);
        textDetalleSets = findViewById(R.id.text_detalle_sets);
        buttonHacerFoto = findViewById(R.id.button_hacer_foto);
        imageFoto = findViewById(R.id.image_foto);
        textDeteccion = findViewById(R.id.text_deteccion);
        labelMiniaturas = findViewById(R.id.label_miniaturas);
        gridMiniaturas = findViewById(R.id.grid_miniaturas);
        grupoCantidad = findViewById(R.id.grupo_cantidad);
        layoutResultado = findViewById(R.id.layout_resultado);

        openCvListo = OpenCVLoader.initLocal();
        if (!openCvListo) {
            Toast.makeText(this, R.string.error_opencv, Toast.LENGTH_LONG).show();
            buttonHacerFoto.setEnabled(false);
        }

        templateLibrary = new TemplateLibrary();
        templateLibrary.cargar(this);
        avisarEstadoPlantillas();

        buttonHacerFoto.setOnClickListener(v -> solicitarFoto());

        TextView buttonAyuda = findViewById(R.id.button_ayuda);
        buttonAyuda.setOnClickListener(v -> startActivity(new Intent(this, AyudaActivity.class)));

        construirCuadricula();
        restaurarEstadoUI();

        grupoCantidad.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int nuevaCantidad = checkedId == R.id.boton_12 ? CANTIDAD_12 : CANTIDAD_15;
            if (nuevaCantidad != viewModel.cantidadCartas) {
                viewModel.cantidadCartas = nuevaCantidad;
                limpiarResultado();
                construirCuadricula();
            }
        });

        buttonComprobar.setOnClickListener(v -> comprobarSets());

        buttonVerSets.setOnClickListener(v -> mostrarDetalleSets());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Comprueba permiso y, si hace falta, lo pide antes de abrir la cámara. */
    private void solicitarFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            pedirPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    /** Prepara un archivo temporal y lanza la app de cámara del sistema. */
    private void abrirCamara() {
        try {
            viewModel.fotoUri = crearUriFoto();
            tomarFoto.launch(viewModel.fotoUri);
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_abrir_camara, Toast.LENGTH_SHORT).show();
        }
    }

    private void avisarEstadoPlantillas() {
        if (!templateLibrary.estaCompleta()) {
            int cargadas = templateLibrary.contarFormasCargadas() + templateLibrary.contarRellenosCargados();
            Toast.makeText(this, getString(R.string.plantillas_incompletas, cargadas), Toast.LENGTH_LONG).show();
        }
    }

    /** Usa plantillas si están las 6; si no, el reconocedor heurístico anterior. */
    private List<Carta> reconocerCartas(List<Bitmap> recortes) {
        if (templateLibrary.estaCompleta()) {
            return CardTemplateMatcher.reconocerTodas(recortes, templateLibrary);
        }
        return CardRecognizer.reconocerTodas(recortes);
    }

    /**
     * Tras capturar la foto: detectar contornos, recortar cartas y mostrar miniaturas.
     * Se ejecuta en segundo plano para no bloquear la interfaz.
     */
    private void procesarFotoConOpenCv() {
        if (!openCvListo) {
            return;
        }

        viewModel.textoDeteccion = getString(R.string.procesando_foto);
        textDeteccion.setText(viewModel.textoDeteccion);
        buttonHacerFoto.setEnabled(false);

        new Thread(() -> {
            try {
                Bitmap bitmap = cargarBitmapDesdeUri(viewModel.fotoUri);
                CardDetector.Resultado resultado = CardDetector.detectar(bitmap);
                List<Carta> reconocidas = reconocerCartas(resultado.cartasRecortadas);

                viewModel.imagenMarcada = resultado.imagenMarcada;
                viewModel.textoDeteccion = getString(R.string.foto_reconocida_hint, reconocidas.size());

                runOnUiThread(() -> {
                    imageFoto.setVisibility(View.VISIBLE);
                    imageFoto.setImageBitmap(viewModel.imagenMarcada);
                    textDeteccion.setText(viewModel.textoDeteccion);
                    mostrarCartasRecortadas(resultado.cartasRecortadas);
                    asignarCartasReconocidas(reconocidas);
                    buttonHacerFoto.setEnabled(true);
                });
            } catch (IOException e) {
                viewModel.textoDeteccion = "";
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.error_procesar_foto, Toast.LENGTH_SHORT).show();
                    textDeteccion.setText("");
                    buttonHacerFoto.setEnabled(true);
                });
            }
        }).start();
    }

    /** Guarda los recortes y los muestra en la cuadrícula de miniaturas y en la cuadrícula principal. */
    private void mostrarCartasRecortadas(List<Bitmap> recortes) {
        viewModel.cartasRecortadas.clear();
        viewModel.cartasRecortadas.addAll(recortes);

        gridMiniaturas.removeAllViews();

        if (recortes.isEmpty()) {
            labelMiniaturas.setVisibility(View.GONE);
            gridMiniaturas.setVisibility(View.GONE);
            actualizarCuadriculaConMiniaturas();
            return;
        }

        labelMiniaturas.setVisibility(View.VISIBLE);
        gridMiniaturas.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < recortes.size(); i++) {
            View item = inflater.inflate(R.layout.item_miniatura_carta, gridMiniaturas, false);

            TextView textNumero = item.findViewById(R.id.text_numero_miniatura);
            ImageView imageMiniatura = item.findViewById(R.id.image_miniatura);

            textNumero.setText(String.valueOf(i + 1));
            imageMiniatura.setImageBitmap(recortes.get(i));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            item.setLayoutParams(params);

            gridMiniaturas.addView(item);
        }

        actualizarCuadriculaConMiniaturas();
    }

    /** Asigna color y forma reconocidos; conserva cantidad y relleno que el usuario ya haya puesto. */
    private void asignarCartasReconocidas(List<Carta> reconocidas) {
        limpiarResultado();
        int limite = Math.min(reconocidas.size(), viewModel.cantidadCartas);
        for (int i = 0; i < limite; i++) {
            Carta anterior = viewModel.cartas.get(i);
            Carta detectada = reconocidas.get(i);
            viewModel.cartas.set(i, new Carta(
                    anterior.getNumero(),
                    detectada.getColor(),
                    detectada.getForma(),
                    anterior.getRelleno()
            ));
        }
        actualizarCuadriculaConMiniaturas();
    }

    /** Coloca cada miniatura y el resumen reconocido en la celda numerada correspondiente. */
    private void actualizarCuadriculaConMiniaturas() {
        for (int i = 0; i < celdasCarta.size(); i++) {
            View celda = celdasCarta.get(i);
            ImageView imageCelda = celda.findViewById(R.id.image_carta_celda);
            TextView textResumen = celda.findViewById(R.id.text_resumen_carta);

            if (i < viewModel.cartasRecortadas.size()) {
                imageCelda.setVisibility(View.VISIBLE);
                imageCelda.setImageBitmap(viewModel.cartasRecortadas.get(i));
                textResumen.setVisibility(View.VISIBLE);
                actualizarResumenCelda(textResumen, viewModel.cartas.get(i));
            } else {
                imageCelda.setVisibility(View.GONE);
                textResumen.setVisibility(View.VISIBLE);
                actualizarResumenCelda(textResumen, viewModel.cartas.get(i));
            }
        }
    }

    private Bitmap cargarBitmapDesdeUri(Uri uri) throws IOException {
        try (InputStream entrada = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(entrada);
            if (bitmap == null) {
                throw new IOException("Bitmap nulo");
            }
            return bitmap;
        }
    }

    /** Crea un archivo en la caché y devuelve su URI segura para la app de cámara. */
    private Uri crearUriFoto() throws IOException {
        File carpetaFotos = new File(getCacheDir(), "fotos");
        if (!carpetaFotos.exists() && !carpetaFotos.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de fotos");
        }
        File archivoFoto = new File(carpetaFotos, "foto_set.jpg");
        return FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                archivoFoto
        );
    }

    private void construirCuadricula() {
        gridCartas.removeAllViews();
        celdasCarta.clear();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < viewModel.cantidadCartas; i++) {
            View celda = inflater.inflate(R.layout.item_carta_celda, gridCartas, false);
            int indice = i;

            TextView textNumero = celda.findViewById(R.id.text_numero_carta);
            TextView textResumen = celda.findViewById(R.id.text_resumen_carta);

            textNumero.setText(String.valueOf(indice + 1));
            actualizarResumenCelda(textResumen, viewModel.cartas.get(indice));

            celda.setOnClickListener(v -> mostrarDialogoEditarCarta(indice, textResumen));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            celda.setLayoutParams(params);

            gridCartas.addView(celda);
            celdasCarta.add(celda);
        }

        if (!viewModel.cartasRecortadas.isEmpty()) {
            actualizarCuadriculaConMiniaturas();
        }
    }

    private void mostrarDialogoEditarCarta(int indice, TextView textResumen) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_carta, null);

        Spinner spinnerNumero = dialogView.findViewById(R.id.dialog_spinner_numero);
        Spinner spinnerColor = dialogView.findViewById(R.id.dialog_spinner_color);
        Spinner spinnerForma = dialogView.findViewById(R.id.dialog_spinner_forma);
        Spinner spinnerRelleno = dialogView.findViewById(R.id.dialog_spinner_relleno);

        rellenarSpinners(spinnerNumero, spinnerColor, spinnerForma, spinnerRelleno);

        Carta carta = viewModel.cartas.get(indice);
        spinnerNumero.setSelection(carta.getNumero() - 1);
        spinnerColor.setSelection(carta.getColor());
        spinnerForma.setSelection(carta.getForma());
        spinnerRelleno.setSelection(carta.getRelleno());

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.editar_carta, indice + 1))
                .setView(dialogView)
                .setPositiveButton(R.string.guardar, (dialog, which) -> {
                    viewModel.cartas.set(indice, cartaDesdeSpinners(
                            spinnerNumero, spinnerColor, spinnerForma, spinnerRelleno));
                    actualizarResumenCelda(textResumen, viewModel.cartas.get(indice));
                    limpiarResultado();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void comprobarSets() {
        quitarResaltadoCeldas();
        List<Carta> cartasActivas = viewModel.cartas.subList(0, viewModel.cantidadCartas);
        viewModel.setsEncontrados = SetUtils.buscarSets(cartasActivas);

        if (viewModel.setsEncontrados.isEmpty()) {
            viewModel.estadoResultado = MainViewModel.EstadoResultado.SIN_SETS;
            textResultado.setText(R.string.no_hay_sets);
            textResultado.setTextColor(ContextCompat.getColor(this, R.color.resultado_no_texto));
            layoutResultado.setBackgroundResource(R.drawable.fondo_resultado_no);
            buttonVerSets.setVisibility(View.GONE);
            textDetalleSets.setVisibility(View.GONE);
        } else {
            viewModel.estadoResultado = MainViewModel.EstadoResultado.HAY_SETS;
            textResultado.setText(R.string.hay_sets);
            textResultado.setTextColor(ContextCompat.getColor(this, R.color.resultado_ok_texto));
            layoutResultado.setBackgroundResource(R.drawable.fondo_resultado_ok);
            buttonVerSets.setVisibility(View.VISIBLE);
            textDetalleSets.setVisibility(View.GONE);
        }
        layoutResultado.setVisibility(View.VISIBLE);
    }

    private void mostrarDetalleSets() {
        StringBuilder detalle = new StringBuilder();

        for (List<Carta> set : viewModel.setsEncontrados) {
            if (detalle.length() > 0) {
                detalle.append('\n');
            }
            detalle.append(formatearSet(set));
        }

        viewModel.textoDetalle = detalle.toString();
        viewModel.detalleVisible = true;
        textDetalleSets.setText(viewModel.textoDetalle);
        textDetalleSets.setVisibility(View.VISIBLE);
        resaltarCeldasSets();
    }

    /** Marca con un borde verde las cartas que forman parte de los SETs encontrados. */
    private void resaltarCeldasSets() {
        for (List<Carta> set : viewModel.setsEncontrados) {
            for (Carta carta : set) {
                int idx = indiceHumanoDeCarta(carta) - 1;
                if (idx >= 0 && idx < celdasCarta.size()) {
                    View celda = celdasCarta.get(idx);
                    if (celda instanceof MaterialCardView) {
                        MaterialCardView card = (MaterialCardView) celda;
                        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.carta_celda_set_fondo));
                        card.setStrokeColor(ContextCompat.getColor(this, R.color.carta_celda_set_borde));
                        card.setStrokeWidth(dp(2.5f));
                    }
                }
            }
        }
    }

    /** Devuelve las tarjetas a su apariencia normal. */
    private void quitarResaltadoCeldas() {
        for (View celda : celdasCarta) {
            if (celda instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) celda;
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.carta_celda_fondo));
                card.setStrokeColor(ContextCompat.getColor(this, R.color.carta_celda_borde));
                card.setStrokeWidth(dp(1f));
            }
        }
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }

    private String formatearSet(List<Carta> set) {
        int[] numeros = new int[3];
        for (int i = 0; i < 3; i++) {
            numeros[i] = indiceHumanoDeCarta(set.get(i));
        }
        Arrays.sort(numeros);
        return numeros[0] + ", " + numeros[1] + " y " + numeros[2];
    }

    /** Devuelve el número de carta visible (1..12 o 1..15) según su posición en la cuadrícula. */
    private int indiceHumanoDeCarta(Carta carta) {
        int indice = viewModel.cartas.indexOf(carta);
        if (indice >= 0 && indice < viewModel.cantidadCartas) {
            return indice + 1;
        }
        return -1;
    }

    private void limpiarResultado() {
        layoutResultado.setVisibility(View.GONE);
        textResultado.setText("");
        buttonVerSets.setVisibility(View.GONE);
        textDetalleSets.setVisibility(View.GONE);
        viewModel.setsEncontrados.clear();
        viewModel.estadoResultado = MainViewModel.EstadoResultado.OCULTO;
        viewModel.detalleVisible = false;
        viewModel.textoDetalle = "";
        quitarResaltadoCeldas();
    }

    /** Reaplica a la interfaz el estado guardado en el ViewModel (p. ej. tras rotar). */
    private void restaurarEstadoUI() {
        if (viewModel.cantidadCartas == CANTIDAD_15) {
            grupoCantidad.check(R.id.boton_15);
        } else {
            grupoCantidad.check(R.id.boton_12);
        }

        if (viewModel.imagenMarcada != null) {
            imageFoto.setVisibility(View.VISIBLE);
            imageFoto.setImageBitmap(viewModel.imagenMarcada);
        }
        textDeteccion.setText(viewModel.textoDeteccion);

        if (!viewModel.cartasRecortadas.isEmpty()) {
            mostrarCartasRecortadas(viewModel.cartasRecortadas);
        } else {
            labelMiniaturas.setVisibility(View.GONE);
            gridMiniaturas.setVisibility(View.GONE);
            actualizarCuadriculaConMiniaturas();
        }

        switch (viewModel.estadoResultado) {
            case HAY_SETS:
                textResultado.setText(R.string.hay_sets);
                textResultado.setTextColor(ContextCompat.getColor(this, R.color.resultado_ok_texto));
                layoutResultado.setBackgroundResource(R.drawable.fondo_resultado_ok);
                buttonVerSets.setVisibility(View.VISIBLE);
                layoutResultado.setVisibility(View.VISIBLE);
                break;
            case SIN_SETS:
                textResultado.setText(R.string.no_hay_sets);
                textResultado.setTextColor(ContextCompat.getColor(this, R.color.resultado_no_texto));
                layoutResultado.setBackgroundResource(R.drawable.fondo_resultado_no);
                buttonVerSets.setVisibility(View.GONE);
                layoutResultado.setVisibility(View.VISIBLE);
                break;
            case OCULTO:
            default:
                layoutResultado.setVisibility(View.GONE);
                break;
        }

        if (viewModel.detalleVisible) {
            textDetalleSets.setText(viewModel.textoDetalle);
            textDetalleSets.setVisibility(View.VISIBLE);
            resaltarCeldasSets();
        } else {
            textDetalleSets.setVisibility(View.GONE);
        }
    }

    private void actualizarResumenCelda(TextView textResumen, Carta carta) {
        String[] colores = {"Rojo", "Verde", "Morado"};
        String[] formas = {"Óvalo", "Rombo", "Onda"};
        String[] rellenos = {"Sólido", "Rayado", "Vacío"};

        textResumen.setText(String.format("%d %s\n%s\n%s",
                carta.getNumero(),
                colores[carta.getColor()],
                formas[carta.getForma()],
                rellenos[carta.getRelleno()]));
    }

    private void rellenarSpinners(Spinner numero, Spinner color, Spinner forma, Spinner relleno) {
        int layout = android.R.layout.simple_spinner_item;
        numero.setAdapter(ArrayAdapter.createFromResource(this, R.array.opciones_numero, layout));
        color.setAdapter(ArrayAdapter.createFromResource(this, R.array.opciones_color, layout));
        forma.setAdapter(ArrayAdapter.createFromResource(this, R.array.opciones_forma, layout));
        relleno.setAdapter(ArrayAdapter.createFromResource(this, R.array.opciones_relleno, layout));

        int dropdown = android.R.layout.simple_spinner_dropdown_item;
        ((ArrayAdapter<?>) numero.getAdapter()).setDropDownViewResource(dropdown);
        ((ArrayAdapter<?>) color.getAdapter()).setDropDownViewResource(dropdown);
        ((ArrayAdapter<?>) forma.getAdapter()).setDropDownViewResource(dropdown);
        ((ArrayAdapter<?>) relleno.getAdapter()).setDropDownViewResource(dropdown);
    }

    private Carta cartaDesdeSpinners(Spinner numero, Spinner color, Spinner forma, Spinner relleno) {
        int n = numero.getSelectedItemPosition() + 1;
        int c = color.getSelectedItemPosition();
        int f = forma.getSelectedItemPosition();
        int r = relleno.getSelectedItemPosition();
        return new Carta(n, c, f, r);
    }
}
