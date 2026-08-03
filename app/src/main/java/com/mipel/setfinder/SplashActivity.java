package com.mipel.setfinder;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Pantalla de portada que se muestra mientras la app arranca.
 * Tras una breve pausa navega a la pantalla principal con una transición suave.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int DURACION_MOSTRAR_MS = 1600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ocultarBarraDeEstado();

        View contenido = findViewById(R.id.contenido_splash);
        contenido.setAlpha(0f);
        contenido.animate().alpha(1f).setDuration(600).start();

        new Handler(Looper.getMainLooper()).postDelayed(this::irAInicio, DURACION_MOSTRAR_MS);
    }

    /** Sustituye al antiguo FLAG_FULLSCREEN con la API moderna de insets. */
    private void ocultarBarraDeEstado() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.statusBars());
    }

    private void irAInicio() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
