package com.example.proyectointegrador.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Models.Package;
import com.example.proyectointegrador.R;

import java.util.List;

public class PackageAdapter extends RecyclerView.Adapter<PackageAdapter.PackageViewHolder> {

    private List<Package> packages;
    private OnPackageClickListener listener;

    public interface OnPackageClickListener {
        void onPackageClick(Package pkg);
    }

    public PackageAdapter(List<Package> packages, OnPackageClickListener listener) {
        this.packages = packages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        Package pkg = packages.get(position);
        
        holder.tvPackageName.setText(pkg.getName());
        holder.tvPackageDescription.setText(pkg.getDescription());
        holder.tvTicketCountBadge.setText(String.valueOf(pkg.getTicketCount()));
        holder.tvDuration.setText(pkg.getDurationDays() + " días");
        holder.tvPackagePrice.setText(String.format("Bs. %.2f", pkg.getPrice()));
        
        holder.packageCard.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPackageClick(pkg);
            }
        });
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    public void updatePackages(List<Package> newPackages) {
        this.packages = newPackages;
        notifyDataSetChanged();
    }

    static class PackageViewHolder extends RecyclerView.ViewHolder {
        CardView packageCard;
        TextView tvPackageName;
        TextView tvPackageDescription;
        TextView tvTicketCountBadge;
        TextView tvDuration;
        TextView tvPackagePrice;

        public PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            packageCard = itemView.findViewById(R.id.packageCard);
            tvPackageName = itemView.findViewById(R.id.tvPackageName);
            tvPackageDescription = itemView.findViewById(R.id.tvPackageDescription);
            tvTicketCountBadge = itemView.findViewById(R.id.tvTicketCountBadge);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvPackagePrice = itemView.findViewById(R.id.tvPackagePrice);
        }
    }
}
