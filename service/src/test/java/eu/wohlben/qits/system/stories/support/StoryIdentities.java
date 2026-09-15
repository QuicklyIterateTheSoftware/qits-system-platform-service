package eu.wohlben.qits.system.stories.support;

import eu.wohlben.qits.servicemock.idp.MockIdp;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/**
 * The two kinds of caller this service has, and how a story hands each of them to a request.
 *
 * <h2>A person is a pair of headers</h2>
 *
 * <p>{@link #operator(RequestSpecification)} sends {@code X-Qits-User} and {@code X-Qits-Roles},
 * which is what the platform edge asserts for a logged-in operator and the whole of this service's
 * relationship with human authentication — it authenticates no person itself. That is not a
 * shortcut around the bearer: the OIDC tenant this catalogue turns on is <b>bearer-only</b>, so a
 * request carrying no {@code Authorization} header is never challenged by it and falls through to
 * qits-auth-core's header mechanism, exactly as it does behind the edge. Using it is what lets a
 * read story say "an operator" honestly rather than dressing a person up as a machine.
 *
 * <h2>A machine is a bearer this run's idp minted</h2>
 *
 * <p>{@link #machine(RequestSpecification)} presents a token signed by {@link MockIdp}'s generated
 * keypair, carrying the platform audience this service enforces and {@code qits:system} in {@code
 * groups}. Every token is minted <b>fresh per call and never cached</b>: a helper that handed the
 * same string to two stories would make {@code assertNotLeaked} a weaker claim than it reads as.
 *
 * <h2>Why both, in one catalogue</h2>
 *
 * <p>Because the difference between them is a rule this service actually has. Every REST route is
 * {@code @RolesAllowed({"qits:admin", "qits:system"})} — reading the host's shape is something a
 * machine may do — and the terminal socket is {@code @RolesAllowed("qits:admin")} alone, because
 * holding an interactive shell on the platform host is not. A catalogue that only ever spoke as one
 * of the two could not show where the ceiling is.
 */
public final class StoryIdentities {

  /** The header the edge names the logged-in person in. */
  public static final String USER_HEADER = "X-Qits-User";

  /** The header the edge asserts that person's roles in, comma-separated. */
  public static final String ROLES_HEADER = "X-Qits-Roles";

  /** The role a person needs for everything here, including a shell. */
  public static final String ADMIN_ROLE = "qits:admin";

  /** The role a platform peer holds: every read, and no terminal. */
  public static final String MACHINE_ROLE = "qits:system";

  /** How the diagram names a person at the console. */
  public static final String OPERATOR = "an operator";

  /** …and a platform peer holding a bearer. */
  public static final String PEER = "a platform service";

  /** The operator, by name — the value the edge would carry in {@code X-Qits-User}. */
  private static final String OPERATOR_NAME = "alice";

  private StoryIdentities() {}

  /** {@code given()} with the two headers the edge asserts for a logged-in operator. */
  public static RequestSpecification operator(RequestSpecification request) {
    return request.header(USER_HEADER, OPERATOR_NAME).header(ROLES_HEADER, ADMIN_ROLE);
  }

  /** The same person, with a role this service's routes have never heard of. */
  public static RequestSpecification withRole(RequestSpecification request, String role) {
    return request.header(USER_HEADER, OPERATOR_NAME).header(ROLES_HEADER, role);
  }

  /** The same two headers, on a WebSocket handshake. */
  public static Map<String, String> operatorHeaders() {
    return Map.of(USER_HEADER, OPERATOR_NAME, ROLES_HEADER, ADMIN_ROLE);
  }

  /** The handshake headers of a person holding some other role. */
  public static Map<String, String> headersWithRole(String role) {
    return Map.of(USER_HEADER, OPERATOR_NAME, ROLES_HEADER, role);
  }

  /** A freshly minted platform-peer bearer: the platform audience, {@code qits:system}. */
  public static String machineToken() {
    return MockIdp.attach()
        .token()
        .subject("a-platform-service")
        .audience(StoryProfile.AUDIENCE)
        .groups(MACHINE_ROLE)
        .mint();
  }

  /** {@code given()} carrying a bearer the story already holds — it has to pin it as not leaked. */
  public static RequestSpecification bearer(RequestSpecification request, String token) {
    return request.header("Authorization", "Bearer " + token);
  }
}
