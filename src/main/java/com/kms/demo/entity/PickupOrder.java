package com.kms.demo.entity;

import java.util.List;

public class PickupOrder {

    private String id;
    private String store;
    private List<OrderItem> items;

    public PickupOrder(Order order) {
        this.id = order.getId();
        this.items = order.getItems();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PickupOrder{" +
                "id='" + id + '\'' +
                ", store='" + store + '\'' +
                '}';
    }
}
