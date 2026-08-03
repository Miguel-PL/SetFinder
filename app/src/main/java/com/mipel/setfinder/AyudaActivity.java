package com.mipel.setfinder;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AyudaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayuda);

        Button buttonVolver = findViewById(R.id.button_volver_ayuda);
        buttonVolver.setOnClickListener(v -> finish());
    }
}
