package edu.upc.dsa;

import edu.upc.dsa.domain.repository.DroneManagerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DroneServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static HttpServer server;
    private static String baseUri;

    @BeforeClass
    public static void startServer() throws Exception {
        server = Main.startServer(URI.create("http://127.0.0.1:0/dsaApp/"));
        int port = server.getListeners().iterator().next().getPort();
        baseUri = "http://127.0.0.1:" + port + "/dsaApp/";
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Before
    public void setUp() throws Exception {
        clearManager();
    }

    @After
    public void tearDown() throws Exception {
        clearManager();
    }

    @Test
    public void swaggerDocumentationTest() throws Exception {
        HttpResult specification = request("GET", "swagger.json", null);
        Assert.assertEquals(200, specification.status);
        Assert.assertEquals("2.0", specification.body.get("swagger").asText());
        JsonNode paths = specification.body.get("paths");
        Assert.assertTrue(paths.has("/drones"));
        Assert.assertTrue(paths.has("/pilots"));
        Assert.assertTrue(paths.has("/flight-plans"));
        Assert.assertTrue(paths.get("/drones").get("get").get("responses").has("200"));
        Assert.assertEquals("string", specification.body.get("definitions").get("FlightPlan")
                .get("properties").get("startTime").get("type").asText());
    }

    @Test
    public void createDronesAndPilotsTest() throws Exception {
        Assert.assertEquals(0, request("GET", "drones", null).body.size());
        Assert.assertEquals(0, request("GET", "pilots", null).body.size());
        HttpResult drone = request("POST", "drones",
                "{\"id\":\"D1\",\"name\":\"Dron uno\",\"manufacturer\":\"DJI\",\"model\":\"Mini\",\"flightHours\":99}");
        Assert.assertEquals(201, drone.status);
        Assert.assertEquals("D1", drone.body.get("id").asText());
        Assert.assertEquals("DJI", drone.body.get("manufacturer").asText());
        Assert.assertEquals(0, drone.body.get("flightHours").asDouble(), 0.001);

        HttpResult toni = request("POST", "pilots", pilot("P1", "Toni", "Oller"));
        HttpResult juan = request("POST", "pilots", pilot("P2", "Juan", "Lopez"));
        Assert.assertEquals(201, toni.status);
        Assert.assertEquals("Toni", toni.body.get("name").asText());
        Assert.assertEquals("Oller", toni.body.get("surname").asText());
        Assert.assertEquals(201, juan.status);
        Assert.assertEquals("Juan", juan.body.get("name").asText());
        Assert.assertEquals("Lopez", juan.body.get("surname").asText());
        Assert.assertEquals(2, request("GET", "pilots", null).body.size());
        Assert.assertEquals(409, request("POST", "pilots", pilot("P1", "Toni", "Oller")).status);
        Assert.assertEquals(409, request("POST", "drones", drone("D1")).status);
        Assert.assertEquals(400, request("POST", "pilots", "{\"id\":\"\",\"name\":\"Juan\",\"surname\":\"Lopez\"}").status);
    }

    @Test
    public void flightPlansAndRankingTest() throws Exception {
        seed();
        String plan = flightPlan("D1", "P1", "2030-01-01T10:00:00", 1.5);
        HttpResult created = request("POST", "flight-plans", plan);
        Assert.assertEquals(201, created.status);
        Assert.assertEquals("D1", created.body.get("droneId").asText());
        Assert.assertEquals("P1", created.body.get("pilotId").asText());
        Assert.assertEquals("2030-01-01T10:00", created.body.get("startTime").asText());
        Assert.assertEquals(2, created.body.get("positions").size());
        Assert.assertEquals(41.28, created.body.get("positions").get(0).get("latitude").asDouble(), 0.001);
        Assert.assertEquals(201, request("POST", "flight-plans",
                flightPlan("D2", "P2", "2030-01-01T10:00:00", 3)).status);

        HttpResult drones = request("GET", "drones", null);
        Assert.assertEquals(200, drones.status);
        Assert.assertEquals("D2", drones.body.get(0).get("id").asText());
        Assert.assertEquals(3, drones.body.get(0).get("flightHours").asDouble(), 0.001);
        Assert.assertEquals("D1", drones.body.get(1).get("id").asText());
        HttpResult pilots = request("GET", "pilots", null);
        Assert.assertEquals(200, pilots.status);
        Assert.assertEquals(2, pilots.body.size());
        Assert.assertEquals("P2", pilots.body.get(0).get("id").asText());
        Assert.assertEquals("P1", pilots.body.get(1).get("id").asText());

        HttpResult byDrone = request("GET", "drones/D1/flight-plans", null);
        HttpResult byPilot = request("GET", "pilots/P1/flight-plans", null);
        Assert.assertEquals(200, byDrone.status);
        Assert.assertEquals(200, byPilot.status);
        Assert.assertEquals(1, byDrone.body.size());
        Assert.assertEquals(byDrone.body, byPilot.body);
        Assert.assertEquals(0, request("GET", "drones/D3/flight-plans", null).body.size());
    }

    @Test
    public void reservationConflictsTest() throws Exception {
        seed();
        String first = flightPlan("D1", "P1", "2030-01-01T10:00:00", 1.5);
        Assert.assertEquals(201, request("POST", "flight-plans", first).status);
        Assert.assertEquals(409, request("POST", "flight-plans",
                flightPlan("D1", "P2", "2030-01-01T11:00:00", 1)).status);
        Assert.assertEquals(409, request("POST", "flight-plans",
                flightPlan("D3", "P1", "2030-01-01T11:00:00", 1)).status);
        Assert.assertEquals(409, request("POST", "flight-plans", first).status);
        Assert.assertEquals(201, request("POST", "flight-plans",
                flightPlan("D1", "P1", "2030-01-01T11:30:00", 0.5)).status);
        Assert.assertEquals(2, request("GET", "drones/D1/flight-plans", null).body.size());
        Assert.assertEquals(2, request("GET", "pilots/P1/flight-plans", null).body.size());
        Assert.assertEquals(2, request("GET", "drones", null).body.get(0).get("flightHours").asDouble(), 0.001);
        Assert.assertEquals(2, request("GET", "pilots", null).body.get(0).get("flightHours").asDouble(), 0.001);
    }

    @Test
    public void maintenanceTest() throws Exception {
        seed();
        Assert.assertEquals(200, request("POST", "drones/D1/maintenance", null).status);
        Assert.assertEquals(200, request("POST", "drones/D2/maintenance", null).status);
        Assert.assertEquals(409, request("POST", "drones/D2/maintenance", null).status);
        Assert.assertEquals(409, request("POST", "flight-plans",
                flightPlan("D1", "P1", "2030-01-01T10:00:00", 1)).status);
        HttpResult repaired = request("POST", "maintenance/repair", null);
        Assert.assertEquals(200, repaired.status);
        Assert.assertEquals("D2", repaired.body.get("id").asText());
        Assert.assertEquals(201, request("POST", "flight-plans",
                flightPlan("D2", "P1", "2030-01-01T10:00:00", 1)).status);
        Assert.assertEquals(409, request("POST", "flight-plans",
                flightPlan("D1", "P2", "2030-01-01T10:00:00", 1)).status);
        Assert.assertEquals("D1", request("POST", "maintenance/repair", null).body.get("id").asText());
        Assert.assertEquals(201, request("POST", "flight-plans",
                flightPlan("D1", "P2", "2030-01-01T10:00:00", 1)).status);
        Assert.assertEquals(409, request("POST", "maintenance/repair", null).status);
        Assert.assertEquals(404, request("POST", "drones/NO_EXISTE/maintenance", null).status);
    }

    @Test
    public void invalidRequestsTest() throws Exception {
        seed();
        HttpResult invalidJson = request("POST", "drones", "{");
        Assert.assertEquals(400, invalidJson.status);
        Assert.assertFalse(invalidJson.body.asText().isEmpty());
        Assert.assertEquals(400, request("POST", "drones", "{\"id\":{}}").status);
        Assert.assertEquals(400, request("POST", "drones", null).status);
        Assert.assertEquals(400, request("POST", "flight-plans",
                flightPlan("D1", "P1", "fecha-incorrecta", 1)).status);
        Assert.assertEquals(400, request("POST", "flight-plans",
                flightPlan("D1", "P1", "2030-01-01T10:00:00", 0)).status);
        String valid = flightPlan("D1", "P1", "2030-01-01T10:00:00", 1);
        Assert.assertEquals(400, request("POST", "flight-plans", valid.replace("\"estimatedHours\":1.0,", "")).status);
        Assert.assertEquals(400, request("POST", "flight-plans", valid.replace("41.28", "91")).status);
        Assert.assertEquals(400, request("POST", "flight-plans", valid.replace("\"latitude\":41.28,", "")).status);
        Assert.assertEquals(404, request("POST", "flight-plans",
                flightPlan("NO_EXISTE", "P1", "2030-01-01T10:00:00", 1)).status);
        Assert.assertEquals(404, request("POST", "flight-plans",
                flightPlan("D1", "NO_EXISTE", "2030-01-01T10:00:00", 1)).status);
        Assert.assertEquals(404, request("GET", "drones/NO_EXISTE/flight-plans", null).status);
        Assert.assertEquals(404, request("GET", "pilots/NO_EXISTE/flight-plans", null).status);
        Assert.assertEquals(404, request("GET", "ruta-inexistente", null).status);
        Assert.assertEquals(405, request("PUT", "drones", null).status);
        Assert.assertEquals(0, request("GET", "drones/D1/flight-plans", null).body.size());
    }

    @Test
    public void concurrentReservationsTest() throws Exception {
        seed();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Future<HttpResult> toni = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return request("POST", "flight-plans", flightPlan("D1", "P1", "2030-01-01T10:00:00", 1));
            });
            Future<HttpResult> juan = executor.submit(() -> {
                barrier.await(5, TimeUnit.SECONDS);
                return request("POST", "flight-plans", flightPlan("D1", "P2", "2030-01-01T10:00:00", 1));
            });
            int firstStatus = toni.get(10, TimeUnit.SECONDS).status;
            int secondStatus = juan.get(10, TimeUnit.SECONDS).status;
            Assert.assertTrue((firstStatus == 201 && secondStatus == 409)
                    || (firstStatus == 409 && secondStatus == 201));
            Assert.assertEquals(1, request("GET", "drones/D1/flight-plans", null).body.size());
            Assert.assertEquals(1, request("GET", "drones", null).body.get(0).get("flightHours").asDouble(), 0.001);
        } finally {
            executor.shutdownNow();
        }
    }

    private void seed() throws Exception {
        for (String id : new String[] {"D1", "D2", "D3"}) {
            Assert.assertEquals(201, request("POST", "drones", drone(id)).status);
        }
        Assert.assertEquals(201, request("POST", "pilots", pilot("P1", "Toni", "Oller")).status);
        Assert.assertEquals(201, request("POST", "pilots", pilot("P2", "Juan", "Lopez")).status);
    }

    private String drone(String id) {
        return "{\"id\":\"" + id + "\",\"name\":\"Dron\",\"manufacturer\":\"DJI\",\"model\":\"Mini\"}";
    }

    private String pilot(String id, String name, String surname) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"surname\":\"" + surname + "\"}";
    }

    private String flightPlan(String droneId, String pilotId, String start, double hours) {
        return "{\"droneId\":\"" + droneId + "\",\"pilotId\":\"" + pilotId
                + "\",\"startTime\":\"" + start + "\",\"estimatedHours\":" + hours
                + ",\"positions\":[{\"latitude\":41.28,\"longitude\":1.97},{\"latitude\":41.29,\"longitude\":1.98}]}";
    }

    private HttpResult request(String method, String path, String json) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(new URL(baseUri), path).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        try {
            if (json != null) {
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            try (InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while (input != null && (count = input.read(buffer)) != -1) {
                    bytes.write(buffer, 0, count);
                }
                String content = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
                String type = connection.getContentType();
                JsonNode body = type != null && type.startsWith("application/json") && !content.isEmpty()
                        ? JSON.readTree(content) : JSON.getNodeFactory().textNode(content);
                return new HttpResult(status, body);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void clearManager() throws ReflectiveOperationException {
        for (String name : new String[] {"drones", "pilots", "flightPlans", "warehouse"}) {
            Field field = DroneManagerImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            ((Collection<?>) field.get(DroneManagerImpl.getInstance())).clear();
        }
    }

    private static class HttpResult {
        private final int status;
        private final JsonNode body;

        private HttpResult(int status, JsonNode body) {
            this.status = status;
            this.body = body;
        }
    }
}
