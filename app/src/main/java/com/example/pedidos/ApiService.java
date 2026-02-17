package com.example.pedidos;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header; // Importante
import retrofit2.http.POST;

public interface ApiService {

    // Login
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // Sincronización (NUEVO)
    // Fíjate que pedimos el Header "Authorization" para enviar el token
    @POST("orders")
    Call<Void> enviarPedidos(
            @Header("Authorization") String token,
            @Body List<Pedido> pedidos
    );
}