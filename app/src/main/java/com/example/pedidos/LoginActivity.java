package com.example.pedidos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText txtEmail, txtPass;
    private MaterialButton btnIngresar;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Verificar si ya estamos logueados (Auto-Login)
        SharedPreferences preferences = getSharedPreferences("sesion_app", Context.MODE_PRIVATE);
        String tokenGuardado = preferences.getString("token", null);

        if (tokenGuardado != null) {
            irAlMenu();
            return;
        }

        setContentView(R.layout.activity_login);

        // 2. Enlazar controles
        txtEmail = findViewById(R.id.txtEmailLogin);
        txtPass = findViewById(R.id.txtPassLogin);
        btnIngresar = findViewById(R.id.btnIngresar);
        progressBar = findViewById(R.id.progressBarLogin);

        btnIngresar.setOnClickListener(v -> hacerLogin());
    }

    private void hacerLogin() {
        String email = txtEmail.getText().toString().trim();
        String pass = txtPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarProgreso(true);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        LoginRequest datos = new LoginRequest(email, pass);

        api.login(datos).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                mostrarProgreso(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse respuesta = response.body();
                    if (respuesta.getToken() != null) {
                        guardarSesion(respuesta.getToken());
                        Toast.makeText(LoginActivity.this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                        irAlMenu();
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + respuesta.getError(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales Incorrectas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                mostrarProgreso(false);
                Toast.makeText(LoginActivity.this, "Fallo de conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarProgreso(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnIngresar.setEnabled(!mostrar);
        txtEmail.setEnabled(!mostrar);
        txtPass.setEnabled(!mostrar);
    }

    private void guardarSesion(String token) {
        SharedPreferences preferences = getSharedPreferences("sesion_app", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("token", token);
        editor.apply();
    }

    private void irAlMenu() {
        Intent i = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }
}
