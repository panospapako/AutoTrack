package com.unipi.ppapakostas.autotrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.unipi.ppapakostas.autotrack.R;
import com.unipi.ppapakostas.autotrack.model.Event;
import com.unipi.ppapakostas.autotrack.model.EvenyTypeEnum;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events;

    public EventAdapter(List<Event> events) {
        this.events = events;
    }


    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.tvNumber.setText(String.format("%s.", String.valueOf(position + 1)));
        holder.tvLocation.setText(String.format("Lat: %s, Lon: %s", event.getLatitude(), event.getLongitude()));
        holder.tvTimestamp.setText(String.format("Time: %s", event.getTimestamp()));
        holder.tvEventType.setText(String.format("Event Type: %s", event.getEventType()));

        if (event.getEventType().equals(EvenyTypeEnum.BRAKING.getValue())) {
            holder.iconEventType.setImageResource(R.drawable.ic_braking);
        } else if (event.getEventType().equals(EvenyTypeEnum.RAPID_ACCELERATION.getValue())) {
            holder.iconEventType.setImageResource(R.drawable.ic_acceleration);
        } else if (event.getEventType().equals(EvenyTypeEnum.SPEED_LIMIT_VIOLATION.getValue())) {
            holder.iconEventType.setImageResource(R.drawable.ic_speed_limit);
        } else if (event.getEventType().equals(EvenyTypeEnum.POTHOLE.getValue())) {
            holder.iconEventType.setImageResource(R.drawable.ic_pothole);
        } else {
            holder.iconEventType.setImageResource(R.drawable.ic_icon_car_black);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateData(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvLocation, tvTimestamp, tvEventType;
        ImageView iconEventType;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber = itemView.findViewById(R.id.tv_number);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvEventType = itemView.findViewById(R.id.tv_event_type);
            iconEventType = itemView.findViewById(R.id.icon_event_type);
        }
    }
}
