package com.example.pedidos;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapaRutaActivity extends AppCompatActivity {

    private MapView mapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inicializar OSMDroid SIEMPRE antes de setContentView
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_mapa_ruta);

        mapa = findViewById(R.id.mapaOSM);
        mapa.setMultiTouchControls(true); // Permite hacer zoom con los dedos

        dibujarRuta();
    }

    private void dibujarRuta() {
        AdminSQLite admin = new AdminSQLite(this, "administracion", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        // Consultar solo los pedidos que tienen coordenadas válidas
        Cursor fila = db.rawQuery("SELECT cliente, latitud, longitud FROM pedidos WHERE latitud != 0 AND longitud != 0 ORDER BY id ASC", null);

        List<GeoPoint> puntosRuta = new ArrayList<>();

        if (fila.moveToFirst()) {
            do {
                String cliente = fila.getString(0);
                double lat = fila.getDouble(1);
                double lon = fila.getDouble(2);

                GeoPoint punto = new GeoPoint(lat, lon);
                puntosRuta.add(punto);

                // Dibujar un Marcador (Pin) por cada cliente
                Marker marcador = new Marker(mapa);
                marcador.setPosition(punto);
                marcador.setTitle("Pedido: " + cliente);
                marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapa.getOverlays().add(marcador);

            } while (fila.moveToNext());
        }
        fila.close();
        db.close();

        // Si hay al menos un punto, dibujamos la línea
        if (!puntosRuta.isEmpty()) {
            Polyline lineaRuta = new Polyline();
            lineaRuta.setPoints(puntosRuta);
            lineaRuta.setColor(0xFFFF0000); // Línea color ROJO
            lineaRuta.setWidth(8f);
            mapa.getOverlays().add(lineaRuta);

            // Centrar la cámara del mapa en el primer pedido registrado
            mapa.getController().setZoom(15.0);
            mapa.getController().setCenter(puntosRuta.get(0));
        }
    }

    // Es obligatorio pausar/reanudar el mapa para que no gaste batería
    @Override
    public void onResume() {
        super.onResume();
        mapa.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapa.onPause();
    }
}