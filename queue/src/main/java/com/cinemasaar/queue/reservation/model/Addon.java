package com.cinemasaar.queue.reservation.model;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "addons")
public class Addon {

    @Id
    private String id;
    private String name;
    private BigDecimal price;
    private boolean active = true;

    public Addon() {
    }

    public Addon(String id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
