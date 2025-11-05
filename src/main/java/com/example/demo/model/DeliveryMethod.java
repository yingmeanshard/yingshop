package com.example.demo.model;

public enum DeliveryMethod {
    CASH_ON_DELIVERY("貨到付款"),
    PICKUP("自取");

    private final String displayName;

    DeliveryMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

