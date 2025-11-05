package com.example.demo.model;

public enum DeliveryPaymentMethod {
    CASH_ON_DELIVERY("貨到付款"),
    PICKUP_CASH("自取付現");

    private final String displayName;

    DeliveryPaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

