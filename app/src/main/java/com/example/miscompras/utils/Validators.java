package com.example.miscompras.utils;

import com.example.miscompras.domain.DiscountType;

/** Validaciones de la aplicación (cantidades, precios y descuentos). */
public final class Validators {

    private Validators() {
    }

    public static boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2;
    }

    public static boolean isValidQuantity(double quantity) {
        return quantity > 0d && quantity < 100000d;
    }

    public static boolean isValidPrice(double price) {
        return price >= 0d && price < 100000000d;
    }

    public static boolean isValidDiscount(DiscountType type, double value) {
        if (value < 0d) {
            return false;
        }
        if (type == DiscountType.PERCENT) {
            return value <= 100d;
        }
        return true;
    }

    public static double parseMoney(String text) {
        if (text == null) {
            return 0d;
        }
        String cleaned = text.replace("$", "").replace(".", "").replace(",", ".").trim();
        if (cleaned.isEmpty()) {
            return 0d;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    public static String toEditableMoney(double value) {
        return String.format(Dates.LOCALE, "%.0f", value);
    }
}
