package com.example.pedidos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class DetallePedidoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_detalle_pedido);

           // MaterialToolbar toolbar = findViewById(R.id.toolbarDetalle);
           // setSupportActionBar(toolbar);
            // Si el icono de atrás falla, lo quitamos por seguridad
            // toolbar.setNavigationOnClickListener(v -> onBackPressed());

            TextView tvCliente = findViewById(R.id.tvClienteDetalle);
            TextView tvPedido = findViewById(R.id.tvPedidoDetalle);
            TextView tvFecha = findViewById(R.id.tvFechaDetalle);
            TextView tvUbicacion = findViewById(R.id.tvUbicacionDetalle);
            TextView tvEstado = findViewById(R.id.tvEstadoDetalle);
            ImageView imgFoto = findViewById(R.id.imgFotoDetalle);

            if (getIntent() != null) {
                String cliente = getIntent().getStringExtra("cliente");
                String detalle = getIntent().getStringExtra("detalle");
                String fecha = getIntent().getStringExtra("fecha");
                String estado = getIntent().getStringExtra("estado");
                String fotoPath = getIntent().getStringExtra("foto");
                double lat = getIntent().getDoubleExtra("lat", 0.0);
                double lon = getIntent().getDoubleExtra("lon", 0.0);

                tvCliente.setText(cliente != null ? cliente : "---");
                tvPedido.setText(detalle != null ? detalle : "---");
                tvFecha.setText(fecha != null ? fecha : "--/--/----");
                tvEstado.setText("Estado: " + (estado != null ? estado : "Pendiente"));
                tvUbicacion.setText(String.format("GPS: %.4f, %.4f", lat, lon));

                // --- CARGA SEGURA DE IMAGEN (SOLUCIÓN AL CRASH) ---
                if (fotoPath != null && !fotoPath.isEmpty()) {
                    Bitmap bitmap = cargarImagenSegura(fotoPath);
                    if (bitmap != null) {
                        imgFoto.setImageBitmap(bitmap);
                    } else {
                        // Si falla la carga, ponemos un icono por defecto pero NO CERRAMOS
                        imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                }
            }

        } catch (Throwable e) {
            // CAMBIO IMPORTANTE: Usamos 'Throwable' para atrapar errores de Memoria
            e.printStackTrace();
            Toast.makeText(this, "Error memoria/visual: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish(); // Cierra suavemente la pantalla en vez de explotar
        }
    }

    // --- MÉTODO MÁGICO PARA REDUCIR LA FOTO ---
    private Bitmap cargarImagenSegura(String path) {
        try {
            // 1. Leer solo las dimensiones (sin cargar la imagen)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // 2. Calcular cuánto reducirla (Escalar a 800px aprox)
            options.inSampleSize = calculateInSampleSize(options, 800, 800);

            // 3. Cargar ahora sí la imagen reducida
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeFile(path, options);

        } catch (Exception e) {
            return null;
        }
    }

    // Algoritmo estándar de Android para reducir fotos
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}