package com.example.pedidos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class DetallePedidoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_pedido);

        // Toolbar con flecha atrás
        MaterialToolbar toolbar = findViewById(R.id.toolbarDetalle);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Enlazar vistas
        TextView tvCliente = findViewById(R.id.tvClienteDetalle);
        TextView tvPedido = findViewById(R.id.tvPedidoDetalle);
        TextView tvFecha = findViewById(R.id.tvFechaDetalle);
        TextView tvUbicacion = findViewById(R.id.tvUbicacionDetalle);
        TextView tvEstado = findViewById(R.id.tvEstadoDetalle);
        ImageView imgFoto = findViewById(R.id.imgFotoDetalle);

        // Recibir datos del Intent
        if (getIntent() != null) {
            String cliente = getIntent().getStringExtra("cliente");
            String detalle = getIntent().getStringExtra("detalle");
            String fecha = getIntent().getStringExtra("fecha");
            String estado = getIntent().getStringExtra("estado");
            String fotoPath = getIntent().getStringExtra("foto");
            double lat = getIntent().getDoubleExtra("lat", 0.0);
            double lon = getIntent().getDoubleExtra("lon", 0.0);

            // Asignar textos
            tvCliente.setText(cliente);
            tvPedido.setText(detalle);
            tvFecha.setText(fecha);
            tvEstado.setText("Estado: " + estado);
            tvUbicacion.setText(String.format("Lat: %.5f  |  Lon: %.5f", lat, lon));

            // Cargar foto grande
            if (fotoPath != null && !fotoPath.isEmpty()) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(fotoPath);
                    if (bitmap != null) {
                        imgFoto.setImageBitmap(bitmap);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}