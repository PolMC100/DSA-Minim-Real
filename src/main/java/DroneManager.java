import models.Drone;
import models.FlightPlan;
import models.Pilot;

import java.util.List;

/**
 * Operaciones de gestion de la flota de drones.
 * La implementacion almacenara drones, pilotos y planes en ArrayList,
 * y los drones en mantenimiento en un Stack (FILO).
 */
public interface DroneManager {

    /**
     * Registra un dron con cero horas de vuelo.
     * @throws IllegalArgumentException si el identificador ya existe
     */
    void addDrone(String id, String name, String manufacturer, String model);

    /**
     * Registra un piloto con cero horas de vuelo.
     * @throws IllegalArgumentException si el identificador ya existe
     */
    void addPilot(String id, String name, String surname);

    /** Devuelve los drones ordenados de mayor a menor numero de horas de vuelo. */
    List<Drone> getDronesByFlightHours();

    /** Devuelve los pilotos ordenados de mayor a menor numero de horas de vuelo. */
    List<Pilot> getPilotsByFlightHours();

    /**
     * Apila el dron en el almacen y deja de estar operativo.
     * @throws IllegalArgumentException si el dron no existe
     * @throws IllegalStateException si el dron ya esta en el almacen
     */
    void storeDrone(String droneId);

    /**
     * Repara y retira el ultimo dron almacenado, que vuelve a estar operativo.
     * @return el dron reparado
     * @throws IllegalStateException si el almacen esta vacio
     */
    Drone repairDrone();

    /**
     * Registra un plan con el ID del dron, el ID del piloto, la fecha y hora
     * de inicio, la duracion estimada en horas y las posiciones del recorrido.
     * El intervalo de reserva incluye el inicio y excluye el final, de modo
     * que un plan puede comenzar justo cuando termina otro.
     * @throws IllegalArgumentException si el dron o el piloto no existen,
     *         faltan datos obligatorios o la duracion no es positiva y finita
     * @throws IllegalStateException si el dron esta en mantenimiento o si
     *         el dron o el piloto tienen otro plan que se solapa con la reserva
     */
    void createFlightPlan(FlightPlan flightPlan);

    /**
     * Devuelve los planes asignados al piloto, o una lista vacia si no tiene.
     * @throws IllegalArgumentException si el piloto no existe
     */
    List<FlightPlan> getFlightPlansByPilot(String pilotId);

    /**
     * Devuelve los planes asignados al dron, o una lista vacia si no tiene.
     * @throws IllegalArgumentException si el dron no existe
     */
    List<FlightPlan> getFlightPlansByDrone(String droneId);
}
