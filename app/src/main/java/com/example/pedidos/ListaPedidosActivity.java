package com.example.pedidos;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListaPedidosActivity extends AppCompatActivity {

    private RecyclerView rvPedidos;
    private ExtendedFloatingActionButton btnSincronizar;
    private ProgressBar progressBar;
    private TextView tvEmptyList;
    private PedidoAdapter adapter;
    private List<Pedido> listaDatos = new ArrayList<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_pedidos);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvPedidos = findViewById(R.id.rvPedidos);
        btnSincronizar = findViewById(R.id.btnSincronizar);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyList = findViewById(R.id.tvEmptyList);

        rvPedidos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidoAdapter(listaDatos);
        rvPedidos.setAdapter(adapter);

        cargarPedidos();

        btnSincronizar.setOnClickListener(v -> sincronizarPendientes());
    }

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        if (mostrar) {
            btnSincronizar.hide();
        } else {
            if(!listaDatos.isEmpty()) btnSincronizar.show();
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
                } else {
                    rvPedidos.setVisibility(View.VISIBLE);
                    tvEmptyList.setVisibility(View.GONE);
                    btnSincronizar.show();
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
                    // CONVERSIÓN SEGURA DE IMAGEN
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

    // --- CORRECCIÓN CRÍTICA: Evitar Crash si la imagen falla ---
    private String convertirImagenABase64(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // Reducir tamaño
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);

            if (bitmap == null) return ""; // Si la imagen no carga, devolvemos vacío en vez de crashear

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return ""; // En caso de error, devolver cadena vacía
        }
    }
}