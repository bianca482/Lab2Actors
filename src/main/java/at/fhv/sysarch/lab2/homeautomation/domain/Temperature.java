package at.fhv.sysarch.lab2.homeautomation.domain;

public class Temperature {
    private double value;
    private Unit unit;

    public enum Unit {
        CELSIUS
    }

    public Temperature(double value, Unit unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
}
