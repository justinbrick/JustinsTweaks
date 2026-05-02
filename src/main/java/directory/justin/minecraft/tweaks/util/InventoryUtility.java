package directory.justin.minecraft.tweaks.util;

import java.util.Arrays;
import java.util.stream.Stream;
import jdk.jshell.spi.ExecutionControlProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtility {

  public static Stream<ItemStack> getInventoryNoHotbar(PlayerInventory inventory) {
    var contents = inventory.getStorageContents();
    return Arrays.stream(contents).skip(9);
  }

  /// update the player's inventory, by providing storage that is outside of
  public static void addToInventoryNoHotbar(PlayerInventory inventory, Stream<ItemStack> contents) throws Exception {
    var storageContents = Stream.concat(Arrays.stream(inventory.getStorageContents()).limit(9), contents)
        .toArray(ItemStack[]::new);
    if (storageContents.length != inventory.getStorageContents().length) {
      throw new Exception("The content is too small to match the player's inventory!");
    }
    inventory.setStorageContents(storageContents);
  }
}
