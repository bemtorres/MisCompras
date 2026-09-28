package com.example.miscompras.utils;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public final class Money {

    private static volatile String cachedSymbol = "$";
    private static volatile boolean cachedHasDecimals = false;

    private Money() {
    }

    public static void updateCurrency(Context context) {
        if (context == null) return;
        AppPreferences.CurrencyOption option = AppPreferences.getSelectedCurrency(context);
        cachedSymbol = option.symbol + " ";
        cachedHasDecimals = option.decimals;
    }

    public static String getCurrencySymbol() {
        return cachedSymbol;
    }

    /** Formato de moneda configurada (ej: $ 1.290, -$ 45.900 o US$ 12.50). */
    public static String format(double value) {
        if (cachedHasDecimals) {
            return decimalFormat().format(value);
        }
        return integerFormat().format(Math.round(value));
    }

    /** Número visual sin símbolo de moneda pero con separador de miles (ej: 1.290 o 1.290,00). */
    public static String plain(double value) {
        if (cachedHasDecimals) {
            return new DecimalFormat("#,##0.00", symbols()).format(value);
        }
        return new DecimalFormat("#,##0", symbols()).format(Math.round(value));
    }

    /** Número entero o decimal crudo sin puntos de miles, ideal para campos de edición (ej: 1290 o 12.5). */
    public static String raw(double value) {
        if (cachedHasDecimals) {
            if (value == Math.rint(value)) {
                return String.valueOf((long) value);
            }
            return String.format(java.util.Locale.US, "%.2f", value);
        }
        return String.valueOf(Math.round(value));
    }

    public static String signed(double value) {
        return (value > 0 ? "+" : "") + format(value);
    }

    public static String quantity(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return new DecimalFormat("#,##0.#", symbols()).format(value);
    }

    /**
     * Forma abreviada para espacios reducidos:
     * $ 1,2 M, $ 950 k o formato normal cuando el monto es pequeño.
     */
    public static String compact(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000d) {
            return scaledFormat().format(value / 1_000_000d) + " M";
        }
        if (abs >= 100_000d) {
            return scaledFormat().format(Math.round(value / 1000d)) + " k";
        }
        return format(value);
    }

    private static DecimalFormatSymbols symbols() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Dates.LOCALE);
        if (!".".equals(symbols.getGroupingSeparator())) {
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');
        }
        return symbols;
    }

    /** El signo va antes del simbolo: -$ 45.900 y no $-45.900. */
    private static void withSymbol(DecimalFormat format) {
        format.setPositivePrefix(cachedSymbol);
        format.setNegativePrefix("-" + cachedSymbol);
    }

    private static DecimalFormat integerFormat() {
        DecimalFormat format = new DecimalFormat("#,##0", symbols());
        withSymbol(format);
        return format;
    }

    private static DecimalFormat decimalFormat() {
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols());
        withSymbol(format);
        return format;
    }

    private static DecimalFormat scaledFormat() {
        DecimalFormat format = new DecimalFormat("#,##0.#", symbols());
        withSymbol(format);
        return format;
    }
}
