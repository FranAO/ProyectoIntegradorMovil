package com.example.proyectointegrador.UI;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyectointegrador.Adapters.PackageAdapter;
import com.example.proyectointegrador.Models.Package;
import com.example.proyectointegrador.R;
import com.example.proyectointegrador.Services.PackageApiService;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class PackagesActivity extends BaseNavigationActivity {

    private CardView activePackageCard, singleTicketCard;
    private TextView priceSingle, tvActivePackageName, tvActivePackageTickets;
    private RecyclerView rvPackages;
    private PackageAdapter packageAdapter;
    private List<Package> packageList = new ArrayList<>();
    private String studentEmail;
    
    private final double PRICE_SINGLE = 5.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_packages);

        setupNavigation();
        initViews();
        loadPackages();
        loadUserPackages();
    }

    private void initViews() {
        SharedPreferences prefs = getSharedPreferences("MiAppPrefs", MODE_PRIVATE);
        studentEmail = prefs.getString("LOGGED_IN_USER_EMAIL", "");

        singleTicketCard = findViewById(R.id.singleTicketCard);
        activePackageCard = findViewById(R.id.activePackageCard);
        rvPackages = findViewById(R.id.rvPackages);
        
        priceSingle = findViewById(R.id.priceSingle);
        tvActivePackageName = findViewById(R.id.tvActivePackageName);
        tvActivePackageTickets = findViewById(R.id.tvActivePackageTickets);

        priceSingle.setText(String.format("Bs. %.2f", PRICE_SINGLE));

        // Setup RecyclerView
        rvPackages.setLayoutManager(new LinearLayoutManager(this));
        packageAdapter = new PackageAdapter(packageList, this::onPackageClick);
        rvPackages.setAdapter(packageAdapter);

        // Listener para el botón de compra individual
        MaterialButton btnBuySingle = findViewById(R.id.btnBuySingle);
        btnBuySingle.setOnClickListener(v -> purchaseSingleTicket());
        
        // activePackageCard click genera QR
        activePackageCard.setOnClickListener(v -> showPackageTicketQR());
    }

    private void loadPackages() {
        PackageApiService.getAllPackages(new PackageApiService.PackageListCallback() {
            @Override
            public void onSuccess(List<Package> packages) {
                packageList.clear();
                packageList.addAll(packages);
                packageAdapter.updatePackages(packageList);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PackagesActivity.this, 
                    "Error al cargar paquetes: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onPackageClick(Package pkg) {
        checkActivePackageBeforePurchase(
            pkg.getName(),
            pkg.getTicketCount(),
            pkg.getPrice(),
            pkg.getDurationDays()
        );
    }

    private void loadUserPackages() {
        if (studentEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.proyectointegrador.Services.TicketApiService.getPackageSummary(
                studentEmail,
                new com.example.proyectointegrador.Services.TicketApiService.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(response);
                            org.json.JSONArray packages = json.getJSONArray("packages");
                            
                            boolean hasActivePackage = false;
                            
                            for (int i = 0; i < packages.length(); i++) {
                                org.json.JSONObject pkg = packages.getJSONObject(i);
                                int available = pkg.getInt("availableTickets");
                                int total = pkg.getInt("totalTickets");
                                
                                if (available > 0) {
                                    activePackageCard.setVisibility(View.VISIBLE);
                                    
                                    String packageName = total + " Tickets";
                                    tvActivePackageName.setText(packageName);
                                    tvActivePackageTickets.setText(String.format("%d/%d tickets disponibles", available, total));
                                    hasActivePackage = true;
                                    break;
                                }
                            }
                            
                            if (!hasActivePackage) {
                                activePackageCard.setVisibility(View.GONE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            activePackageCard.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        activePackageCard.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void checkActivePackageBeforePurchase(String packageName, int ticketCount, double price, int durationDays) {
        if (studentEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si hay un paquete activo
        com.example.proyectointegrador.Services.TicketApiService.getPackageSummary(
                studentEmail,
                new com.example.proyectointegrador.Services.TicketApiService.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(response);
                            org.json.JSONArray packages = json.getJSONArray("packages");
                            
                            boolean hasActivePackage = false;
                            String activePackageInfo = "";
                            
                            for (int i = 0; i < packages.length(); i++) {
                                org.json.JSONObject pkg = packages.getJSONObject(i);
                                int available = pkg.getInt("availableTickets");
                                boolean active = pkg.getBoolean("active");
                                
                                // Si hay tickets disponibles y el paquete está activo
                                if (available > 0 && active) {
                                    hasActivePackage = true;
                                    int total = pkg.getInt("totalTickets");
                                    showActivePackageDialog(available, total);
                                    break;
                                }
                            }
                            
                            if (!hasActivePackage) {
                                // Permitir la compra
                                showConfirmationDialog(packageName, ticketCount, price, durationDays);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(PackagesActivity.this, "Error al verificar paquetes", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(PackagesActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void showActivePackageDialog(int availableTickets, int totalTickets) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_active_package);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Configurar ancho y alto del diálogo
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
        dialog.getWindow().setAttributes(params);

        TextView tvTicketsInfo = dialog.findViewById(R.id.tvTicketsInfo);
        MaterialButton btnUnderstood = dialog.findViewById(R.id.btnUnderstood);

        tvTicketsInfo.setText(String.format("%d de %d tickets", availableTickets, totalTickets));

        btnUnderstood.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(true);
        dialog.show();
    }

    private void showConfirmationDialog(String packageName, int ticketCount, double price, int durationDays) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_package_confirmation);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvPackageName = dialog.findViewById(R.id.tvPackageName);
        TextView tvPackagePrice = dialog.findViewById(R.id.tvPackagePrice);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvPackageName.setText(packageName);
        tvPackagePrice.setText(String.format("Bs. %.2f", price));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            proceedToPayment(packageName, ticketCount, price, durationDays);
        });

        dialog.show();
    }

    private void proceedToPayment(String packageName, int ticketCount, double price, int durationDays) {
        Intent intent = new Intent(this, PaymentQRActivity.class);
        intent.putExtra("packageId", "PKG-" + ticketCount);
        intent.putExtra("packageName", packageName);
        intent.putExtra("ticketCount", ticketCount);
        intent.putExtra("price", price);
        intent.putExtra("durationDays", durationDays);
        intent.putExtra("studentEmail", studentEmail);
        startActivity(intent);
    }

    private void purchaseSingleTicket() {
        if (studentEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar diálogo de confirmación personalizado
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_single_ticket);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Configurar ancho y alto del diálogo
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
        dialog.getWindow().setAttributes(params);

        TextView tvPrice = dialog.findViewById(R.id.tvPrice);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvPrice.setText(String.format("Bs. %.2f", PRICE_SINGLE));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            
            // Llamar al servicio para comprar ticket individual
            android.util.Log.d("PackagesActivity", "Comprando ticket para: " + studentEmail);
            com.example.proyectointegrador.Services.TicketApiService.purchaseSingleTicket(
                    studentEmail,
                    null, // No hay tripId al comprar, es ticket universal
                    new com.example.proyectointegrador.Services.TicketApiService.ApiCallback() {
                        @Override
                        public void onSuccess(String response) {
                            android.util.Log.d("PackagesActivity", "Ticket comprado exitosamente: " + response);
                            Toast.makeText(PackagesActivity.this, "¡Ticket comprado exitosamente!", Toast.LENGTH_SHORT).show();
                            // Navegar a HistoryActivity
                            Intent intent = new Intent(PackagesActivity.this, HistoryActivity.class);
                            startActivity(intent);
                        }

                        @Override
                        public void onError(String error) {
                            android.util.Log.e("PackagesActivity", "Error al comprar ticket: " + error);
                            Toast.makeText(PackagesActivity.this, "Error al comprar ticket: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        dialog.setCancelable(true);
        dialog.show();
    }

    private void showPackageTicketQR() {
        if (studentEmail.isEmpty()) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        // OBTENER ticket disponible del paquete SIN consumirlo
        com.example.proyectointegrador.Services.TicketApiService.getPackageTicket(
                studentEmail,
                new com.example.proyectointegrador.Services.TicketApiService.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            org.json.JSONObject json = new org.json.JSONObject(response);
                            org.json.JSONObject ticket = json.getJSONObject("ticket");
                            String ticketId = ticket.getString("id");

                            // Mostrar dialog con QR (el ticket NO se marca como usado aún)
                            showQRDialog(ticketId);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(PackagesActivity.this, "Error al obtener ticket", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(PackagesActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void showQRDialog(String ticketId) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_qr_display);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.ImageView imgQR = dialog.findViewById(R.id.imgQRCode);
        TextView tvTicketId = dialog.findViewById(R.id.tvTicketId);
        MaterialButton btnClose = dialog.findViewById(R.id.btnClose);

        tvTicketId.setText("Ticket: " + ticketId);

        // Generar QR
        try {
            com.google.zxing.BarcodeFormat format = com.google.zxing.BarcodeFormat.QR_CODE;
            com.journeyapps.barcodescanner.BarcodeEncoder encoder = new com.journeyapps.barcodescanner.BarcodeEncoder();
            android.graphics.Bitmap bitmap = encoder.encodeBitmap(ticketId, format, 400, 400);
            imgQR.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar QR", Toast.LENGTH_SHORT).show();
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserPackages();
    }

    @Override
    protected int getNavigationIndex() {
        return 2;
    }
}