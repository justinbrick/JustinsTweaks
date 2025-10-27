package directory.justin.minecraft.tweaks.util;

import static org.bukkit.persistence.PersistentDataType.BOOLEAN;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import directory.justin.minecraft.tweaks.TweaksMod;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class SortUtility implements Listener {

  // Command registration
  private static final NamespacedKey KEY_SHOULD_SORT = new NamespacedKey("jt", "should_sort");
  // Actual sorting logic
  private static final int SORT_DELAY = 1000;
  private static final int SORT_THRESHOLD = 250;
  /// a mapping of the player UUID, and the last time a player had sorted.
  /// typically used to prevent over-sorting
  private static final HashMap<UUID, Instant> SORT_DEBOUNCE = new HashMap<>();
  /// a mapping of the player UUID, and the last time a sort request had been triggered.
  /// used to determine when to sort.
  private static final HashMap<UUID, Instant> LAST_SORT_EVENTS = new HashMap<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(SortUtility.class);
  private static final PlainTextComponentSerializer PLAIN_TEXT_COMPONENT_SERIALIZER =
      PlainTextComponentSerializer.plainText();
  /// the base ranking of blocks, where ranking starts.
  private static final int BASE_BLOCK_RANKING = 250;
  /// the tiering of ranks for blocks
  private static final int BASE_BLOCK_INCREMENT = 10;
  /// a specified rank objects breakable by pickaxes
  private static final int RANKING_PICKAXE;
  /// a specified rank objects breakable by axes
  private static final int RANKING_AXE;
  /// a specified rank objects breakable by shovels
  private static final int RANKING_SHOVEL;
  /// If an item count as a block, this list sequentially determines which ranking it has.
  private static final List<Tag<Material>> BLOCK_TAG_RANKINGS =
      Arrays.asList(
          Tag.BASE_STONE_OVERWORLD,
          Tag.BASE_STONE_NETHER,
          Tag.LOGS,
          Tag.CROPS,
          Tag.DIRT,
          Tag.WOOL,
          Tag.BEDS,
          Tag.CAMPFIRES,
          Tag.LANTERNS,
          Tag.CLIMBABLE,
          Tag.STAIRS,
          Tag.SLABS,
          Tag.WOOL_CARPETS,
          Tag.FENCES,
          Tag.FENCE_GATES,
          Tag.ALL_SIGNS,
          Tag.WALLS,
          Tag.DOORS,
          Tag.BARS);

  private static final Comparator<ItemStack> STACK_COMPARATOR =
      Comparator.comparing(SortUtility::getGroupValue)
          .thenComparing(
              stack ->
                  Optional.ofNullable(stack)
                      .map(s -> s.getData(DataComponentTypes.ITEM_NAME))
                      .map(PLAIN_TEXT_COMPONENT_SERIALIZER::serialize)
                      .orElse("\uFFFF"))
          .thenComparing(
              stack ->
                  Optional.ofNullable(stack)
                      .map(s -> s.getData(DataComponentTypes.DAMAGE))
                      .orElse(0))
          .thenComparing(stack -> Optional.ofNullable(stack).map(ItemStack::getAmount).orElse(-1));

  static {
    TweaksMod.BASE_COMMAND.then(
        Commands.literal("sort")
            .then(Commands.literal("enable").executes(c -> disableSort(c, true)))
            .then(Commands.literal("disable").executes(c -> disableSort(c, false))));

    RANKING_PICKAXE =
        getBlockRanking(BLOCK_TAG_RANKINGS.indexOf(Tag.BASE_STONE_NETHER))
            + BASE_BLOCK_INCREMENT / 2;
    RANKING_AXE = getBlockRanking(BLOCK_TAG_RANKINGS.indexOf(Tag.LOGS)) + BASE_BLOCK_INCREMENT / 2;
    RANKING_SHOVEL =
        getBlockRanking(BLOCK_TAG_RANKINGS.indexOf(Tag.DIRT)) + BASE_BLOCK_INCREMENT / 2;
  }

  private static int disableSort(CommandContext<CommandSourceStack> context, boolean enabled) {
    var source = context.getSource();
    var sender = source.getSender();
    var executor = source.getExecutor();
    if (!(executor instanceof Player p)) {
      sender.sendPlainMessage("Only players can use this command.");
      return Command.SINGLE_SUCCESS;
    }

    var pdc = p.getPersistentDataContainer();
    pdc.set(KEY_SHOULD_SORT, BOOLEAN, enabled);
    p.sendPlainMessage(String.format("Sorting enabled: %b", enabled));

    return Command.SINGLE_SUCCESS;
  }

  private static boolean shouldSort(Player player) {
    var shouldSort = player.getPersistentDataContainer().get(KEY_SHOULD_SORT, BOOLEAN);
    return shouldSort == null || shouldSort;
  }

  @EventHandler
  private static void onPlayerJoined(PlayerJoinEvent event) {
    SORT_DEBOUNCE.put(event.getPlayer().getUniqueId(), Instant.now());
    LAST_SORT_EVENTS.put(event.getPlayer().getUniqueId(), Instant.now());
  }

  @EventHandler
  private static void onPlayerQuit(PlayerQuitEvent event) {
    SORT_DEBOUNCE.remove(event.getPlayer().getUniqueId());
    LAST_SORT_EVENTS.remove(event.getPlayer().getUniqueId());
  }

  @EventHandler
  private static void onInventoryClicked(InventoryClickEvent event) {
    var inventory = event.getInventory();
    var clickedInventory = event.getClickedInventory();
    // We want them to click off inventory to sort.
    if (clickedInventory != null) return;
    if (!(event.getWhoClicked() instanceof Player player)) return;
    // If player has disabled sort, we don't care.
    if (!shouldSort(player)) return;
    var uuid = player.getUniqueId();
    var now = Instant.now();
    var lastSort = SORT_DEBOUNCE.get(uuid);
    if (lastSort.until(now, ChronoUnit.MILLIS) < SORT_DELAY) return;
    var lastClick = LAST_SORT_EVENTS.get(uuid);
    LAST_SORT_EVENTS.put(uuid, now);
    if (lastClick.until(now, ChronoUnit.MILLIS) > SORT_THRESHOLD) {
      return;
    }

    SORT_DEBOUNCE.put(uuid, now);
    LOGGER.trace("Sorting inventory for {}, {}", player.getName(), inventory);
    sortInventory(inventory, player);
  }

  private static void sortInventory(@NotNull Inventory inventory, @NotNull Player player) {
    switch (inventory.getType()) {
      case CHEST:
      case BARREL:
      case SHULKER_BOX:
        sortInventory(inventory);
        break;
      default:
        sortInventory(player.getInventory());
        break;
    }
  }

  private static void sortInventory(@NotNull Inventory inventory) {
    var contents = inventory.getStorageContents();
    var toSort = Arrays.stream(contents);
    toSort = inventory instanceof PlayerInventory ? toSort.skip(9) : toSort;
    var sorted = toSort.sorted(STACK_COMPARATOR).toArray(ItemStack[]::new);
    mergeStacks(sorted);
    sorted =
        inventory instanceof PlayerInventory
            ? Stream.concat(Arrays.stream(contents).limit(9), Arrays.stream(sorted))
                .toArray(ItemStack[]::new)
            : sorted;
    inventory.setStorageContents(sorted);
  }

  private static void mergeStacks(ItemStack[] stacks) {
    ItemStack last = stacks[0];
    Queue<Integer> emptySlots = new LinkedList<>();

    for (int i = 1; i < stacks.length; i++) {
      var current = stacks[i];
      if (current == null) break;
      if (!current.isSimilar(last)) {
        last = current;
      }

      if (last != current) {
        var wantAdd = last.getMaxStackSize() - last.getAmount();
        var toAdd = Math.min(wantAdd, current.getAmount());
        last.add(toAdd);
        current.subtract(toAdd);

        if (current.getAmount() <= 0) {
          stacks[i] = null;
          emptySlots.add(i);
          continue;
        } else {
          last = current;
        }
      }

      if (!emptySlots.isEmpty()) {
        var toMove = emptySlots.remove();
        stacks[toMove] = current;
        stacks[i] = null;
        emptySlots.add(i);
      }
    }
  }

  private static int getBlockRanking(int index) {
    return BASE_BLOCK_RANKING + index * BASE_BLOCK_INCREMENT;
  }

  /// Given an item stack, returns a value representing the "sort order" of an item, depending on
  // what tags it has.
  private static int getGroupValue(ItemStack item) {
    if (item == null) return Integer.MAX_VALUE;
    var type = item.getType();

    ///  In order:
    /// Blocks, Decorational, Food, Tools, Weapons, Armor
    if (item.hasData(DataComponentTypes.EQUIPPABLE)) return 6000;
    if (item.hasData(DataComponentTypes.WEAPON)) return 5000;
    if (item.hasData(DataComponentTypes.TOOL)) return 4000;
    if (type.isEdible() || type == Material.CAKE) return 3000;
    if (type.isBlock()) {
      for (var i = 0; i < BLOCK_TAG_RANKINGS.size(); i++) {
        if (BLOCK_TAG_RANKINGS.get(i).isTagged(type))
          return BASE_BLOCK_RANKING + i * BASE_BLOCK_INCREMENT;
      }

      if (Tag.MINEABLE_PICKAXE.isTagged(type)) return RANKING_PICKAXE;
      if (Tag.MINEABLE_AXE.isTagged(type)) return RANKING_AXE;
      if (Tag.MINEABLE_SHOVEL.isTagged(type)) return RANKING_SHOVEL;
      // If none match, just place at the end.
      return BASE_BLOCK_RANKING + 1 + BLOCK_TAG_RANKINGS.size() * BASE_BLOCK_INCREMENT;
    }

    return Integer.MAX_VALUE;
  }
}
