package com.example.miscompras.domain;

public enum DiscountType {
    NONE("Sin descuento"),
    PERCENT("Porcentaje"),
    FIXED("Monto fijo");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static DiscountType fromName(String name) {
        for (DiscountType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return NONE;
    }
}
