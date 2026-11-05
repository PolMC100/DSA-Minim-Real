package models;

public class Pilot {
    private final String id;
    private final String name;
    private final String surname;
    private double flightHours; // No lo dice el enunciado pero hace falta para ordenar por joras

    public Pilot(String id, String name, String surname) {
        this.id = id;
        this.name = name;
        this.surname = surname;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public double getFlightHours() {
        return flightHours;
    }

    public void setFlightHours(double flightHours) {
        this.flightHours = flightHours;
    }
}
