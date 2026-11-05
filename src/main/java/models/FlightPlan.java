package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlightPlan {
    private final String droneId;
    private final String pilotId; // Esto no lo dice el enunciado pero asumo que es necesario para poder asociar un piloto a un plan de vuelo
    private final LocalDateTime startTime;
    private final double estimatedHours;
    private final ArrayList<Position> positions;

    public FlightPlan(String droneId, String pilotId, LocalDateTime startTime,
                      double estimatedHours, List<Position> positions) {
        this.droneId = droneId;
        this.pilotId = pilotId;
        this.startTime = startTime;
        this.estimatedHours = estimatedHours;
        this.positions = new ArrayList<>(positions);
    }

    public String getDroneId() {
        return droneId;
    }

    public String getPilotId() {
        return pilotId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public double getEstimatedHours() {
        return estimatedHours;
    }

    public List<Position> getPositions() {
        return new ArrayList<>(positions);
    }

    @Override
    public String toString() {
        return "FlightPlan{droneId='" + droneId + "', pilotId='" + pilotId
                + "', startTime=" + startTime + ", estimatedHours=" + estimatedHours
                + ", positions=" + positions + "}";
    }
}
