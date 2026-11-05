package models;

public class Drone {
    private final String id;
    private final String name;
    private final String manufacturer;
    private final String model;
    private double flightHours;

    public Drone(String id, String name, String manufacturer, String model) {
        this.id = id;
        this.name = name;
        this.manufacturer = manufacturer;
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public double getFlightHours() {
        return flightHours;
    }

    public void setFlightHours(double flightHours) {
        this.flightHours = flightHours;
    }
}
