package com.example.proyectointegrador.UI;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.proyectointegrador.R;

public abstract class BaseNavigationActivity extends AppCompatActivity {

    protected View slidingIndicator;
    protected ConstraintLayout[] navButtons = new ConstraintLayout[5];
    protected ImageView[] icons = new ImageView[5];
    protected TextView[] labels = new TextView[5];
    protected int currentSelectedIndex = 0;

    protected abstract int getNavigationIndex();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
    }

    protected void setupNavigation() {
        initializeNavigationViews();
        setupNavigationListeners();
        selectButton(getNavigationIndex());
    }

    private void initializeNavigationViews() {
        slidingIndicator = findViewById(R.id.sliding_indicator);

        // Inicializar botones
        navButtons[0] = findViewById(R.id.nav_button_1);
        navButtons[1] = findViewById(R.id.nav_button_2);
        navButtons[2] = findViewById(R.id.nav_button_3);
        navButtons[3] = findViewById(R.id.nav_button_4);
        navButtons[4] = findViewById(R.id.nav_button_5);

        // Inicializar iconos
        icons[0] = findViewById(R.id.icon_1);
        icons[1] = findViewById(R.id.icon_2);
        icons[2] = findViewById(R.id.icon_3);
        icons[3] = findViewById(R.id.icon_4);
        icons[4] = findViewById(R.id.icon_5);

        // Inicializar etiquetas
        labels[0] = findViewById(R.id.label_1);
        labels[1] = findViewById(R.id.label_2);
        labels[2] = findViewById(R.id.label_3);
        labels[3] = findViewById(R.id.label_4);
        labels[4] = findViewById(R.id.label_5);
    }

    private void setupNavigationListeners() {
        for (int i = 0; i < navButtons.length; i++) {
            final int index = i;

            navButtons[i].setOnClickListener(v -> {
                if (currentSelectedIndex != index) {
                    selectButton(index);
                    navigateToActivity(index);
                }
            });

            // Animaciones de hover
            navButtons[i].setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        animateButtonPress(index, true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        animateButtonPress(index, false);
                        break;
                }
                return false;
            });
        }
    }

    private void navigateToActivity(int index) {
        Intent intent;

        switch (index) {
            case 0:
                if (!(this instanceof MainActivity)) {
                    intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                break;
            case 1:
                if (!(this instanceof BusesActivity)) {
                    intent = new Intent(this, BusesActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                break;
            case 2:
                if (!(this instanceof PackagesActivity)) {
                    intent = new Intent(this, PackagesActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                break;
            case 3:
                if (!(this instanceof HistoryActivity)) {
                    intent = new Intent(this, HistoryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                break;
            case 4:
                if (!(this instanceof ReportActivity)) {
                    intent = new Intent(this, ReportActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                break;
        }
    }

    protected void selectButton(int index) {
        // Resetear todos los botones
        for (int i = 0; i < navButtons.length; i++) {
            resetButton(i);
        }

        // Activar el botón seleccionado
        activateButton(index);

        // Mover el indicador deslizante
        moveIndicator(index);

        currentSelectedIndex = index;
    }

    private void resetButton(int index) {
        // Resetear colores del icono y texto
        ImageViewCompat.setImageTintList(icons[index],
                ContextCompat.getColorStateList(this, R.color.text_inactive));
        labels[index].setTextColor(ContextCompat.getColor(this, R.color.text_inactive));
        labels[index].setAlpha(0.7f);

        // Resetear escala
        ObjectAnimator scaleXIcon = ObjectAnimator.ofFloat(icons[index], "scaleX", 1.0f);
        ObjectAnimator scaleYIcon = ObjectAnimator.ofFloat(icons[index], "scaleY", 1.0f);
        ObjectAnimator scaleXLabel = ObjectAnimator.ofFloat(labels[index], "scaleX", 1.0f);
        ObjectAnimator scaleYLabel = ObjectAnimator.ofFloat(labels[index], "scaleY", 1.0f);

        scaleXIcon.setDuration(100);
        scaleYIcon.setDuration(100);
        scaleXLabel.setDuration(100);
        scaleYLabel.setDuration(100);

        scaleXIcon.start();
        scaleYIcon.start();
        scaleXLabel.start();
        scaleYLabel.start();
    }

    private void activateButton(int index) {
        // Cambiar colores del botón activo
        ImageViewCompat.setImageTintList(icons[index],
                ContextCompat.getColorStateList(this, R.color.text_active));
        labels[index].setTextColor(ContextCompat.getColor(this, R.color.text_active));
        labels[index].setAlpha(1.0f);

        // Animar escala del botón activo
        ObjectAnimator scaleXIcon = ObjectAnimator.ofFloat(icons[index], "scaleX", 1.2f);
        ObjectAnimator scaleYIcon = ObjectAnimator.ofFloat(icons[index], "scaleY", 1.2f);
        ObjectAnimator scaleXLabel = ObjectAnimator.ofFloat(labels[index], "scaleX", 1.1f);
        ObjectAnimator scaleYLabel = ObjectAnimator.ofFloat(labels[index], "scaleY", 1.1f);

        scaleXIcon.setDuration(80);
        scaleYIcon.setDuration(80);
        scaleXLabel.setDuration(80);
        scaleYLabel.setDuration(80);

        scaleXIcon.setInterpolator(new DecelerateInterpolator());
        scaleYIcon.setInterpolator(new DecelerateInterpolator());
        scaleXLabel.setInterpolator(new DecelerateInterpolator());
        scaleYLabel.setInterpolator(new DecelerateInterpolator());

        scaleXIcon.start();
        scaleYIcon.start();
        scaleXLabel.start();
        scaleYLabel.start();

        // Animación de bounce para el icono
        ObjectAnimator bounceAnimator = ObjectAnimator.ofFloat(icons[index], "translationY", 0, -10, 0);
        bounceAnimator.setDuration(150);
        bounceAnimator.setInterpolator(new DecelerateInterpolator());
        bounceAnimator.start();
    }

    private void moveIndicator(int index) {
        for (int i = 0; i < navButtons.length; i++) {
            resetButton(i);
        }
        // Activar el botón objetivo
        activateButton(index);

        slidingIndicator.setVisibility(View.VISIBLE);

        // Calcular la posición X del botón objetivo
        navButtons[index].post(() -> {
            float targetX = navButtons[index].getX()
                    + (navButtons[index].getWidth() - slidingIndicator.getWidth()) / 2f;

            // Animar el movimiento del indicador
            ObjectAnimator animator = ObjectAnimator.ofFloat(slidingIndicator, "x", targetX);
            animator.setDuration(150);
            animator.setInterpolator(new DecelerateInterpolator());

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(slidingIndicator, "scaleX", 0.8f, 1.2f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(slidingIndicator, "scaleY", 0.8f, 1.2f, 1.0f);
            scaleX.setDuration(150);
            scaleY.setDuration(150);

            animator.start();
            scaleX.start();
            scaleY.start();
        });

        currentSelectedIndex = index; // Actualizar el índice actual
    }

    private void animateButtonPress(int index, boolean pressed) {
        float scale = pressed ? 0.95f : 1.0f;
        float alpha = pressed ? 0.7f : 1.0f;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(navButtons[index], "scaleX", scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(navButtons[index], "scaleY", scale);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(navButtons[index], "alpha", alpha);

        scaleX.setDuration(100);
        scaleY.setDuration(100);
        alphaAnim.setDuration(100);

        scaleX.start();
        scaleY.start();
        alphaAnim.start();
    }
}