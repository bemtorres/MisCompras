package com.example.miscompras.ui.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.miscompras.R;
import com.example.miscompras.ui.common.SystemBars;
import com.example.miscompras.utils.AppPreferences;
import com.example.miscompras.utils.Money;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private androidx.recyclerview.widget.RecyclerView recyclerThemeColors;
    private Spinner spinnerCurrency;
    private Spinner spinnerLanguage;
    private TextView themePreview;
    private TextView currencyPreview;

    private boolean isInitialCurrencyLoad = true;
    private boolean isInitialLanguageLoad = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SystemBars.pad(findViewById(R.id.settings_root));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerThemeColors = findViewById(R.id.recycler_theme_colors);
        spinnerCurrency = findViewById(R.id.spinner_currency);
        spinnerLanguage = findViewById(R.id.spinner_language);
        themePreview = findViewById(R.id.theme_preview);
        currencyPreview = findViewById(R.id.currency_preview);

        setupThemeColorPicker();
        setupCurrencySpinner();
        setupLanguageSpinner();
    }

    private void setupThemeColorPicker() {
        List<AppPreferences.ColorThemeOption> themes = AppPreferences.getAvailableThemes();
        String currentTheme = AppPreferences.getAppTheme(this);

        for (AppPreferences.ColorThemeOption option : themes) {
            if (option.key.equalsIgnoreCase(currentTheme)) {
                themePreview.setText("Color actual: " + option.name);
                break;
            }
        }

        recyclerThemeColors.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));

        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(themes, currentTheme, option -> {
            String oldKey = AppPreferences.getAppTheme(SettingsActivity.this);
            if (!option.key.equalsIgnoreCase(oldKey)) {
                AppPreferences.setAppTheme(SettingsActivity.this, option.key);
                // Recrear la actividad de inmediato para cambiar el color de toda la UI en tiempo real
                recreate();
            }
        });
        recyclerThemeColors.setAdapter(colorAdapter);
    }

    private void setupCurrencySpinner() {
        List<AppPreferences.CurrencyOption> currencies = AppPreferences.getAvailableCurrencies();
        ArrayAdapter<AppPreferences.CurrencyOption> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, currencies);
        spinnerCurrency.setAdapter(adapter);

        String currentCode = AppPreferences.getCurrencyCode(this);
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).code.equalsIgnoreCase(currentCode)) {
                spinnerCurrency.setSelection(i);
                break;
            }
        }

        updateCurrencyPreview();

        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialCurrencyLoad) {
                    isInitialCurrencyLoad = false;
                    return;
                }
                AppPreferences.CurrencyOption selected = currencies.get(position);
                AppPreferences.setCurrencyCode(SettingsActivity.this, selected.code);
                Money.updateCurrency(SettingsActivity.this);
                updateCurrencyPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateCurrencyPreview() {
        if (currencyPreview != null) {
            currencyPreview.setText("Vista previa: " + Money.format(1290) + " / " + Money.format(24990));
        }
    }

    private void setupLanguageSpinner() {
        List<AppPreferences.LanguageOption> languages = AppPreferences.getAvailableLanguages();
        ArrayAdapter<AppPreferences.LanguageOption> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

        String currentLang = AppPreferences.getLanguage(this);
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code.equalsIgnoreCase(currentLang)) {
                spinnerLanguage.setSelection(i);
                break;
            }
        }

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isInitialLanguageLoad) {
                    isInitialLanguageLoad = false;
                    return;
                }
                AppPreferences.LanguageOption selected = languages.get(position);
                AppPreferences.setLanguage(SettingsActivity.this, selected.code);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}
