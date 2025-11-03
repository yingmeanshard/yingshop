package com.example.demo.model;

public enum OrderStatus {

    PENDING_PAYMENT("待付款"),
    PAID("已付款"),
    PENDING_CONFIRMATION("待確認"),
    PENDING_SHIPMENT("待出貨"),
    PROCESSING("待處理"),
    SHIPPED("已出貨");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OrderStatus fromDisplayName(String displayName) {
        for (OrderStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status display name: " + displayName);
    }
}