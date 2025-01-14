package com.example.internetspeedmeter.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.internetspeedmeter.R;
import com.example.internetspeedmeter.datahandler.DataUsageHandler;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DataUsageAdapter extends RecyclerView.Adapter<DataUsageAdapter.DataUsageViewHolder>
{
    private final List<DataUsageHandler> dataUsageList;
    public DataUsageAdapter(List<DataUsageHandler> dataUsageList)
    {
        this.dataUsageList = dataUsageList;
    }

    @NonNull
    @Override
    public DataUsageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data_usage, parent, false);
        return new DataUsageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DataUsageAdapter.DataUsageViewHolder holder, int position)
    {
        DataUsageHandler data = dataUsageList.get(position);
        holder.typeTextView.setText(data.getType());
        holder.usageTextView.setText(data.getUsage());
    }

    @Override
    public int getItemCount() {
        return dataUsageList.size();
    }

    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord record) {

        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    };
    public static class DataUsageViewHolder  extends RecyclerView.ViewHolder
    {
        TextView typeTextView, usageTextView;
        public DataUsageViewHolder(@NonNull View itemView)
        {
            super(itemView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            usageTextView = itemView.findViewById(R.id.usageTextView);
        }
    }
}
