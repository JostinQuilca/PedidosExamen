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
        // Inflamos el nuevo layout de tarjeta
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

        // --- COLORES ---
        if ("Sincronizado".equals(pedido.estado)) {
            int colorVerde = Color.parseColor("#43A047");
            holder.lblEstado.setTextColor(colorVerde);
            holder.iconEstado.setColorFilter(colorVerde);
        } else {
            int colorNaranja = Color.parseColor("#FB8C00");
            holder.lblEstado.setTextColor(colorNaranja);
            holder.iconEstado.setColorFilter(colorNaranja);
        }

        // --- CARGAR FOTO ---
        if (pedido.fotoPath != null && !pedido.fotoPath.isEmpty()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(pedido.fotoPath);
                if (bitmap != null) {
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

        // --- CLICK PARA VER DETALLE (NUEVO) ---
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), DetallePedidoActivity.class);
            intent.putExtra("cliente", pedido.cliente);
            intent.putExtra("detalle", pedido.detalle);
            intent.putExtra("fecha", pedido.fecha);       // Requisito: Fecha y Hora
            intent.putExtra("lat", pedido.latitud);       // Requisito: Latitud
            intent.putExtra("lon", pedido.longitud);      // Requisito: Longitud
            intent.putExtra("estado", pedido.estado);
            intent.putExtra("foto", pedido.fotoPath);     // Requisito: Fotografía
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaPedidos.size();
    }

    // --- VIEWHOLDER: Referencias a los controles del XML ---
    public static class PedidoViewHolder extends RecyclerView.ViewHolder {

        TextView lblCliente, lblDetalle, lblEstado;
        ImageView imgFoto, iconEstado;

        public PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            // Enlazamos con los IDs del nuevo item_pedido.xml
            lblCliente = itemView.findViewById(R.id.lblClienteItem);
            lblDetalle = itemView.findViewById(R.id.lblDetalleItem);
            lblEstado = itemView.findViewById(R.id.lblEstadoItem);
            imgFoto = itemView.findViewById(R.id.imgFotoItem);
            iconEstado = itemView.findViewById(R.id.iconEstado); // Nuevo icono de estado
        }
    }
}