package edu.upc.dsa;

import edu.upc.dsa.domain.repository.DroneManager;
import edu.upc.dsa.domain.repository.DroneManagerImpl;
import edu.upc.dsa.domain.models.Drone;
import edu.upc.dsa.domain.models.FlightPlan;
import edu.upc.dsa.domain.models.Pilot;
import edu.upc.dsa.domain.models.Position;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DroneManagerTest {
    private DroneManager manager;
    private final LocalDateTime startTime = LocalDateTime.of(2030, 1, 1, 10, 0);

    @Before
    public void setUp() throws ReflectiveOperationException {
        manager = DroneManagerImpl.getInstance();
        clearManager();

        manager.addDrone("D1", "Dron uno", "DJI", "Mini");
        manager.addDrone("D2", "Dron dos", "Parrot", "Anafi");
        manager.addDrone("D3", "Dron tres", "DJI", "Mavic");
        manager.addPilot("P1", "Toni", "Oller");
        manager.addPilot("P2", "Juan", "Lopez");
    }

    @After
    public void tearDown() throws ReflectiveOperationException {
        clearManager();
        manager = null;
    }

    @Test
    public void addDroneTest() {
        Assert.assertSame(manager, DroneManagerImpl.getInstance());
        Assert.assertEquals(3, manager.getDronesByFlightHours().size());

        manager.addDrone("D4", "Dron cuatro", "Parrot", "Bebop");
        List<Drone> drones = manager.getDronesByFlightHours();
        Assert.assertEquals(4, drones.size());
        Drone added = drones.stream().filter(drone -> drone.getId().equals("D4"))
                .findFirst().get();
        Assert.assertEquals("Dron cuatro", added.getName());
        Assert.assertEquals("Parrot", added.getManufacturer());
        Assert.assertEquals("Bebop", added.getModel());
        Assert.assertEquals(0, added.getFlightHours(), 0.001);

        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.addDrone("D4", "Duplicado", "DJI", "Mini"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.addDrone(null, "Sin ID", "DJI", "Mini"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.addDrone("D5", "   ", "DJI", "Mini"));
        Assert.assertEquals(4, manager.getDronesByFlightHours().size());

        // Modificar la lista devuelta no debe borrar los drones del gestor.
        drones.clear();
        Assert.assertEquals(4, manager.getDronesByFlightHours().size());
    }

    @Test
    public void addPilotTest() throws ReflectiveOperationException {
        clearManager();
        Assert.assertTrue(manager.getPilotsByFlightHours().isEmpty());
        manager.addPilot("P1", "Toni", "Oller");
        Assert.assertEquals(1, manager.getPilotsByFlightHours().size());
        manager.addPilot("P2", "Juan", "Lopez");
        List<Pilot> pilots = manager.getPilotsByFlightHours();
        Assert.assertEquals(2, pilots.size());
        Pilot added = pilots.stream().filter(pilot -> pilot.getId().equals("P2"))
                .findFirst().get();
        Assert.assertEquals("Juan", added.getName());
        Assert.assertEquals("Lopez", added.getSurname());
        Assert.assertEquals(0, added.getFlightHours(), 0.001);

        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.addPilot("P1", "Toni", "Oller"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.addPilot("", "Juan", "Lopez"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.addPilot("P2", "Juan", null));
        Assert.assertEquals(2, manager.getPilotsByFlightHours().size());

        pilots.clear();
        Assert.assertEquals(2, manager.getPilotsByFlightHours().size());
    }

    @Test
    public void maintenanceTest() {
        Assert.assertThrows(IllegalStateException.class, () -> manager.repairDrone());
        Assert.assertThrows(IllegalArgumentException.class, () -> manager.storeDrone("NO_EXISTE"));

        manager.storeDrone("D1");
        manager.storeDrone("D2");
        Assert.assertThrows(IllegalStateException.class, () -> manager.storeDrone("D2"));
        Assert.assertThrows(IllegalStateException.class,
                () -> manager.createFlightPlan(plan("D2", "P1", startTime, 1)));
        Assert.assertTrue(manager.getFlightPlansByDrone("D2").isEmpty());

        // Sale primero D2: es el ultimo que se ha guardado y esta junto a la puerta.
        Assert.assertEquals("D2", manager.repairDrone().getId());
        manager.createFlightPlan(plan("D2", "P1", startTime, 1));
        Assert.assertEquals(1, manager.getFlightPlansByDrone("D2").size());

        // D1 sigue en mantenimiento hasta que tambien se retira del almacen.
        Assert.assertThrows(IllegalStateException.class,
                () -> manager.createFlightPlan(plan("D1", "P2", startTime, 1)));
        Assert.assertEquals("D1", manager.repairDrone().getId());
        manager.createFlightPlan(plan("D1", "P2", startTime, 1));
        Assert.assertEquals(1, manager.getFlightPlansByDrone("D1").size());
        Assert.assertThrows(IllegalStateException.class, () -> manager.repairDrone());
        Assert.assertEquals(3, manager.getDronesByFlightHours().size());
    }

    @Test
    public void createFlightPlanTest() {
        Assert.assertTrue(manager.getFlightPlansByDrone("D1").isEmpty());
        Assert.assertTrue(manager.getFlightPlansByPilot("P1").isEmpty());

        FlightPlan first = plan("D1", "P1", startTime, 1.5);
        manager.createFlightPlan(first);
        Assert.assertEquals(Collections.singletonList(first), manager.getFlightPlansByDrone("D1"));
        Assert.assertEquals(Collections.singletonList(first), manager.getFlightPlansByPilot("P1"));

        // P2 esta libre: el primer conflicto es exclusivamente del dron D1.
        Assert.assertThrows(IllegalStateException.class,
                () -> manager.createFlightPlan(plan("D1", "P2", startTime.plusHours(1), 1)));
        Assert.assertThrows(IllegalStateException.class,
                () -> manager.createFlightPlan(plan("D3", "P1", startTime.plusHours(1), 1)));
        Assert.assertThrows(IllegalStateException.class,
                () -> manager.createFlightPlan(plan("D1", "P2", startTime.minusHours(1), 4)));
        Assert.assertThrows(IllegalStateException.class,
                () -> manager.createFlightPlan(plan("D1", "P2", startTime.plusMinutes(30), 0.5)));
        Assert.assertThrows(IllegalStateException.class, () -> manager.createFlightPlan(first));

        Assert.assertThrows(IllegalArgumentException.class, () -> manager.createFlightPlan(null));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.createFlightPlan(plan("NO_EXISTE", "P1", startTime, 1)));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.createFlightPlan(plan("D1", "NO_EXISTE", startTime, 1)));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.createFlightPlan(plan("D3", "P2", null, 1)));
        for (double hours : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> manager.createFlightPlan(plan("D3", "P2", startTime, hours)));
        }
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.createFlightPlan(new FlightPlan("D3", "P2", startTime, 1,
                        Collections.emptyList())));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.createFlightPlan(new FlightPlan("D3", "P2", startTime, 1,
                        Collections.singletonList(null))));

        // Los intentos rechazados no deben insertar planes ni acumular horas.
        Assert.assertEquals(1, manager.getFlightPlansByDrone("D1").size());
        Assert.assertEquals(1, manager.getFlightPlansByPilot("P1").size());
        Assert.assertTrue(manager.getFlightPlansByDrone("D3").isEmpty());
        Assert.assertTrue(manager.getFlightPlansByPilot("P2").isEmpty());

        // Se permiten vuelos simultaneos con otro dron y otro piloto.
        manager.createFlightPlan(plan("D2", "P2", startTime, 3));

        // Se permiten reservas contiguas tanto antes como despues del primer plan.
        FlightPlan after = plan("D1", "P1", startTime.plusMinutes(90), 0.5);
        FlightPlan otherDrone = plan("D3", "P1", startTime.plusHours(2), 0.25);
        FlightPlan before = plan("D1", "P1", startTime.minusMinutes(30), 0.5);
        manager.createFlightPlan(after);
        manager.createFlightPlan(otherDrone);
        manager.createFlightPlan(before);
        Assert.assertEquals(Arrays.asList(first, after, before), manager.getFlightPlansByDrone("D1"));
        Assert.assertEquals(Arrays.asList(first, after, otherDrone, before), manager.getFlightPlansByPilot("P1"));

        List<Drone> drones = manager.getDronesByFlightHours();
        Assert.assertEquals("D2", drones.get(0).getId());
        Assert.assertEquals(3, drones.get(0).getFlightHours(), 0.001);
        Assert.assertEquals("D1", drones.get(1).getId());
        Assert.assertEquals(2.5, drones.get(1).getFlightHours(), 0.001);
        Assert.assertEquals("D3", drones.get(2).getId());
        Assert.assertEquals(0.25, drones.get(2).getFlightHours(), 0.001);

        List<Pilot> pilots = manager.getPilotsByFlightHours();
        Assert.assertEquals(2, pilots.size());
        Assert.assertEquals("P2", pilots.get(0).getId());
        Assert.assertEquals(3, pilots.get(0).getFlightHours(), 0.001);
        Assert.assertEquals("P1", pilots.get(1).getId());
        Assert.assertEquals(2.75, pilots.get(1).getFlightHours(), 0.001);

        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.getFlightPlansByDrone("NO_EXISTE"));
        Assert.assertThrows(IllegalArgumentException.class,
                () -> manager.getFlightPlansByPilot("NO_EXISTE"));
        manager.getFlightPlansByDrone("D1").clear();
        manager.getFlightPlansByPilot("P1").clear();
        Assert.assertEquals(3, manager.getFlightPlansByDrone("D1").size());
        Assert.assertEquals(4, manager.getFlightPlansByPilot("P1").size());
    }

    // La limpieza del Singleton queda dentro de los tests, sin exponer sus colecciones.
    private void clearManager() throws ReflectiveOperationException {
        for (String fieldName : new String[] {"drones", "pilots", "flightPlans", "warehouse"}) {
            Field field = DroneManagerImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            ((Collection<?>) field.get(manager)).clear();
        }
    }

    private FlightPlan plan(String droneId, String pilotId, LocalDateTime start, double hours) {
        return new FlightPlan(droneId, pilotId, start, hours,
                Arrays.asList(new Position(41.28, 1.97), new Position(41.29, 1.98)));
    }
}
