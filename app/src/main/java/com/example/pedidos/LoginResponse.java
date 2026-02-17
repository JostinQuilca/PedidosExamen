package com.example.pedidos;

public class LoginResponse {
    private String token;
    private String mensaje;
    private int usuario_id;
    private String error; // Por si falla

    public String getToken() {
        return token;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getError() {
        return error;
    }
}