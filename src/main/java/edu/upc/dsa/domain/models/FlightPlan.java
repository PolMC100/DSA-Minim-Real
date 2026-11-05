package edu.upc.dsa.domain.models;

import edu.upc.dsa.util.LocalDateTimeAdapter;
import io.swagger.annotations.ApiModelProperty;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlightPlan {
    private String droneId;
    private String pilotId; // Esto no lo dice el enunciado pero asumo que es necesario para poder asociar un piloto a un plan de vuelo
    private LocalDateTime startTime;
    private double estimatedHours;
    private ArrayList<Position> positions = new ArrayList<>();

    public FlightPlan() {
    }

    public FlightPlan(String droneId, String pilotId, LocalDateTime startTime,
                      double estimatedHours, List<Position> positions) {
        this.droneId = droneId;
        this.pilotId = pilotId;
        this.startTime = startTime;
        this.estimatedHours = estimatedHours;
        setPositions(positions);
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public String getPilotId() {
        return pilotId;
    }

    public void setPilotId(String pilotId) {
        this.pilotId = pilotId;
    }

    @XmlJavaTypeAdapter(LocalDateTimeAdapter.class)
    @ApiModelProperty(dataType = "string", example = "2030-01-01T10:00:00", required = true)
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public List<Position> getPositions() {
        return new ArrayList<>(positions);
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions == null ? new ArrayList<>() : new ArrayList<>(positions);
    }

    @Override
    public String toString() {
        return "FlightPlan{droneId='" + droneId + "', pilotId='" + pilotId
                + "', startTime=" + startTime + ", estimatedHours=" + estimatedHours
                + ", positions=" + positions + "}";
    }
}
