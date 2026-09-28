package com.example.miscompras.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class AppPreferences {

    private static final String PREF_NAME = "mis_compras_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_LANGUAGE = "app_language"; // "es", "en", "pt"
    private static final String KEY_CURRENCY_CODE = "currency_code"; // "CLP", "USD", "ARS", "PEN", etc.

    public static class ColorThemeOption {
        public final String key;
        public final String name;
        public final int colorRes;
        public final int themeResId;

        public ColorThemeOption(String key, String name, int colorRes, int themeResId) {
            this.key = key;
            this.name = name;
            this.colorRes = colorRes;
            this.themeResId = themeResId;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class CurrencyOption {
        public final String code;
        public final String name;
        public final String symbol;
        public final boolean decimals;

        public CurrencyOption(String code, String name, String symbol, boolean decimals) {
            this.code = code;
            this.name = name;
            this.symbol = symbol;
            this.decimals = decimals;
        }

        @Override
        public String toString() {
            return name + " (" + symbol + ")";
        }
    }

    public static class LanguageOption {
        public final String code;
        public final String displayName;

        public LanguageOption(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private AppPreferences() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isOnboardingCompleted(Context context) {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public static String getLanguage(Context context) {
        return getPrefs(context).getString(KEY_LANGUAGE, "es");
    }

    public static void setLanguage(Context context, String langCode) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, langCode).apply();
    }

    public static String getCurrencyCode(Context context) {
        return getPrefs(context).getString(KEY_CURRENCY_CODE, "CLP");
    }

    public static void setCurrencyCode(Context context, String currencyCode) {
        getPrefs(context).edit().putString(KEY_CURRENCY_CODE, currencyCode).apply();
    }

    private static final String KEY_APP_THEME = "app_theme_key";

    public static String getAppTheme(Context context) {
        return getPrefs(context).getString(KEY_APP_THEME, "green");
    }

    public static void setAppTheme(Context context, String themeKey) {
        getPrefs(context).edit().putString(KEY_APP_THEME, themeKey).apply();
    }

    public static void applyTheme(android.app.Activity activity) {
        String themeKey = getAppTheme(activity);
        for (ColorThemeOption option : getAvailableThemes()) {
            if (option.key.equalsIgnoreCase(themeKey)) {
                activity.setTheme(option.themeResId);
                return;
            }
        }
        activity.setTheme(com.example.miscompras.R.style.Theme_MisCompras_Green);
    }

    public static List<ColorThemeOption> getAvailableThemes() {
        List<ColorThemeOption> list = new ArrayList<>();
        list.add(new ColorThemeOption("green", "Verde Esmeralda", com.example.miscompras.R.color.brand_green, com.example.miscompras.R.style.Theme_MisCompras_Green));
        list.add(new ColorThemeOption("blue", "Azul Océano", com.example.miscompras.R.color.theme_blue_primary, com.example.miscompras.R.style.Theme_MisCompras_Blue));
        list.add(new ColorThemeOption("purple", "Violeta Real", com.example.miscompras.R.color.theme_purple_primary, com.example.miscompras.R.style.Theme_MisCompras_Purple));
        list.add(new ColorThemeOption("orange", "Naranja Atardecer", com.example.miscompras.R.color.theme_orange_primary, com.example.miscompras.R.style.Theme_MisCompras_Orange));
        list.add(new ColorThemeOption("red", "Rojo Pasión", com.example.miscompras.R.color.theme_red_primary, com.example.miscompras.R.style.Theme_MisCompras_Red));
        return list;
    }

    public static List<LanguageOption> getAvailableLanguages() {
        List<LanguageOption> list = new ArrayList<>();
        list.add(new LanguageOption("es", "Español"));
        list.add(new LanguageOption("en", "English"));
        list.add(new LanguageOption("pt", "Português"));
        return list;
    }

    public static List<CurrencyOption> getAvailableCurrencies() {
        List<CurrencyOption> list = new ArrayList<>();
        list.add(new CurrencyOption("CLP", "Peso Chileno (CLP)", "$", false));
        list.add(new CurrencyOption("USD", "Dólar Estadounidense (USD)", "US$", true));
        list.add(new CurrencyOption("ARS", "Peso Argentino (ARS)", "$", false));
        list.add(new CurrencyOption("PEN", "Sol Peruano (PEN)", "S/.", true));
        list.add(new CurrencyOption("EUR", "Euro (EUR)", "€", true));
        list.add(new CurrencyOption("MXN", "Peso Mexicano (MXN)", "$", true));
        list.add(new CurrencyOption("COP", "Peso Colombiano (COP)", "$", false));
        return list;
    }

    public static CurrencyOption getSelectedCurrency(Context context) {
        String code = getCurrencyCode(context);
        for (CurrencyOption opt : getAvailableCurrencies()) {
            if (opt.code.equalsIgnoreCase(code)) {
                return opt;
            }
        }
        return getAvailableCurrencies().get(0);
    }
}
