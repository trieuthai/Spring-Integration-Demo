package com.kms.demo.entity;

public class OrderItem {
    private String name;
    private String status;

    public OrderItem() {
    }

    public OrderItem(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
