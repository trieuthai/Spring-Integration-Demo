package com.kms.demo.entity;

import java.util.List;

public class DeliveryOrder{
    private String id;
    private String from;
    private List<OrderItem> items;

    public DeliveryOrder(Order order) {
        this.id = order.getId();
        this.items = order.getItems();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "DeliveryOrder{" +
                "id='" + id + '\'' +
                ", from='" + from + '\'' +
                '}';
    }
}
