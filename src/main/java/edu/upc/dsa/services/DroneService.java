package edu.upc.dsa.services;

import edu.upc.dsa.domain.models.Drone;
import edu.upc.dsa.domain.models.FlightPlan;
import edu.upc.dsa.domain.models.Pilot;
import edu.upc.dsa.domain.repository.DroneManager;
import edu.upc.dsa.domain.repository.DroneManagerImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.ws.rs.Consumes;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(value = "/", description = "Servicio de gestion de drones, pilotos y planes de vuelo")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DroneService {
    private final DroneManager manager = DroneManagerImpl.getInstance();

    @POST
    @Path("drones")
    @ApiOperation(value = "Crear un dron", response = Drone.class)
    @ApiResponses({@ApiResponse(code = 201, message = "Dron creado", response = Drone.class),
            @ApiResponse(code = 400, message = "Datos invalidos"),
            @ApiResponse(code = 409, message = "Identificador duplicado")})
    public Response addDrone(Drone drone) {
        synchronized (manager) {
            if (drone == null) {
                throw new IllegalArgumentException("El cuerpo del dron es obligatorio");
            }
            for (Drone existing : manager.getDronesByFlightHours()) {
                if (existing.getId().equals(drone.getId())) {
                    throw new ClientErrorException("El dron ya existe: " + drone.getId(), 409);
                }
            }
            manager.addDrone(drone.getId(), drone.getName(), drone.getManufacturer(), drone.getModel());
            return Response.status(201).entity(findDrone(drone.getId())).build();
        }
    }

    @GET
    @Path("drones")
    @ApiOperation(value = "Listar drones por horas de vuelo descendentes", response = Drone.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Listado de drones", response = Drone.class, responseContainer = "List")})
    public Response getDronesByFlightHours() {
        synchronized (manager) {
            GenericEntity<List<Drone>> entity = new GenericEntity<List<Drone>>(manager.getDronesByFlightHours()) {};
            return Response.ok(entity).build();
        }
    }

    @POST
    @Path("pilots")
    @ApiOperation(value = "Crear un piloto", response = Pilot.class)
    @ApiResponses({@ApiResponse(code = 201, message = "Piloto creado", response = Pilot.class),
            @ApiResponse(code = 400, message = "Datos invalidos"),
            @ApiResponse(code = 409, message = "Identificador duplicado")})
    public Response addPilot(Pilot pilot) {
        synchronized (manager) {
            if (pilot == null) {
                throw new IllegalArgumentException("El cuerpo del piloto es obligatorio");
            }
            for (Pilot existing : manager.getPilotsByFlightHours()) {
                if (existing.getId().equals(pilot.getId())) {
                    throw new ClientErrorException("El piloto ya existe: " + pilot.getId(), 409);
                }
            }
            manager.addPilot(pilot.getId(), pilot.getName(), pilot.getSurname());
            return Response.status(201).entity(findPilot(pilot.getId())).build();
        }
    }

    @GET
    @Path("pilots")
    @ApiOperation(value = "Listar pilotos por horas de vuelo descendentes", response = Pilot.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Listado de pilotos", response = Pilot.class, responseContainer = "List")})
    public Response getPilotsByFlightHours() {
        synchronized (manager) {
            GenericEntity<List<Pilot>> entity = new GenericEntity<List<Pilot>>(manager.getPilotsByFlightHours()) {};
            return Response.ok(entity).build();
        }
    }

    @POST
    @Path("drones/{id}/maintenance")
    @ApiOperation(value = "Guardar un dron en el almacen", response = Drone.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Dron almacenado", response = Drone.class),
            @ApiResponse(code = 404, message = "Dron inexistente"),
            @ApiResponse(code = 409, message = "El dron ya esta en el almacen")})
    public Response storeDrone(@PathParam("id") String droneId) {
        synchronized (manager) {
            Drone drone = findDrone(droneId);
            manager.storeDrone(droneId);
            return Response.ok(drone).build();
        }
    }

    @POST
    @Path("maintenance/repair")
    @ApiOperation(value = "Reparar y retirar el ultimo dron almacenado", response = Drone.class)
    @ApiResponses({@ApiResponse(code = 200, message = "Dron reparado", response = Drone.class),
            @ApiResponse(code = 409, message = "Almacen vacio")})
    public Response repairDrone() {
        synchronized (manager) {
            return Response.ok(manager.repairDrone()).build();
        }
    }

    @POST
    @Path("flight-plans")
    @ApiOperation(value = "Crear un plan de vuelo", response = FlightPlan.class)
    @ApiResponses({@ApiResponse(code = 201, message = "Plan creado", response = FlightPlan.class),
            @ApiResponse(code = 404, message = "Dron o piloto inexistente"),
            @ApiResponse(code = 400, message = "Datos invalidos"),
            @ApiResponse(code = 409, message = "Mantenimiento o reserva solapada")})
    public Response createFlightPlan(FlightPlan plan) {
        synchronized (manager) {
            if (plan == null) {
                throw new IllegalArgumentException("El cuerpo del plan de vuelo es obligatorio");
            }
            findDrone(plan.getDroneId());
            findPilot(plan.getPilotId());
            manager.createFlightPlan(plan);
            return Response.status(201).entity(plan).build();
        }
    }

    @GET
    @Path("drones/{id}/flight-plans")
    @ApiOperation(value = "Consultar planes de un dron", response = FlightPlan.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Planes del dron", response = FlightPlan.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Dron inexistente")})
    public Response getFlightPlansByDrone(@PathParam("id") String droneId) {
        synchronized (manager) {
            findDrone(droneId);
            GenericEntity<List<FlightPlan>> entity = new GenericEntity<List<FlightPlan>>(manager.getFlightPlansByDrone(droneId)) {};
            return Response.ok(entity).build();
        }
    }

    @GET
    @Path("pilots/{id}/flight-plans")
    @ApiOperation(value = "Consultar planes de un piloto", response = FlightPlan.class, responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "Planes del piloto", response = FlightPlan.class, responseContainer = "List"),
            @ApiResponse(code = 404, message = "Piloto inexistente")})
    public Response getFlightPlansByPilot(@PathParam("id") String pilotId) {
        synchronized (manager) {
            findPilot(pilotId);
            GenericEntity<List<FlightPlan>> entity = new GenericEntity<List<FlightPlan>>(manager.getFlightPlansByPilot(pilotId)) {};
            return Response.ok(entity).build();
        }
    }

    private Drone findDrone(String id) {
        for (Drone drone : manager.getDronesByFlightHours()) {
            if (drone.getId().equals(id)) {
                return drone;
            }
        }
        throw new NotFoundException("No existe el dron: " + id);
    }

    private Pilot findPilot(String id) {
        for (Pilot pilot : manager.getPilotsByFlightHours()) {
            if (pilot.getId().equals(id)) {
                return pilot;
            }
        }
        throw new NotFoundException("No existe el piloto: " + id);
    }
}
