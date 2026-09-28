package com.example.miscompras.domain;

public enum ItemStatus {
    PURCHASED("Comprado"),
    NOT_FOUND("No encontrado"),
    PENDING("Pendiente");

    private final String label;

    ItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
