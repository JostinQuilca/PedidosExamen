package com.example.pedidos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder> {

    private List<Pedido> listaPedidos;

    public PedidoAdapter(List<Pedido> listaPedidos) {
        this.listaPedidos = listaPedidos;
    }

    @NonNull
    @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el nuevo layout de tarjeta (asegúrate de que item_pedido.xml exista)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        Pedido pedido = listaPedidos.get(position);
        Context context = holder.itemView.getContext();

        // 1. Asignar Textos
        holder.lblCliente.setText(pedido.cliente);
        holder.lblDetalle.setText(pedido.detalle);
        holder.lblEstado.setText(pedido.estado);

        // 2. Lógica de Colores y Estados
        if ("Sincronizado".equals(pedido.estado)) {
            int colorVerde = Color.parseColor("#43A047");
            holder.lblEstado.setTextColor(colorVerde);
            holder.iconEstado.setColorFilter(colorVerde);
            holder.iconEstado.setImageResource(R.drawable.ic_sync); // O un icono de check
        } else {
            int colorNaranja = Color.parseColor("#FB8C00");
            holder.lblEstado.setTextColor(colorNaranja);
            holder.iconEstado.setColorFilter(colorNaranja);
            holder.iconEstado.setImageResource(R.drawable.ic_sync); // Icono de reloj/sync
        }

        // 3. Cargar Foto (Optimizada para no llenar la memoria)
        if (pedido.fotoPath != null && !pedido.fotoPath.isEmpty()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(pedido.fotoPath);
                if (bitmap != null) {
                    // Reducimos la imagen a 150x150 para que la lista sea rápida
                    Bitmap small = Bitmap.createScaledBitmap(bitmap, 150, 150, true);
                    holder.imgFoto.setImageBitmap(small);
                } else {
                    holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } catch (Exception e) {
                holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
        }

        // 4. CLICK PARA VER DETALLE (Con Protección Anti-Crash)
        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(context, DetallePedidoActivity.class);

                // Pasamos todos los datos necesarios
                intent.putExtra("cliente", pedido.cliente);
                intent.putExtra("detalle", pedido.detalle);
                intent.putExtra("fecha", pedido.fecha);
                intent.putExtra("lat", pedido.latitud);
                intent.putExtra("lon", pedido.longitud);
                intent.putExtra("estado", pedido.estado);
                intent.putExtra("foto", pedido.fotoPath);

                // Intentamos abrir la actividad
                context.startActivity(intent);

            } catch (Exception e) {
                // SI FALLA: Muestra el error en pantalla en lugar de cerrar la app
                e.printStackTrace();
                Toast.makeText(context, "Error al abrir detalle: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaPedidos.size();
    }

    // --- ViewHolder ---
    public static class PedidoViewHolder extends RecyclerView.ViewHolder {

        TextView lblCliente, lblDetalle, lblEstado;
        ImageView imgFoto, iconEstado;

        public PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            // Enlazamos con los IDs del layout item_pedido.xml
            lblCliente = itemView.findViewById(R.id.lblClienteItem);
            lblDetalle = itemView.findViewById(R.id.lblDetalleItem);
            lblEstado = itemView.findViewById(R.id.lblEstadoItem);
            imgFoto = itemView.findViewById(R.id.imgFotoItem);
            iconEstado = itemView.findViewById(R.id.iconEstado);
        }
    }
}