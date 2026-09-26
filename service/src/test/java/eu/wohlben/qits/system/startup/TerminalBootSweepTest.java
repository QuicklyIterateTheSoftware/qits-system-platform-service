package eu.wohlben.qits.system.startup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.wohlben.qits.system.testdocker.FakeDocker;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The boot sweep, driven by hand.
 *
 * <p>Its own {@code @Observes StartupEvent} already ran when this application started, which is
 * exactly why it is fired again here: at that moment there was nothing to reap and no test had
 * arranged one. Firing it a second time is safe — every step is idempotent and none is fatal —
 * and it is the only way to arrange the state the sweep exists for.
 *
 * <p>WHAT IT IS FOR. A glances container outlives the CLI that started it: {@code --rm} fires when
 * the CONTAINER exits, and a service that was killed rather than stopped (an OOM, a host reboot
 * mid-session) leaves one running with the host's pid namespace and a socket, and nothing pointing
 * at it. This sweep is the only thing that would ever clean it up.
 */
@QuarkusTest
class TerminalBootSweepTest {

  @Inject TerminalBootSweep sweep;

  private Path leftovers() {
    return FakeDocker.directory().resolve("leftovers");
  }

  @AfterEach
  void tidy() throws Exception {
    Files.deleteIfExists(leftovers());
    FakeDocker.clearCalls();
  }

  @Test
  void itRemovesWhatAPreviousRunLeftBehind() throws Exception {
    // What the stand-in's `ps -aq --filter …` will report as still running.
    Files.writeString(leftovers(), "abc123\ndef456\n", StandardCharsets.UTF_8);
    FakeDocker.clearCalls();

    sweep.onStart(new StartupEvent());

    // THE FILTER IS THE OWNER LABEL, and its value is this application's name — not a name prefix,
    // and not the bare label. Two platforms can share one docker daemon, and a sweep that reaped
    // every qits.system.owner container would kill the other one's live terminals on every restart.
    //
    // The literal is spelled out rather than read back from config on purpose: asserting
    // `quarkus.application.name` against itself would pass for any value, including an empty one,
    // and the point of the assertion is that the sweep filters on THE DEPLOYED NAME. That makes this
    // line move whenever the name does — it moved to qits-system when the platform plane was deleted
    // and qits-platform-system stopped being an address anything on the estate answers on — and the
    // move is the test doing its job, not friction to route around.
    assertTrue(
        FakeDocker.called("ps -aq --filter label=qits.system.owner=qits-system"),
        FakeDocker.calls().toString());
    assertTrue(FakeDocker.called("rm -f abc123 def456"), FakeDocker.calls().toString());
  }

  @Test
  void withNothingToReapItRemovesNothing() {
    FakeDocker.clearCalls();

    sweep.onStart(new StartupEvent());

    assertTrue(FakeDocker.called("version"), "the daemon is probed first: " + FakeDocker.calls());
    assertFalse(
        FakeDocker.called("rm -f"),
        "an empty list must not become a removal of nothing: " + FakeDocker.calls());
    // The suite turns the pre-pull off; the sweep must honour that rather than pull anyway.
    assertFalse(FakeDocker.called("pull"), FakeDocker.calls().toString());
  }
}
