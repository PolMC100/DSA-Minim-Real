package edu.upc.dsa.domain.repository;

import edu.upc.dsa.domain.models.Drone;
import edu.upc.dsa.domain.models.FlightPlan;
import edu.upc.dsa.domain.models.Pilot;
import edu.upc.dsa.domain.models.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class DroneManagerImpl implements DroneManager {
    private static final Logger LOGGER = LogManager.getLogger(DroneManagerImpl.class);
    private static final DroneManagerImpl INSTANCE = new DroneManagerImpl();

    private final ArrayList<Drone> drones;
    private final ArrayList<Pilot> pilots;
    private final ArrayList<FlightPlan> flightPlans;
    private final Stack<Drone> warehouse;

    private DroneManagerImpl() {
        LOGGER.info("DroneManagerImpl inicio");
        drones = new ArrayList<>();
        pilots = new ArrayList<>();
        flightPlans = new ArrayList<>();
        warehouse = new Stack<>();
        LOGGER.info("DroneManagerImpl fin");
    }

    public static DroneManagerImpl getInstance() {
        LOGGER.info("getInstance inicio");
        LOGGER.info("getInstance fin");
        return INSTANCE;
    }

    @Override
    public void addDrone(String id, String name, String manufacturer, String model) {
        LOGGER.info("addDrone inicio: id={}, name={}, manufacturer={}, model={}",
                id, name, manufacturer, model);
        try {
            validateText(id, "id");
            validateText(name, "name");
            validateText(manufacturer, "manufacturer");
            validateText(model, "model");
            if (findDrone(id) != null) {
                LOGGER.error("Ya existe un dron con id={}", id);
                throw new IllegalArgumentException("El dron ya existe: " + id);
            }
            drones.add(new Drone(id, name, manufacturer, model));
        } finally {
            LOGGER.info("addDrone fin: id={}, name={}, manufacturer={}, model={}",
                    id, name, manufacturer, model);
        }
    }

    @Override
    public void addPilot(String id, String name, String surname) {
        LOGGER.info("addPilot inicio: id={}, name={}, surname={}", id, name, surname);
        try {
            validateText(id, "id");
            validateText(name, "name");
            validateText(surname, "surname");
            if (findPilot(id) != null) {
                LOGGER.error("Ya existe un piloto con id={}", id);
                throw new IllegalArgumentException("El piloto ya existe: " + id);
            }
            pilots.add(new Pilot(id, name, surname));
        } finally {
            LOGGER.info("addPilot fin: id={}, name={}, surname={}", id, name, surname);
        }
    }

    @Override
    public List<Drone> getDronesByFlightHours() {
        LOGGER.info("getDronesByFlightHours inicio");
        List<Drone> result = new ArrayList<>(drones);
        result.sort(Comparator.comparingDouble(Drone::getFlightHours).reversed());
        LOGGER.info("getDronesByFlightHours fin: {} drones", result.size());
        return result;
    }

    @Override
    public List<Pilot> getPilotsByFlightHours() {
        LOGGER.info("getPilotsByFlightHours inicio");
        List<Pilot> result = new ArrayList<>(pilots);
        result.sort(Comparator.comparingDouble(Pilot::getFlightHours).reversed());
        LOGGER.info("getPilotsByFlightHours fin: {} pilotos", result.size());
        return result;
    }

    @Override
    public void storeDrone(String droneId) {
        LOGGER.info("storeDrone inicio: droneId={}", droneId);
        try {
            Drone drone = findDrone(droneId);
            if (drone == null) {
                LOGGER.error("No existe el dron con id={}", droneId);
                throw new IllegalArgumentException("No existe el dron: " + droneId);
            }
            if (warehouse.contains(drone)) {
                LOGGER.error("El dron {} ya esta en el almacen", droneId);
                throw new IllegalStateException("El dron ya esta en el almacen: " + droneId);
            }
            warehouse.push(drone);
        } finally {
            LOGGER.info("storeDrone fin: droneId={}", droneId);
        }
    }

    @Override
    public Drone repairDrone() {
        LOGGER.info("repairDrone inicio");
        try {
            if (warehouse.empty()) {
                LOGGER.error("No se puede reparar: el almacen esta vacio");
                throw new IllegalStateException("El almacen esta vacio");
            }
            Drone drone = warehouse.pop();
            LOGGER.info("Dron reparado: id={}", drone.getId());
            return drone;
        } finally {
            LOGGER.info("repairDrone fin");
        }
    }

    @Override
    public void createFlightPlan(FlightPlan flightPlan) {
        LOGGER.info("createFlightPlan inicio: plan={}", flightPlan);
        try {
            if (flightPlan == null) {
                LOGGER.error("El plan de vuelo es obligatorio");
                throw new IllegalArgumentException("El plan de vuelo es obligatorio");
            }
            Drone drone = findDrone(flightPlan.getDroneId());
            Pilot pilot = findPilot(flightPlan.getPilotId());
            if (drone == null || pilot == null) {
                LOGGER.error("No existe el dron {} o el piloto {}",
                        flightPlan.getDroneId(), flightPlan.getPilotId());
                throw new IllegalArgumentException("El dron y el piloto deben existir");
            }
            LocalDateTime end = getEndTime(flightPlan);
            List<Position> positions = flightPlan.getPositions();
            if (positions.isEmpty() || positions.contains(null)) {
                LOGGER.error("El recorrido debe contener posiciones validas");
                throw new IllegalArgumentException("El recorrido debe contener posiciones validas");
            }
            for (Position position : positions) {
                if (position.getLatitude() == null || position.getLongitude() == null
                        || !Double.isFinite(position.getLatitude()) || !Double.isFinite(position.getLongitude())
                        || position.getLatitude() < -90 || position.getLatitude() > 90
                        || position.getLongitude() < -180 || position.getLongitude() > 180) {
                    LOGGER.error("Coordenadas no validas: {}", position);
                    throw new IllegalArgumentException("La latitud y longitud deben ser validas");
                }
            }
            if (warehouse.contains(drone)) { // Un dron en el almacen está pendiente de mantienimiento
                LOGGER.error("El dron {} esta en mantenimiento", drone.getId());
                throw new IllegalStateException("El dron esta en mantenimiento: " + drone.getId());
            }
            for (FlightPlan existing : flightPlans) {
                boolean sameDrone = existing.getDroneId().equals(drone.getId());
                boolean samePilot = existing.getPilotId().equals(pilot.getId());
                if ((sameDrone || samePilot)
                        && flightPlan.getStartTime().isBefore(getEndTime(existing))
                        && existing.getStartTime().isBefore(end)) {
                    LOGGER.error("Reserva solapada para el dron {} o el piloto {}",
                            drone.getId(), pilot.getId());
                    throw new IllegalStateException("El dron o el piloto ya tienen una reserva en ese intervalo");
                }
            }

            // Se contabiliza el tiempo estimado
            flightPlans.add(flightPlan);
            drone.setFlightHours(drone.getFlightHours() + flightPlan.getEstimatedHours());
            pilot.setFlightHours(pilot.getFlightHours() + flightPlan.getEstimatedHours());
        } finally {
            LOGGER.info("createFlightPlan fin: plan={}", flightPlan);
        }
    }

    @Override
    public List<FlightPlan> getFlightPlansByPilot(String pilotId) {
        LOGGER.info("getFlightPlansByPilot inicio: pilotId={}", pilotId);
        try {
            if (findPilot(pilotId) == null) {
                LOGGER.error("No existe el piloto con id={}", pilotId);
                throw new IllegalArgumentException("No existe el piloto: " + pilotId);
            }
            List<FlightPlan> result = new ArrayList<>();
            for (FlightPlan plan : flightPlans) {
                if (plan.getPilotId().equals(pilotId)) {
                    result.add(plan);
                }
            }
            return result;
        } finally {
            LOGGER.info("getFlightPlansByPilot fin: pilotId={}", pilotId);
        }
    }

    @Override
    public List<FlightPlan> getFlightPlansByDrone(String droneId) {
        LOGGER.info("getFlightPlansByDrone inicio: droneId={}", droneId);
        try {
            if (findDrone(droneId) == null) {
                LOGGER.error("No existe el dron con id={}", droneId);
                throw new IllegalArgumentException("No existe el dron: " + droneId);
            }
            List<FlightPlan> result = new ArrayList<>();
            for (FlightPlan plan : flightPlans) {
                if (plan.getDroneId().equals(droneId)) {
                    result.add(plan);
                }
            }
            return result;
        } finally {
            LOGGER.info("getFlightPlansByDrone fin: droneId={}", droneId);
        }
    }

    private Drone findDrone(String id) {
        LOGGER.info("findDrone inicio: id={}", id);
        try {
            validateText(id, "droneId");
            for (Drone drone : drones) {
                if (drone.getId().equals(id)) {
                    return drone;
                }
            }
            return null;
        } finally {
            LOGGER.info("findDrone fin: id={}", id);
        }
    }

    private Pilot findPilot(String id) {
        LOGGER.info("findPilot inicio: id={}", id);
        try {
            validateText(id, "pilotId");
            for (Pilot pilot : pilots) {
                if (pilot.getId().equals(id)) {
                    return pilot;
                }
            }
            return null;
        } finally {
            LOGGER.info("findPilot fin: id={}", id);
        }
    }

    private void validateText(String value, String field) {
        LOGGER.info("validateText inicio: value={}, field={}", value, field);
        try {
            if (value == null || value.trim().isEmpty()) {
                LOGGER.error("El campo {} es obligatorio", field);
                throw new IllegalArgumentException("El campo " + field + " es obligatorio");
            }
        } finally {
            LOGGER.info("validateText fin: value={}, field={}", value, field);
        }
    }

    private LocalDateTime getEndTime(FlightPlan plan) {
        LOGGER.info("getEndTime inicio: plan={}", plan);
        try {
            double hours = plan.getEstimatedHours();
            if (plan.getStartTime() == null || !Double.isFinite(hours) || hours <= 0) {
                throw new IllegalArgumentException("La fecha es obligatoria y la duracion debe ser positiva y finita");
            }
            // LocalDateTime trabaja con nanosegundos; conserva las fracciones de hora.
            long nanos = BigDecimal.valueOf(hours).multiply(new BigDecimal("3600000000000"))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (nanos == 0) {
                throw new IllegalArgumentException("La duracion debe ser de al menos un nanosegundo");
            }
            return plan.getStartTime().plusNanos(nanos);
        } catch (ArithmeticException | DateTimeException | IllegalArgumentException e) {
            LOGGER.error("Fecha o duracion de reserva no validas: {}", plan, e);
            throw new IllegalArgumentException("Fecha o duracion de reserva no validas", e);
        } finally {
            LOGGER.info("getEndTime fin: plan={}", plan);
        }
    }
}
