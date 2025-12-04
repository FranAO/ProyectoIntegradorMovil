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

public abstract class BaseNavigationActivity_C extends AppCompatActivity {

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

        navButtons[0] = findViewById(R.id.nav_button_1);
        navButtons[1] = findViewById(R.id.nav_button_2);
        navButtons[2] = findViewById(R.id.nav_button_3);
        navButtons[3] = findViewById(R.id.nav_button_4);
        navButtons[4] = findViewById(R.id.nav_button_5);

        icons[0] = findViewById(R.id.icon_1);
        icons[1] = findViewById(R.id.icon_2);
        icons[2] = findViewById(R.id.icon_3);
        icons[3] = findViewById(R.id.icon_4);
        icons[4] = findViewById(R.id.icon_5);

        labels[0] = findViewById(R.id.label_1);
        labels[1] = findViewById(R.id.label_2);
        labels[2] = findViewById(R.id.label_3);
        labels[3] = findViewById(R.id.label_4);
        labels[4] = findViewById(R.id.label_5);
    }

    private void setupNavigationListeners() {
        for (int i = 0; i < navButtons.length; i++) {
            final int index = i;
            
            // Add null check to prevent NullPointerException
            if (navButtons[i] == null) {
                continue;
            }

            navButtons[i].setOnClickListener(v -> {
                if (currentSelectedIndex != index) {
                    selectButton(index);
                    navigateToActivity(index);
                }
            });

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
        Class<?> targetActivity = null;

        switch (index) {
            case 0:
                targetActivity = MainActivity_C.class;
                break;
            case 1:
                targetActivity = RouteActivity_C.class;
                break;
            case 2:
                targetActivity = PassengersActivity.class;
                break;
            case 3:
                targetActivity = CameraActivity.class;
                break;
            case 4:
                targetActivity = WarningActivity.class;
                break;
        }

        if (targetActivity != null && this.getClass() != targetActivity) {
            Intent intent = new Intent(this, targetActivity);
            // Flags para una navegación más fluida (evita apilar activities infinitamente)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            // Animación suave (opcional, pero recomendada)
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            // Finalizamos la actual para no dejar basura en el stack
            finish();
        }
    }

    protected void selectButton(int index) {
        for (int i = 0; i < navButtons.length; i++) {
            resetButton(i);
        }
        activateButton(index);
        moveIndicator(index);
        currentSelectedIndex = index;
    }

    private void resetButton(int index) {
        ImageViewCompat.setImageTintList(icons[index],
                ContextCompat.getColorStateList(this, R.color.text_inactive));
        labels[index].setTextColor(ContextCompat.getColor(this, R.color.text_inactive));
        labels[index].setAlpha(0.7f);

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
        ImageViewCompat.setImageTintList(icons[index],
                ContextCompat.getColorStateList(this, R.color.text_active));
        labels[index].setTextColor(ContextCompat.getColor(this, R.color.text_active));
        labels[index].setAlpha(1.0f);

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

        ObjectAnimator bounceAnimator = ObjectAnimator.ofFloat(icons[index], "translationY", 0, -10, 0);
        bounceAnimator.setDuration(150);
        bounceAnimator.setInterpolator(new DecelerateInterpolator());
        bounceAnimator.start();
    }

    private void moveIndicator(int index) {
        for (int i = 0; i < navButtons.length; i++) {
            resetButton(i);
        }
        activateButton(index);

        slidingIndicator.setVisibility(View.VISIBLE);

        navButtons[index].post(() -> {
            float targetX = navButtons[index].getX() + (navButtons[index].getWidth() - slidingIndicator.getWidth()) / 2f;

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

        currentSelectedIndex = index;
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