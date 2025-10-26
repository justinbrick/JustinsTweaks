package directory.justin.minecraft.tweaks.util;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class SortUtility implements Listener {
    private static final int SORT_DELAY = 1000;
    private static final int SORT_THRESHOLD = 250;
    /// a mapping of the player UUID, and the last time a player had sorted.
    /// typically used to prevent over-sorting
    private static final HashMap<UUID, Instant> SORT_DEBOUNCE = new HashMap<>();
    /// a mapping of the player UUID, and the last time a sort request had been triggered.
    /// used to determine when to sort.
    private static final HashMap<UUID, Instant> LAST_SORT_EVENTS = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(SortUtility.class);
    private static final PlainTextComponentSerializer PLAIN_TEXT_COMPONENT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private final static Comparator<ItemStack> STACK_COMPARATOR = Comparator.comparing(SortUtility::getGroupValue)
            .thenComparing(stack -> Optional.ofNullable(stack)
                    .map(s -> s.getData(DataComponentTypes.ITEM_NAME))
                    .map(PLAIN_TEXT_COMPONENT_SERIALIZER::serialize)
                    .orElse("\uFFFF")
            )
            .thenComparing(stack -> Optional.ofNullable(stack)
                    .map(s -> s.getData(DataComponentTypes.DAMAGE))
                    .orElse(0)
            )
            .thenComparing(stack -> Optional.ofNullable(stack).map(ItemStack::getAmount).orElse(-1));


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
        LOGGER.info("Sorting inventory, {}", inventory);
        sortInventory(inventory, player);
    }

    private static void sortInventory(Inventory inventory, Player player) {
        if (inventory.getType() == InventoryType.CHEST) {
            sortInventory(inventory);
            return;
        }

        // Some items, we don't want to sort inside of them (think crafting tables, etc.) in this case, sort player's inv.
        sortInventory(player.getInventory());
    }

    private static void sortInventory(Inventory inventory) {
        var contents = inventory.getStorageContents();
        var toSort = Arrays.stream(contents);
        toSort = inventory instanceof PlayerInventory ? toSort.skip(9) : toSort;
        var sorted = toSort
                .sorted(STACK_COMPARATOR)
                .toArray(ItemStack[]::new);
        mergeStacks(sorted);
        sorted = inventory instanceof PlayerInventory
                ? Stream.concat(Arrays.stream(contents).limit(9), Arrays.stream(sorted)).toArray(ItemStack[]::new)
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
            if (Tag.BASE_STONE_OVERWORLD.isTagged(type)) return 200;
            if (Tag.CAMPFIRES.isTagged(type)) return 494;
            if (Tag.LANTERNS.isTagged(type)) return 495;
            if (Tag.STAIRS.isTagged(type)) return 496;
            if (Tag.SLABS.isTagged(type)) return 497;
            if (Tag.FENCES.isTagged(type)) return 498;
            if (Tag.FENCE_GATES.isTagged(type)) return 498;
            if (Tag.WALLS.isTagged(type)) return 499;
            if (Tag.WOOL_CARPETS.isTagged(type)) return 500;
            if (Tag.DOORS.isTagged(type)) return 501;
            if (Tag.BARS.isTagged(type)) return 502;
            return 250;
        }

        return Integer.MAX_VALUE;
    }
}