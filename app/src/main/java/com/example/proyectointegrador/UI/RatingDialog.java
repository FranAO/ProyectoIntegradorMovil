package com.example.proyectointegrador.UI;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.proyectointegrador.R;
import com.google.android.material.button.MaterialButton;

public class RatingDialog extends Dialog {

    private String tripId;
    private String driverId;
    private String studentId;
    private RatingListener listener;

    private ImageView star1, star2, star3, star4, star5;
    private int selectedRating = 0;
    private EditText etComment;
    private MaterialButton btnSubmit, btnSkip;
    private TextView tvTitle, tvRatingText;

    public interface RatingListener {
        void onRatingSubmitted(float rating, String comment);
        void onRatingSkipped();
    }

    public RatingDialog(@NonNull Context context, String tripId, String driverId, String studentId, RatingListener listener) {
        super(context);
        this.tripId = tripId;
        this.driverId = driverId;
        this.studentId = studentId;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_rating);
        setCancelable(false);

        // Inicializar vistas
        star1 = findViewById(R.id.star1);
        star2 = findViewById(R.id.star2);
        star3 = findViewById(R.id.star3);
        star4 = findViewById(R.id.star4);
        star5 = findViewById(R.id.star5);
        etComment = findViewById(R.id.etComment);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnSkip = findViewById(R.id.btnSkip);
        tvTitle = findViewById(R.id.tvTitle);
        tvRatingText = findViewById(R.id.tvRatingText);

        tvTitle.setText("¿Cómo fue tu viaje?");

        // Configurar listeners para las estrellas
        setupStarListeners();

        btnSubmit.setOnClickListener(v -> {
            if (selectedRating == 0) {
                Toast.makeText(getContext(), "Por favor selecciona una calificación", Toast.LENGTH_SHORT).show();
                return;
            }
            String comment = etComment.getText().toString().trim();
            if (listener != null) {
                listener.onRatingSubmitted(selectedRating, comment);
            }
            dismiss();
        });

        btnSkip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRatingSkipped();
            }
            dismiss();
        });
    }

    private void setupStarListeners() {
        star1.setOnClickListener(v -> setRating(1));
        star2.setOnClickListener(v -> setRating(2));
        star3.setOnClickListener(v -> setRating(3));
        star4.setOnClickListener(v -> setRating(4));
        star5.setOnClickListener(v -> setRating(5));
    }

    private void setRating(int rating) {
        selectedRating = rating;
        updateStarsUI();
        updateRatingText();
    }

    private void updateStarsUI() {
        ImageView[] stars = {star1, star2, star3, star4, star5};
        
        for (int i = 0; i < stars.length; i++) {
            if (i < selectedRating) {
                // Estrella seleccionada
                stars[i].setImageResource(R.drawable.ic_star_filled);
                stars[i].setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_orange_light));
            } else {
                // Estrella no seleccionada
                stars[i].setImageResource(R.drawable.ic_star_outline);
                stars[i].setColorFilter(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
        }
    }

    private void updateRatingText() {
        String[] ratingTexts = {
            "Selecciona una calificación",
            "Muy malo",
            "Malo",
            "Regular",
            "Bueno",
            "Excelente"
        };
        
        if (selectedRating >= 0 && selectedRating <= 5) {
            tvRatingText.setText(ratingTexts[selectedRating]);
            
            // Cambiar color según calificación
            if (selectedRating == 0) {
                tvRatingText.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            } else {
                tvRatingText.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_color));
            }
        }
    }

    public String getTripId() {
        return tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getStudentId() {
        return studentId;
    }
}
