package com.example.miscompras.domain;

import com.example.miscompras.utils.Dates;

public final class Pricing {

    private Pricing() {
    }

    public static double subtotal(double quantity, double unitPrice) {
        if (quantity <= 0 || unitPrice <= 0) {
            return 0d;
        }
        return round(quantity * unitPrice);
    }

    /** Precio de una unidad después de aplicar el descuento. Nunca negativo. */
    public static double finalUnitPrice(double unitPrice, DiscountType type, double discountValue) {
        double base = Math.max(0d, unitPrice);
        double value = Math.max(0d, discountValue);
        double result = base;
        switch (type == null ? DiscountType.NONE : type) {
            case PERCENT:
                double percent = Math.min(100d, value);
                result = base * (1d - percent / 100d);
                break;
            case FIXED:
                result = base - value;
                break;
            case NONE:
            default:
                break;
        }
        return round(Math.max(0d, result));
    }

    /** Monto de descuento efectivamente aplicado sobre el precio de una unidad. */
    public static double discountAmount(double unitPrice, DiscountType type, double discountValue) {
        return round(Math.max(0d, Math.max(0d, unitPrice) - finalUnitPrice(unitPrice, type, discountValue)));
    }

    public static double discountAmount(double quantity, double unitPrice, DiscountType type, double discountValue) {
        return round(discountAmount(unitPrice, type, discountValue) * Math.max(0d, quantity));
    }

    public static double difference(double estimated, double real) {
        return round(real - estimated);
    }

    public static double percentDifference(double estimated, double real) {
        if (estimated <= 0d) {
            return 0d;
        }
        return round((real - estimated) / estimated * 100d);
    }

    public static double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.round(value * 100d) / 100d;
    }

    public static String formatPercent(double value) {
        return String.format(Dates.LOCALE, "%.1f%%", value);
    }
}
