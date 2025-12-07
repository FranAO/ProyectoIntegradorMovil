package com.example.proyectointegrador.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.R;

import java.util.List;

public class StudentReturnAdapter extends RecyclerView.Adapter<StudentReturnAdapter.ViewHolder> {

    private List<String> studentNames;
    private List<String> ticketIds;
    private OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(int position, String studentName, String ticketId);
    }

    public StudentReturnAdapter(List<String> studentNames, List<String> ticketIds, OnStudentClickListener listener) {
        this.studentNames = studentNames;
        this.ticketIds = ticketIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_return, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String studentName = studentNames.get(position);
        String ticketId = ticketIds.get(position);

        holder.tvStudentName.setText(studentName);
        holder.tvTicketId.setText("Ticket #" + ticketId.substring(Math.max(0, ticketId.length() - 8)));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStudentClick(position, studentName, ticketId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvTicketId;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
        }
    }
}
