package com.example.proyectointegrador.UI;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private TextView tvIdTicket, tvFecha, tvEstudiante, tvStatus;
    private String ticketId;
    private String studentEmail;
    private String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        imgQR = findViewById(R.id.imgQR);
        tvIdTicket = findViewById(R.id.tvIdTicket);
        tvFecha = findViewById(R.id.tvFecha);
        tvEstudiante = findViewById(R.id.tvEstudiante);
        tvStatus = findViewById(R.id.tvStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ticketId = getIntent().getStringExtra("ticketId");
        studentEmail = getIntent().getStringExtra("studentEmail");
        status = getIntent().getStringExtra("status");

        if (status == null || status.isEmpty()) {
            status = "available";
        }

        cargarDatosTicket();
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
