package com.example.pedidos;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListaPedidosActivity extends AppCompatActivity {

    private RecyclerView rvPedidos;
    private ExtendedFloatingActionButton btnSincronizar;
    private ExtendedFloatingActionButton btnExportarCSV;
    private ProgressBar progressBar;
    private TextView tvEmptyList;
    private PedidoAdapter adapter;
    private List<Pedido> listaDatos = new ArrayList<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int REQUEST_CODE_CSV = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_pedidos);

        // Configuración Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Enlazar Vistas
        rvPedidos = findViewById(R.id.rvPedidos);
        btnSincronizar = findViewById(R.id.btnSincronizar);
        btnExportarCSV = findViewById(R.id.btnExportarCSV);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyList = findViewById(R.id.tvEmptyList);

        // Configurar RecyclerView
        rvPedidos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidoAdapter(listaDatos);
        rvPedidos.setAdapter(adapter);

        // Cargar datos iniciales
        cargarPedidos();

        // Listeners Botones
        btnSincronizar.setOnClickListener(v -> sincronizarPendientes());
        btnExportarCSV.setOnClickListener(v -> exportarCSV());
    }

    // =========================================================================
    //   BONUS: EXPORTAR A CSV (VENTANA FLOTANTE NATIVA)
    // =========================================================================

    private void exportarCSV() {
        if (listaDatos.isEmpty()) {
            Toast.makeText(this, "No hay pedidos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Abrir ventana nativa de Android para guardar el archivo
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");

        String fecha = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        intent.putExtra(Intent.EXTRA_TITLE, "Pedidos_" + fecha + ".csv");

        startActivityForResult(intent, REQUEST_CODE_CSV);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si el usuario eligió dónde guardar y le dio a "Guardar"
        if (requestCode == REQUEST_CODE_CSV && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                escribirCSVEnRuta(data.getData());
            }
        }
    }

    private void escribirCSVEnRuta(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream == null) return;

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

            // Escribir Cabeceras
            writer.append("ID,Cliente,Telefono,Direccion,Detalle,TipoPago,Estado,Latitud,Longitud\n");

            // Escribir Datos
            for (Pedido p : listaDatos) {
                writer.append(String.valueOf(p.id)).append(",")
                        .append("\"").append(p.cliente != null ? p.cliente.replace("\"", "\"\"") : "").append("\",")
                        .append("\"").append(p.telefono != null ? p.telefono : "").append("\",")
                        .append("\"").append(p.direccion != null ? p.direccion.replace("\"", "\"\"") : "").append("\",")
                        .append("\"").append(p.detalle != null ? p.detalle.replace("\"", "\"\"") : "").append("\",")
                        .append(p.tipoPago != null ? p.tipoPago : "").append(",")
                        .append(p.estado != null ? p.estado : "").append(",")
                        .append(String.valueOf(p.latitud)).append(",")
                        .append(String.valueOf(p.longitud)).append("\n");
            }

            writer.flush();
            writer.close();
            outputStream.close();

            Toast.makeText(this, "✅ Archivo CSV guardado correctamente", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Error al guardar el archivo", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    //   LÓGICA DE BASE DE DATOS Y SINCRONIZACIÓN
    // =========================================================================

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        if (mostrar) {
            btnSincronizar.hide();
            btnExportarCSV.hide();
        } else {
            if (!listaDatos.isEmpty()) {
                btnSincronizar.show();
                btnExportarCSV.show();
            }
        }
    }

    private void cargarPedidos() {
        mostrarProgreso(true);
        executorService.execute(() -> {
            List<Pedido> pedidos = new ArrayList<>();
            try (AdminSQLite admin = new AdminSQLite(this, "administracion", null, 1);
                 SQLiteDatabase db = admin.getReadableDatabase();
                 Cursor fila = db.rawQuery("SELECT id, cliente, detalle, foto_path, estado, telefono, direccion, tipo_pago, fecha, latitud, longitud FROM pedidos ORDER BY id DESC", null)) {

                if (fila.moveToFirst()) {
                    do {
                        Pedido p = new Pedido(
                                fila.getInt(fila.getColumnIndexOrThrow("id")),
                                fila.getString(fila.getColumnIndexOrThrow("cliente")),
                                fila.getString(fila.getColumnIndexOrThrow("detalle")),
                                fila.getString(fila.getColumnIndexOrThrow("foto_path")),
                                fila.getString(fila.getColumnIndexOrThrow("estado"))
                        );
                        p.telefono = fila.getString(fila.getColumnIndexOrThrow("telefono"));
                        p.direccion = fila.getString(fila.getColumnIndexOrThrow("direccion"));
                        p.tipoPago = fila.getString(fila.getColumnIndexOrThrow("tipo_pago"));
                        p.fecha = fila.getString(fila.getColumnIndexOrThrow("fecha"));
                        p.latitud = fila.getDouble(fila.getColumnIndexOrThrow("latitud"));
                        p.longitud = fila.getDouble(fila.getColumnIndexOrThrow("longitud"));
                        pedidos.add(p);
                    } while (fila.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.post(() -> {
                mostrarProgreso(false);
                listaDatos.clear();
                listaDatos.addAll(pedidos);
                adapter.notifyDataSetChanged();

                if (listaDatos.isEmpty()) {
                    rvPedidos.setVisibility(View.GONE);
                    tvEmptyList.setVisibility(View.VISIBLE);
                    btnSincronizar.hide();
                    btnExportarCSV.hide();
                } else {
                    rvPedidos.setVisibility(View.VISIBLE);
                    tvEmptyList.setVisibility(View.GONE);
                    btnSincronizar.show();
                    btnExportarCSV.show();
                }
            });
        });
    }

    private void sincronizarPendientes() {
        mostrarProgreso(true);
        executorService.execute(() -> {
            List<Pedido> paraEnviar = new ArrayList<>();
            for (Pedido p : listaDatos) {
                if ("Pendiente".equals(p.estado)) {
                    if (p.fotoPath != null && !p.fotoPath.isEmpty()) {
                        p.fotoBase64 = convertirImagenABase64(p.fotoPath);
                    } else {
                        p.fotoBase64 = "";
                    }
                    paraEnviar.add(p);
                }
            }

            if (paraEnviar.isEmpty()) {
                handler.post(() -> {
                    mostrarProgreso(false);
                    Toast.makeText(this, "No hay pendientes para enviar", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            SharedPreferences prefs = getSharedPreferences("sesion_app", Context.MODE_PRIVATE);
            String token = "Bearer " + prefs.getString("token", "");

            try {
                ApiService api = ApiClient.getClient().create(ApiService.class);
                api.enviarPedidos(token, paraEnviar).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        handler.post(() -> {
                            if (response.isSuccessful()) {
                                marcarComoSincronizados();
                            } else {
                                mostrarProgreso(false);
                                Toast.makeText(ListaPedidosActivity.this, "Error Servidor: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        handler.post(() -> {
                            mostrarProgreso(false);
                            Toast.makeText(ListaPedidosActivity.this, "Error Red: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                handler.post(() -> {
                    mostrarProgreso(false);
                    Toast.makeText(this, "Error al iniciar envío", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void marcarComoSincronizados() {
        executorService.execute(() -> {
            try (AdminSQLite admin = new AdminSQLite(this, "administracion", null, 1);
                 SQLiteDatabase db = admin.getWritableDatabase()) {
                ContentValues cv = new ContentValues();
                cv.put("estado", "Sincronizado");
                db.update("pedidos", cv, "estado=?", new String[]{"Pendiente"});
            }
            handler.post(() -> {
                Toast.makeText(ListaPedidosActivity.this, "¡Sincronización completada!", Toast.LENGTH_LONG).show();
                cargarPedidos();
            });
        });
    }

    private String convertirImagenABase64(String path) {
        if (path == null || path.isEmpty()) return "";
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = calcularInSampleSize(options, 800, 800);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) return "";
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            bitmap.recycle();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
    }

    private int calcularInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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