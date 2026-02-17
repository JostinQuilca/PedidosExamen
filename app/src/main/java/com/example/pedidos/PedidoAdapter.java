package com.example.pedidos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        Pedido pedido = listaPedidos.get(position);

        holder.lblCliente.setText(pedido.cliente);
        holder.lblDetalle.setText(pedido.detalle);
        holder.lblEstado.setText(pedido.estado);

        // Color según estado
        if ("Sincronizado".equals(pedido.estado)) {
            holder.lblEstado.setTextColor(Color.parseColor("#4CAF50")); // Verde
        } else {
            holder.lblEstado.setTextColor(Color.parseColor("#F44336")); // Rojo
        }

        // Cargar foto pequeña
        if (pedido.fotoPath != null && !pedido.fotoPath.isEmpty()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(pedido.fotoPath);
                // Reducir imagen para no saturar memoria
                if (bitmap != null) {
                    Bitmap small = Bitmap.createScaledBitmap(bitmap, 100, 100, true);
                    holder.imgFoto.setImageBitmap(small);
                }
            } catch (Exception e) {
                holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
            }
        } else {
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    @Override
    public int getItemCount() {
        return listaPedidos.size();
    }

    // Clase interna ViewHolder
    public static class PedidoViewHolder extends RecyclerView.ViewHolder {
        TextView lblCliente, lblDetalle, lblEstado;
        ImageView imgFoto;

        public PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            lblCliente = itemView.findViewById(R.id.lblClienteItem);
            lblDetalle = itemView.findViewById(R.id.lblDetalleItem);
            lblEstado = itemView.findViewById(R.id.lblEstadoItem);
            imgFoto = itemView.findViewById(R.id.imgFotoItem);
        }
    }
}