package eu.wohlben.qits.system.stories.support;

import eu.wohlben.qits.system.api.TerminalSocket;

/**
 * The names and addresses every story in this catalogue shares — spelled once, so a diagram and the
 * assertion that pins it cannot disagree about what a thing is called.
 *
 * <p><b>A name here is a stable literal, never a run stamp.</b> {@code
 * eu.wohlben.qits.userflows.Labels} rewrites only what it can tell was generated — a UUID, a long
 * hex run, a bare numeric path segment — so anything else in a label survives into the story's
 * {@code networkHash}. A fixture named after a timestamp would move that hash on every run, and the
 * only symptom is a hash that never settles.
 */
public final class StoryTarget {

  /**
   * How every diagram in this catalogue names the launched process, on both sides of an edge: it is
   * the {@code to} of everything a story sends here and the {@code from} of everything it spawns.
   *
   * <p>It is the DEPLOYED name, and it has to be, because {@code TokenValidationBootstrapIT} builds
   * the boot sweep's expected docker line out of it — {@code ps -aq --filter
   * label=qits.system.owner=} plus this — and that value comes from {@code
   * quarkus.application.name} through {@code qits.system.terminals.owner}. So this constant is not
   * free to be a pretty label: it tracks the application name, and it moved from {@code
   * qits-platform-system} to {@code qits-system} when the platform plane was deleted. The {@code
   * networkHash} moves once with it and then settles, which is the normal consequence of renaming a
   * node and not a failure.
   */
  public static final String SERVICE = "qits-system";

  /** The machine surface's root. Path-routed verbatim by the edge on every host. */
  public static final String API = "/system/api";

  /**
   * The landing read: the machine, its disk and its swarm in one call. The only guarded route with
   * no path parameter at all.
   */
  public static final String OVERVIEW = API + "/overview";

  /** The terminal lifecycle resource. The bytes travel on {@link TerminalSocket} instead. */
  public static final String TERMINALS = API + "/terminals";

  /**
   * The node every node-scoped read in this catalogue addresses. {@code local} is the alias for the
   * node this service runs on, and it is the only node it can answer about — any other id is a
   * deliberate {@code 409 NODE_REMOTE} rather than an empty list.
   */
  public static final String LOCAL_NODE = API + "/nodes/local";

  /**
   * The container the stand-in daemon knows about, by the name docker prints for it. A swarm task's
   * container name: not a UUID, not hex, not a number, so it survives scrubbing exactly as written
   * and reads in a diagram as the thing an operator actually clicked on.
   */
  public static final String CONTAINER = "dev-qits-ci.1.k4g0nn1ld7o272jl7fciatg6m";

  /** The swarm service the stand-in knows, as the swarm reads address it. */
  public static final String SWARM_SERVICE = "dev-qits-artifacts";

  /** The swarm config the stand-in knows — the one object whose DATA is decoded into the answer. */
  public static final String SWARM_CONFIG = "qits-edge-vhosts";

  /** This node, as {@code docker node ls} names it. */
  public static final String NODE_ID = "iwbwrdui2z0n62kqcjfo1erwh";

  /**
   * The scrubbed marker a generated id becomes in a label. Authored here rather than interpolated,
   * because a story that put a real session id in a label would move its own {@code networkHash} on
   * every run — and the id is generated per run by definition.
   */
  public static final String ID = "{id}";

  /** The same, for a 64-hex container id or an image digest. */
  public static final String DIGEST = "{digest}";

  /**
   * The one value in the fixtures that must never leave this service: a swarm service's and a
   * container's environment is answered as KEYS only, and this is the value behind one of those
   * keys. Every story that reads a container or a service pins that it reached no label, no note
   * and no transcript in the published bundle.
   */
  public static final String SCRUBBED_SECRET = "not-a-real-secret-scrubbed-for-the-fixture";

  /** …and the key it sits behind, which the answer DOES carry. */
  public static final String SECRET_ENV_KEY = "QITS_RESOURCE_DB_PASSWORD";

  private StoryTarget() {}

  /** The socket address of one terminal, as {@code TerminalView.socketPath} spells it. */
  public static String socketPath(String sessionId) {
    return TerminalSocket.PATH_PREFIX + sessionId;
  }

  /**
   * The label the WebSocket edge carries, template-shaped. The session id is generated per run, so
   * it is written as {@link #ID} here and never interpolated.
   */
  public static String socketLabel(String outcome) {
    return "WS " + TerminalSocket.PATH_PREFIX + ID + " -> " + outcome;
  }
}
