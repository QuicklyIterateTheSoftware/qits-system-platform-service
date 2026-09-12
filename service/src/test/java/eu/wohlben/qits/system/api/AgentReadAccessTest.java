package eu.wohlben.qits.system.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.wohlben.qits.system.testdocker.TerminalClient;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.net.http.WebSocketHandshakeException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@code qits:agent}, a commissioned agent's own role: it reads the host like {@code qits:system}
 * does, and may not open, end or attach to a terminal.
 *
 * <p>Each request names its identity in {@code X-Qits-User} / {@code X-Qits-Roles}, so the {@code
 * %test} dev user does not apply and the identity holds exactly the role sent.
 */
@QuarkusTest
class AgentReadAccessTest {

  private static final Map<String, String> AGENT =
      Map.of("X-Qits-User", "dyn-workspace-agent", "X-Qits-Roles", "qits:agent");

  @TestHTTPResource("/")
  URI base;

  private static RequestSpecification as(String role) {
    return given().header("X-Qits-User", "dyn-workspace-agent").header("X-Qits-Roles", role);
  }

  private static RequestSpecification agent() {
    return as("qits:agent");
  }

  @Test
  void anAgentReadsTheOverview() {
    agent().get("/system/api/overview").then().statusCode(200);
  }

  @Test
  void anAgentReadsTheSwarm() {
    for (String path :
        new String[] {
          "/system/api/swarm/nodes",
          "/system/api/swarm/services",
          "/system/api/swarm/configs",
          "/system/api/swarm/secrets"
        }) {
      agent().get(path).then().statusCode(not(anyOf(is(401), is(403))));
    }
  }

  @Test
  void anAgentReadsTheNode() {
    for (String path :
        new String[] {
          "/system/api/nodes/local/containers",
          "/system/api/nodes/local/images",
          "/system/api/nodes/local/volumes",
          "/system/api/nodes/local/networks"
        }) {
      agent().get(path).then().statusCode(not(anyOf(is(401), is(403))));
    }
  }

  @Test
  void anAgentListsTerminalsButOpensAndEndsNone() {
    agent().get("/system/api/terminals").then().statusCode(200);
    agent()
        .get("/system/api/terminals/" + UUID.randomUUID())
        .then()
        .statusCode(not(anyOf(is(401), is(403))));
    agent()
        .contentType("application/json")
        .body("{\"kind\":\"GLANCES\"}")
        .post("/system/api/terminals")
        .then()
        .statusCode(403);
    agent().delete("/system/api/terminals/" + UUID.randomUUID()).then().statusCode(403);
  }

  @Test
  void anAgentIsRefusedTheTerminalSocket() {
    URI uri = TerminalClient.socketUri(base, "/system/api/terminals/" + UUID.randomUUID());
    try {
      TerminalClient.connect(uri, AGENT);
      throw new AssertionError("an agent must not attach to a terminal");
    } catch (RuntimeException thrown) {
      Throwable cause = thrown.getCause() == null ? thrown : thrown.getCause();
      assertInstanceOf(WebSocketHandshakeException.class, cause, "expected a refused upgrade");
      assertEquals(403, ((WebSocketHandshakeException) cause).getResponse().statusCode());
    }
  }

  @Test
  void aRoleOutsideTheBoundaryIsStillRefused() {
    as("qits:reader").get("/system/api/overview").then().statusCode(403);
  }
}
