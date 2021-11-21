package at.fhv.sysarch.lab2.homeautomation.domain;

import java.time.LocalDateTime;

public class Receipt {

    private String id;
    private LocalDateTime timeStamp;
    private Order order;

    public Receipt(String id, LocalDateTime timeStamp, Order order) {
        this.id = id;
        this.timeStamp = timeStamp;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "id='" + id + '\'' +
                ", timeStamp=" + timeStamp +
                ", order=" + order +
                '}';
    }
}
