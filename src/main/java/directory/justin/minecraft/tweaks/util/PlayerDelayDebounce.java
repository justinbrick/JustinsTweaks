package directory.justin.minecraft.tweaks.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.entity.Player;

/// a utility class which tracks a player "action" for repetition,
/// and does debouncing depending on the last "action".
/// note: i'm aware, this is an awful class name.
public class PlayerDelayDebounce {
  private final HashMap<UUID, Instant> debounces = new HashMap<>();
  private final HashMap<UUID, Instant> lastDelays = new HashMap<>();
  /// the delay before an "action" can be activated again.
  private final int debounceMs;
  /// the delay to consider before activating an "action"
  private final int thresholdMs;

  /// @param debounceMs the delay before an "action" can be activated again, in ms
  /// @param thresholdMs the threshold before activating an "action", in ms
  public PlayerDelayDebounce(int debounceMs, int thresholdMs) {
    this.thresholdMs = thresholdMs;
    this.debounceMs = debounceMs;
  }

  public boolean isDebounced(Player player) {
    var lastDebounced = debounces.get(player.getUniqueId());
    return lastDebounced != null
        && lastDebounced.until(Instant.now(), ChronoUnit.MILLIS) < debounceMs;
  }

  public boolean isInThreshold(Player player) {
    var lastDelay = lastDelays.get(player.getUniqueId());
    return lastDelay != null && lastDelay.until(Instant.now(), ChronoUnit.MILLIS) < thresholdMs;
  }

  public void updateThreshold(Player player) {
    lastDelays.put(player.getUniqueId(), Instant.now());
  }

  public void updateDebounce(Player player) {
    debounces.put(player.getUniqueId(), Instant.now());
  }

  public void removePlayer(Player player) {
    var uuid =  player.getUniqueId();
    debounces.remove(uuid);
    lastDelays.remove(uuid);
  }
}
