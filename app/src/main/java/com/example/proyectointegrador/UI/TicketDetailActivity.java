package com.example.proyectointegrador.UI;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyectointegrador.Config.ApiConfig;
import com.example.proyectointegrador.Database.DBHelper;
import com.example.proyectointegrador.Models.Student;
import com.example.proyectointegrador.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TicketDetailActivity extends AppCompatActivity {

    private ImageView imgQR;
    private TextView tvIdTicket, tvViaje, tvFecha, tvEstudiante, tvStatus;
    private String ticketId;
    private String studentEmail;
    private String status;
    private String tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        imgQR = findViewById(R.id.imgQR);
        tvIdTicket = findViewById(R.id.tvIdTicket);
        tvViaje = findViewById(R.id.tvViaje);
        tvFecha = findViewById(R.id.tvFecha);
        tvEstudiante = findViewById(R.id.tvEstudiante);
        tvStatus = findViewById(R.id.tvStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ticketId = getIntent().getStringExtra("ticketId");
        studentEmail = getIntent().getStringExtra("studentEmail");
        status = getIntent().getStringExtra("status");
        tripId = getIntent().getStringExtra("tripId");

        if (status == null || status.isEmpty()) {
            status = "available";
        }

        cargarDatosTicket();
        if (tripId != null && !tripId.isEmpty()) {
            cargarInfoViaje();
        } else {
            tvViaje.setText("Viaje: Sin asignar");
        }
        generarQR();
    }

    private void cargarDatosTicket() {
        DBHelper dbHelper = new DBHelper(this);
        Student student = dbHelper.obtenerStudentPorEmail(studentEmail);

        if (student != null) {
            tvEstudiante.setText("Estudiante: " + student.getFirstName() + " " + student.getLastName());
        }

        tvIdTicket.setText("ID: " + ticketId);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvFecha.setText("Fecha: " + sdf.format(new Date()));

        String displayStatus = "available".equals(status) ? "Disponible" : "Usado";
        tvStatus.setText(displayStatus);
        if ("available".equals(status)) {
            tvStatus.setBackgroundResource(R.drawable.status_badge_available);
        } else {
            tvStatus.setBackgroundResource(R.drawable.status_badge_used);
        }
    }

    private void cargarInfoViaje() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(ApiConfig.getApiUrl(TicketDetailActivity.this, "/trip/" + tripId));
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                org.json.JSONObject tripJson = new org.json.JSONObject(response.toString());
                String routeId = tripJson.has("RouteId") ? tripJson.getString("RouteId") : tripJson.getString("routeId");
                String startTime = tripJson.has("StartTime") ? tripJson.getString("StartTime") : tripJson.getString("startTime");
                
                // Obtener información de la ruta
                java.net.URL urlRoute = new java.net.URL(ApiConfig.getApiUrl(TicketDetailActivity.this, "/busroute/" + routeId));
                java.net.HttpURLConnection connRoute = (java.net.HttpURLConnection) urlRoute.openConnection();
                connRoute.setRequestMethod("GET");

                java.io.BufferedReader readerRoute = new java.io.BufferedReader(new java.io.InputStreamReader(connRoute.getInputStream()));
                StringBuilder responseRoute = new StringBuilder();
                String lineRoute;
                while ((lineRoute = readerRoute.readLine()) != null) {
                    responseRoute.append(lineRoute);
                }
                readerRoute.close();

                org.json.JSONObject routeJson = new org.json.JSONObject(responseRoute.toString());
                String routeName = routeJson.has("Name") ? routeJson.getString("Name") : routeJson.getString("name");
                
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(startTime);
                String formattedDate = outputFormat.format(date);
                
                runOnUiThread(() -> {
                    tvViaje.setText("Viaje: " + routeName + " - " + formattedDate);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvViaje.setText("Viaje: " + tripId.substring(Math.max(0, tripId.length() - 6)));
                });
            }
        }).start();
    }

    private void generarQR() {
        try {
            // Solo incluir el ticketId en el QR
            // El backend obtiene toda la información del ticket usando este ID
            String dataQR = ticketId;

            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(dataQR, BarcodeFormat.QR_CODE, 400, 400);

            imgQR.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar QR", Toast.LENGTH_SHORT).show();
        }
    }
}
