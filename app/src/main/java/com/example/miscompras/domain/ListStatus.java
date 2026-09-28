package com.example.miscompras.domain;

public enum ListStatus {
    PLANNED("Planificada"),
    IN_PROGRESS("En curso"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada");

    private final String label;

    ListStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ListStatus fromName(String name) {
        for (ListStatus status : values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        return PLANNED;
    }
}
