package com.example.miscompras.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miscompras.R;
import com.example.miscompras.ui.settings.ColorPickerAdapter;
import com.example.miscompras.utils.AppPreferences;
import com.example.miscompras.utils.Money;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_WELCOME = 0;
    public static final int TYPE_SLIDE = 1;
    public static final int TYPE_CONFIG = 2;

    public static class SlideItem {
        public final String icon;
        public final String step;
        public final int titleRes;
        public final int descRes;

        public SlideItem(String icon, String step, int titleRes, int descRes) {
            this.icon = icon;
            this.step = step;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    private final Context context;
    private final List<SlideItem> slides;

    public OnboardingAdapter(Context context, List<SlideItem> slides) {
        this.context = context;
        this.slides = slides;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_WELCOME;
        }
        if (position <= slides.size()) {
            return TYPE_SLIDE;
        }
        return TYPE_CONFIG;
    }

    @Override
    public int getItemCount() {
        // 1 (Welcome) + N (Slides) + 1 (Config)
        return 1 + slides.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_WELCOME) {
            View view = inflater.inflate(R.layout.item_onboarding_welcome, parent, false);
            return new WelcomeViewHolder(view);
        } else if (viewType == TYPE_SLIDE) {
            View view = inflater.inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_onboarding_config, parent, false);
            return new ConfigViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SlideViewHolder) {
            SlideItem item = slides.get(position - 1);
            ((SlideViewHolder) holder).bind(item);
        } else if (holder instanceof ConfigViewHolder) {
            ((ConfigViewHolder) holder).bind(context);
        }
    }

    static class WelcomeViewHolder extends RecyclerView.ViewHolder {
        WelcomeViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        private final TextView iconView;
        private final TextView stepView;
        private final TextView titleView;
        private final TextView descView;

        SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.slide_icon);
            stepView = itemView.findViewById(R.id.slide_step);
            titleView = itemView.findViewById(R.id.slide_title);
            descView = itemView.findViewById(R.id.slide_desc);
        }

        void bind(SlideItem item) {
            iconView.setText(item.icon);
            stepView.setText(item.step);
            titleView.setText(item.titleRes);
            descView.setText(item.descRes);
        }
    }

    static class ConfigViewHolder extends RecyclerView.ViewHolder {
        private final Spinner spinnerLanguage;
        private final Spinner spinnerCurrency;
        private final androidx.recyclerview.widget.RecyclerView recyclerThemeColors;
        private final TextView previewText;
        private final TextView themePreviewText;

        ConfigViewHolder(@NonNull View itemView) {
            super(itemView);
            spinnerLanguage = itemView.findViewById(R.id.spinner_language);
            spinnerCurrency = itemView.findViewById(R.id.spinner_currency);
            recyclerThemeColors = itemView.findViewById(R.id.recycler_theme_colors);
            previewText = itemView.findViewById(R.id.currency_preview);
            themePreviewText = itemView.findViewById(R.id.theme_preview);
        }

        void bind(Context context) {
            List<AppPreferences.LanguageOption> languages = AppPreferences.getAvailableLanguages();
            ArrayAdapter<AppPreferences.LanguageOption> langAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_dropdown_item, languages);
            spinnerLanguage.setAdapter(langAdapter);

            String currentLang = AppPreferences.getLanguage(context);
            for (int i = 0; i < languages.size(); i++) {
                if (languages.get(i).code.equalsIgnoreCase(currentLang)) {
                    spinnerLanguage.setSelection(i);
                    break;
                }
            }

            spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    AppPreferences.setLanguage(context, languages.get(position).code);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            List<AppPreferences.CurrencyOption> currencies = AppPreferences.getAvailableCurrencies();
            ArrayAdapter<AppPreferences.CurrencyOption> currAdapter = new ArrayAdapter<>(
                    context, android.R.layout.simple_spinner_dropdown_item, currencies);
            spinnerCurrency.setAdapter(currAdapter);

            String currentCurr = AppPreferences.getCurrencyCode(context);
            for (int i = 0; i < currencies.size(); i++) {
                if (currencies.get(i).code.equalsIgnoreCase(currentCurr)) {
                    spinnerCurrency.setSelection(i);
                    break;
                }
            }
            previewText.setText("Vista previa: " + Money.format(1290) + " / " + Money.format(24990));

            spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    AppPreferences.CurrencyOption selected = currencies.get(position);
                    AppPreferences.setCurrencyCode(context, selected.code);
                    Money.updateCurrency(context);
                    previewText.setText("Vista previa: " + Money.format(1290) + " / " + Money.format(24990));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            List<AppPreferences.ColorThemeOption> themes = AppPreferences.getAvailableThemes();
            String currentTheme = AppPreferences.getAppTheme(context);

            for (AppPreferences.ColorThemeOption option : themes) {
                if (option.key.equalsIgnoreCase(currentTheme)) {
                    if (themePreviewText != null) {
                        themePreviewText.setText("Color seleccionado: " + option.name);
                    }
                    break;
                }
            }

            recyclerThemeColors.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                    context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));

            ColorPickerAdapter colorAdapter = new ColorPickerAdapter(themes, currentTheme, option -> {
                String oldTheme = AppPreferences.getAppTheme(context);
                if (!option.key.equalsIgnoreCase(oldTheme)) {
                    AppPreferences.setAppTheme(context, option.key);
                    if (themePreviewText != null) {
                        themePreviewText.setText("Color seleccionado: " + option.name);
                    }
                    if (context instanceof OnboardingActivity) {
                        ((OnboardingActivity) context).applySelectedTheme(option.key);
                    }
                }
            });
            recyclerThemeColors.setAdapter(colorAdapter);
        }
    }
}
