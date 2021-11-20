package at.fhv.sysarch.lab2.homeautomation.domain;

import java.util.Objects;

public class Order {
    private Product product;
    private int amount;

    public Order(Product product, int amount) {
        this.product = product;
        this.amount = amount;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Order{" +
                "product=" + product +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return amount == order.amount && Objects.equals(product, order.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, amount);
    }
}
