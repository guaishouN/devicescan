package com.desay.devicescan;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;

public class QrCodeDataAdapter extends RecyclerView.Adapter<QrCodeDataAdapter.InnerHolder>{
    private LinkedList<QrCodeItem> dataList;
    @SuppressLint("NotifyDataSetChanged")
    public void setData(LinkedList<QrCodeItem> dataList){
        if (dataList!=null){
            this.dataList = dataList;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QrCodeDataAdapter.InnerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, null, false);
        return new InnerHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull QrCodeDataAdapter.InnerHolder holder, int position) {
        QrCodeItem item = dataList.get(position);
        holder.setEditable(item.isEditable);
        holder.keyView.setText(item.key+": ");
        if(item.isEditable){
            holder.valueViewEditable.setText(item.value);
        }else {
            holder.valueViewText.setText(item.value);
        }
    }

    @Override
    public int getItemCount() {
        return dataList!=null?dataList.size():0;
    }

    public static class InnerHolder extends RecyclerView.ViewHolder {
        public TextView keyView;
        public TextView valueViewText;
        public TextView valueViewEditable;
        public InnerHolder(@NonNull View itemView) {
            super(itemView);
            keyView = itemView.findViewById(R.id.key_text);
            valueViewText = itemView.findViewById(R.id.value);
            valueViewEditable = itemView.findViewById(R.id.edit_value);
            setEditable(false);
        }

        public void setEditable(boolean editable){
            if (editable){
                valueViewText.setVisibility(View.GONE);
                valueViewEditable.setVisibility(View.VISIBLE);
            }else {
                valueViewText.setVisibility(View.VISIBLE);
                valueViewEditable.setVisibility(View.GONE);
            }
        }
    }
}
