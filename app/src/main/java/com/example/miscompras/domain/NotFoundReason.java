package com.example.miscompras.domain;

public enum NotFoundReason {
    OUT_OF_STOCK("Sin stock"),
    NOT_AVAILABLE("No estaba disponible"),
    CHANGED_MIND("Cambié de opinión"),
    OTHER("Otro");

    private final String label;

    NotFoundReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static NotFoundReason fromName(String name) {
        if (name == null) {
            return null;
        }
        for (NotFoundReason reason : values()) {
            if (reason.name().equals(name)) {
                return reason;
            }
        }
        return OTHER;
    }
}
