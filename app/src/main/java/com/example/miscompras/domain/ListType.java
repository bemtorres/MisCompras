package com.example.miscompras.domain;

public enum ListType {
    WEEKLY("Semanal"),
    BIWEEKLY("Quincenal"),
    MONTHLY("Mensual"),
    OTHER("Otra");

    private final String label;

    ListType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ListType fromName(String name) {
        for (ListType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return WEEKLY;
    }
}
