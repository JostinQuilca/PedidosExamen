package com.example.pedidos;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    TextInputEditText txtCliente, txtTelefono, txtDireccion, txtDetalle;
    TextView lblGPS;
    MaterialButton btnScanQR, btnGuardar, btnFoto, btnLista;
    ImageView imgFoto;
    RadioGroup rgPago;

    private FusedLocationProviderClient fusedLocationClient;
    double latitud = 0.0;
    double longitud = 0.0;
    String rutaImagenActual = "";
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Configurar la Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbar);

        // 2. Enlazar controles
        txtCliente = findViewById(R.id.txtCliente);
        txtTelefono = findViewById(R.id.txtTelefono);
        txtDireccion = findViewById(R.id.txtDireccion);
        txtDetalle = findViewById(R.id.txtDetalle);
        lblGPS = findViewById(R.id.lblGPS);
        rgPago = findViewById(R.id.rgPago);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnFoto = findViewById(R.id.btnFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnLista = findViewById(R.id.btnLista);
        imgFoto = findViewById(R.id.imgFoto);

        // 3. Configurar GPS y listeners
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        obtenerUbicacion();

        btnScanQR.setOnClickListener(view -> escanearQR());
        btnFoto.setOnClickListener(v -> tomarFoto());
        btnGuardar.setOnClickListener(view -> guardarEnBD());
        btnLista.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ListaPedidosActivity.class);
            startActivity(i);
        });
    }

    // --- MANEJO DE LA TOOLBAR Y MENÚ ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Borrar el token de la sesión
        SharedPreferences preferences = getSharedPreferences("sesion_app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

        // Navegar de vuelta al Login
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    // --- BLOQUE GPS ---
    @SuppressWarnings("MissingPermission")
    private void obtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitud = location.getLatitude();
                longitud = location.getLongitude();
                lblGPS.setText(String.format("Ubicación: %.4f, %.4f", latitud, longitud));
            }
        });
    }

    // --- BLOQUE QR ---
    private void escanearQR() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Escanea el QR del Cliente");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    // --- BLOQUE FOTO ---
    private void tomarFoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = crearArchivoImagen();
        } catch (IOException ex) {
            Toast.makeText(this, "Error al crear el archivo de imagen", Toast.LENGTH_SHORT).show();
        }
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, "com.example.pedidos.fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File crearArchivoImagen() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        rutaImagenActual = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            llenarCamposDesdeQR(result.getContents());
            return;
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(rutaImagenActual);
            imgFoto.setImageBitmap(bitmap);
            imgFoto.setVisibility(View.VISIBLE); // Mostrar la imagen
        }
    }

    private void llenarCamposDesdeQR(String qrString) {
        try {
            String[] pares = qrString.split("\\|");
            for (String par : pares) {
                String[] keyVal = par.split("=");
                if (keyVal.length == 2) {
                    String key = keyVal[0].trim().toUpperCase();
                    String value = keyVal[1].trim();
                    if (key.equals("CLIENTE")) txtCliente.setText(value);
                    if (key.equals("TEL")) txtTelefono.setText(value);
                    if (key.equals("DIR")) txtDireccion.setText(value);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show();
        }
    }

    // --- BLOQUE BASE DE DATOS ---
    private void guardarEnBD() {
        if (txtCliente.getText().toString().isEmpty() || txtDetalle.getText().toString().isEmpty()) {
            Toast.makeText(this, "El nombre del cliente y el detalle son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        try (AdminSQLite admin = new AdminSQLite(this, "administracion", null, 1);
             SQLiteDatabase db = admin.getWritableDatabase()) {

            String tipoPagoSeleccionado = (rgPago.getCheckedRadioButtonId() == R.id.rbTransferencia) ? "Transferencia" : "Efectivo";
            String fechaActual = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            ContentValues registro = new ContentValues();
            registro.put("cliente", txtCliente.getText().toString());
            registro.put("telefono", txtTelefono.getText().toString());
            registro.put("direccion", txtDireccion.getText().toString());
            registro.put("detalle", txtDetalle.getText().toString());
            registro.put("tipo_pago", tipoPagoSeleccionado);
            registro.put("fecha", fechaActual);
            registro.put("latitud", latitud);
            registro.put("longitud", longitud);
            registro.put("foto_path", rutaImagenActual);
            registro.put("estado", "Pendiente");

            db.insert("pedidos", null, registro);

            limpiarFormulario();
            Toast.makeText(this, "✅ Pedido guardado localmente", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpiarFormulario() {
        txtCliente.setText("");
        txtTelefono.setText("");
        txtDireccion.setText("");
        txtDetalle.setText("");
        imgFoto.setImageDrawable(null);
        imgFoto.setVisibility(View.GONE); // Ocultar la imagen
        rutaImagenActual = "";
        rgPago.check(R.id.rbEfectivo);
        obtenerUbicacion();
    }
}
