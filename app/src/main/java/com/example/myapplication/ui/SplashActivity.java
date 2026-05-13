package com.example.myapplication.ui;

import android.content.Intent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.MainActivity;
import com.example.myapplication.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Icon: scale from 0.8 → 1.0 + fade in
        binding.ivSplashIcon.setAlpha(0f);
        binding.ivSplashIcon.setScaleX(0.8f);
        binding.ivSplashIcon.setScaleY(0.8f);
        binding.ivSplashIcon.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Tagline: fade in + slide up after 300ms delay
        binding.tvSplashTagline.setTranslationY(20f);
        binding.tvSplashTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(300)
                .start();

        // Navigate to MainActivity after 2s
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.getRoot().animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                    })
                    .start();
        }, 2000);
    }

    @Override
    public void onBackPressed() {
        // Disable back press on splash
    }
}
