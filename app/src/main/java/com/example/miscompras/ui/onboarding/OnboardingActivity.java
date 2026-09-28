package com.example.miscompras.ui.onboarding;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.miscompras.MainActivity;
import com.example.miscompras.R;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.ui.common.Ui;
import com.example.miscompras.utils.AppPreferences;
import com.example.miscompras.utils.Money;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private MaterialButton btnSkip;
    private MaterialButton btnNext;
    private OnboardingAdapter adapter;
    private int totalPages = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        SystemBars.pad(findViewById(R.id.onboarding_root));

        viewPager = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);

        List<OnboardingAdapter.SlideItem> slides = new ArrayList<>();
        slides.add(new OnboardingAdapter.SlideItem("📋", "PASO 1 DE 3",
                R.string.onboarding_slide1_title, R.string.onboarding_slide1_desc));
        slides.add(new OnboardingAdapter.SlideItem("🛒", "PASO 2 DE 3",
                R.string.onboarding_slide2_title, R.string.onboarding_slide2_desc));
        slides.add(new OnboardingAdapter.SlideItem("📊", "PASO 3 DE 3",
                R.string.onboarding_slide3_title, R.string.onboarding_slide3_desc));

        adapter = new OnboardingAdapter(this, slides);
        totalPages = adapter.getItemCount();
        viewPager.setAdapter(adapter);

        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUIForPage(position);
            }
        });

        btnSkip.setOnClickListener(v -> {
            // Salta directamente a la última página de configuración
            viewPager.setCurrentItem(totalPages - 1, true);
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < totalPages - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        updateUIForPage(viewPager.getCurrentItem());

        if (savedInstanceState != null) {
            int savedPage = savedInstanceState.getInt("saved_page", 0);
            viewPager.setCurrentItem(savedPage, false);
            updateUIForPage(savedPage);
        }
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (viewPager != null) {
            outState.putInt("saved_page", viewPager.getCurrentItem());
        }
    }

    public void applySelectedTheme(String themeKey) {
        // Al tocar un punto de color en Onboarding, recreamos la pantalla para ver el color en vivo
        recreate();
    }

    private void updateUIForPage(int position) {
        setupDots(position);
        if (position == 0) {
            btnSkip.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.welcome_cta_start);
        } else if (position == totalPages - 1) {
            btnSkip.setVisibility(View.GONE);
            btnNext.setText(R.string.welcome_cta_finish);
        } else {
            btnSkip.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.welcome_cta_next);
        }
    }

    private void setupDots(int current) {
        dotsContainer.removeAllViews();
        int activeColor = Ui.color(this, androidx.appcompat.R.attr.colorPrimary);
        int inactiveColor = Ui.color(this, com.google.android.material.R.attr.colorOutlineVariant);

        for (int i = 0; i < totalPages; i++) {
            ImageView dot = new ImageView(this);
            int width = (i == current) ? dpToPx(20) : dpToPx(8);
            int height = dpToPx(8);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(dpToPx(4));
            drawable.setColor(i == current ? activeColor : inactiveColor);
            dot.setImageDrawable(drawable);

            dotsContainer.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void finishOnboarding() {
        AppPreferences.setOnboardingCompleted(this, true);
        Money.updateCurrency(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
