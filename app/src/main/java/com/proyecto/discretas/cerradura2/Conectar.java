package com.proyecto.discretas.cerradura2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class Conectar extends AppCompatActivity {
    Button btnConectar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conectar);

        btnConectar=(Button)findViewById(R.id.conectar);

        btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cambio=new Intent(Conectar.this,ListBluetooth.class);
                startActivity(cambio);
            }
        });

    }





}
