package com.cinemasaar.queue.reservation.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CartItem {

    private String seatId;
    private String rowLabel;
    private int seatNumber;
    private BigDecimal seatPrice;
    private List<AddonLine> addons = new ArrayList<>();

    public CartItem() {
    }

    public CartItem(String seatId, String rowLabel, int seatNumber, BigDecimal seatPrice) {
        this.seatId = seatId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.seatPrice = seatPrice;
    }

    public static CartItem from(ShowtimeSeat seat) {
        return new CartItem(seat.getSeatId(), seat.getRowLabel(),
                seat.getSeatNumber(), seat.getPrice());
    }

    public BigDecimal addonTotal() {
        return addons.stream()
                .map(AddonLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public BigDecimal getSeatPrice() {
        return seatPrice;
    }

    public void setSeatPrice(BigDecimal seatPrice) {
        this.seatPrice = seatPrice;
    }

    public List<AddonLine> getAddons() {
        return addons;
    }

    public void setAddons(List<AddonLine> addons) {
        this.addons = addons == null ? new ArrayList<>() : addons;
    }
}
