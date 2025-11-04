package com.example.demo.model;

import java.util.Locale;

public enum UserRole {
    ADMIN("ROLE_ADMIN"),
    STAFF("ROLE_STAFF"),
    CUSTOMER("ROLE_CUSTOMER");

    private final String persistedValue;

    UserRole(String persistedValue) {
        this.persistedValue = persistedValue;
    }

    public String getPersistedValue() {
        return persistedValue;
    }

    public String getAuthority() {
        return persistedValue;
    }

    public static UserRole fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return CUSTOMER;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (UserRole role : values()) {
            if (role.name().equals(normalized) || role.persistedValue.toUpperCase(Locale.ROOT).equals(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role value: " + value);
    }
}
