package com.example.proyectointegrador.UI;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectointegrador.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.UUID;

public class PaymentQRActivity extends AppCompatActivity {

    private ImageView imgPaymentQR;
    private TextView tvPackageInfo, tvAmount, tvPaymentId;
    private String paymentId;
    private String packageId;
    private String packageName;
    private int ticketCount;
    private double price;
    private int durationDays;
    private String studentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_qr);
        
        imgPaymentQR = findViewById(R.id.imgPaymentQR);
        tvPackageInfo = findViewById(R.id.tvPackageInfo);
        tvAmount = findViewById(R.id.tvAmount);
        tvPaymentId = findViewById(R.id.tvPaymentId);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> confirmPayment());

        packageId = getIntent().getStringExtra("packageId");
        packageName = getIntent().getStringExtra("packageName");
        ticketCount = getIntent().getIntExtra("ticketCount", 0);
        price = getIntent().getDoubleExtra("price", 0.0);
        durationDays = getIntent().getIntExtra("durationDays", 30);
        studentEmail = getIntent().getStringExtra("studentEmail");

        paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        setupUI();
        generateQR();
    }

    private void setupUI() {
        tvPackageInfo.setText("Paquete: " + packageName);
        tvAmount.setText(String.format("Monto: Bs. %.2f", price));
        tvPaymentId.setText("ID: " + paymentId);
    }

    private void generateQR() {
        try {
            String qrData = paymentId + "|" + price + "|" + packageName;

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 600, 600);

            imgPaymentQR.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar código QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmPayment() {
        if (studentEmail == null || studentEmail.isEmpty()) {
            Toast.makeText(this, "Error: Email de estudiante no encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (ticketCount <= 0) {
            Toast.makeText(this, "Error: Cantidad de tickets inválida", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Procesando compra...", Toast.LENGTH_SHORT).show();

        com.example.proyectointegrador.Services.TicketApiService.purchasePackage(
                studentEmail,
                ticketCount,
                durationDays,
                new com.example.proyectointegrador.Services.TicketApiService.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        Toast.makeText(PaymentQRActivity.this, "¡Paquete activado exitosamente!", Toast.LENGTH_LONG).show();
                        
                        Intent intent = new Intent(PaymentQRActivity.this, PackagesActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        String errorMsg = "Error al activar el paquete";
                        if (error != null && !error.isEmpty()) {
                            errorMsg += ": " + error;
                        }
                        errorMsg += "\n\nAsegúrate de que el servidor esté ejecutándose.";
                        Toast.makeText(PaymentQRActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
