package com.example.demo.model;

public enum PaymentMethod {
    CASH_ON_DELIVERY("貨到付款"),
    PICKUP("自取");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
