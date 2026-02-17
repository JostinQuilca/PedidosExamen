package com.example.pedidos;

import com.google.gson.annotations.SerializedName;

public class Pedido {
    public int id;
    public String cliente;
    public String detalle;
    @SerializedName("foto_path")
    public String fotoPath;
    public String estado;
    public String telefono;
    public String direccion;
    @SerializedName("tipo_pago")
    public String tipoPago;
    public String fecha;
    public double latitud;
    public double longitud;
    @SerializedName("foto_base64")
    public String fotoBase64;


    public Pedido(int id, String cliente, String detalle, String fotoPath, String estado) {
        this.id = id;
        this.cliente = cliente;
        this.detalle = detalle;
        this.fotoPath = fotoPath;
        this.estado = estado;
    }
}
