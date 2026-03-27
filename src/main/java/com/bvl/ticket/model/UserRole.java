package com.bvl.ticket.model;

/**
 * Definiert die verschiedenen Rollen im System.
 * Jedes Referat hat eine eigene Rolle für die Daten-Trennung.
 */
/**
 * @author Omar
 */
public enum UserRole {
    ADMIN("Admin"),
    REFERAT401("Referat401"),
    REFERAT402("Referat402"),
    REFERAT403("Referat403"),
    REFERAT404("Referat404"),
    REFERAT405("Referat405");

    private String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}



