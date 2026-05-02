package directory.justin.minecraft.tweaks.util;

import java.util.HashMap;
import java.util.HashSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestUtility implements Listener {
  private static final PlayerDelayDebounce DEBOUNCE = new PlayerDelayDebounce(1000, 250);

  @EventHandler
  private static void onInventoryClick(InventoryClickEvent event) {
    if (!event.isShiftClick()
        || !event.isLeftClick()
        || event.getClickedInventory() != null
        || !(event.getWhoClicked() instanceof Player player)
        || DEBOUNCE.isDebounced(player)) return;

    insertMatching(event.getInventory(), player);
  }

  /// checks for valid chest types, ones that we are fine doing storage operations on.
  public static boolean isValidInventory(Inventory inventory) {
    return switch (inventory.getType()) {
      case CHEST, ENDER_CHEST, SHULKER_BOX, BARREL -> true;
      default -> false;
    };
  }

  /// TODO: sort into inventory that's open based off matching from player's inven.
  /// also, check for maybe permissions? random scope creep.
  private static void insertMatching(Inventory toInsert, Player matchingPlayer) {
    if (!isValidInventory(toInsert)) return;
    var matched = matchingPlayer.getInventory();
    var available = new HashMap<Material, HashSet<ItemStack>>();
    for (var stack : InventoryUtility.getInventoryNoHotbar(matched).toArray(ItemStack[]::new)) {
      if (stack == null) continue;
      var type = stack.getType();
      if (available.get(type) instanceof HashSet<ItemStack> set) {
        set.add(stack);
      } else {
        available.put(type, new HashSet<>() {{
          add(stack);
        }});
      }
    }


  }
}
