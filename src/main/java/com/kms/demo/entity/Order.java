package com.kms.demo.entity;

import java.util.List;

public class Order {
    private String id;
    private String type;
    private List<OrderItem> items;

    public Order() {
    }

    public Order(String id, String type, List<OrderItem> items) {
        this.id = id;
        this.type = type;
        this.items = items;
    }

    public Order(List<OrderItem> items) {
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    protected String toStringItems(){
        StringBuilder builder = new StringBuilder("[");
        items.forEach(item -> builder.append(item.getName()));
        builder.deleteCharAt(builder.lastIndexOf(","));
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
