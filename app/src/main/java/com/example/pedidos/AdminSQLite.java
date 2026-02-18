package com.example.pedidos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class AdminSQLite extends SQLiteOpenHelper {

    public AdminSQLite(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creamos la tabla con la estructura EXACTA que usa el resto de tu app
        db.execSQL("CREATE TABLE pedidos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "cliente TEXT, " +
                "telefono TEXT, " +
                "direccion TEXT, " +
                "detalle TEXT, " +
                "tipo_pago TEXT, " +
                "fecha TEXT, " +
                "latitud REAL, " +
                "longitud REAL, " +
                "foto_path TEXT, " +
                "estado TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Si la versión cambia, borramos la tabla vieja y creamos la nueva
        db.execSQL("DROP TABLE IF EXISTS pedidos");
        onCreate(db);
    }
}