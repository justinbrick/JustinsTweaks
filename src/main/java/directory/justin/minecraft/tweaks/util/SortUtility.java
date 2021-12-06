package directory.justin.minecraft.tweaks.util;

import directory.justin.minecraft.tweaks.TweaksMod;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class SortUtility {
    public final static HashMap<PlayerEntity, Long> PLAYER_CLICKS = new HashMap<>();
    public final static HashMap<PlayerEntity, Boolean> PLAYER_SHOULD_SORT = new HashMap<>();
    private final static Comparator<ItemStack> STACK_COMPARATOR = Comparator.comparing(SortUtility::getGroupValue)
            .thenComparing(SortUtility::getRarityValue)
            .thenComparing(itemStack -> itemStack.getName().getString())
            .thenComparing(ItemStack::getDamage)
            .thenComparing(ItemStack::getCount);

    public SortUtility() {
        ServerPlayConnectionEvents.JOIN.register(SortUtility::onPlayerJoined);
        ServerPlayConnectionEvents.DISCONNECT.register(SortUtility::onPlayerLeft);
        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("toggle_sort").executes(context -> {
                var source = context.getSource();
                if (!(source.getEntity() instanceof ServerPlayerEntity p)) {
                    source.sendError(Text.of("You must be a player to use this command, you silly goober."));
                    return -1;
                }
                boolean shouldSort = !PLAYER_SHOULD_SORT.get(p);
                TweaksMod.CONFIG.setDataInt(p, "bShouldSort", shouldSort ? 1 : 0);
                PLAYER_SHOULD_SORT.put(p, shouldSort);
                source.sendFeedback(Text.of(String.format("%s sorting!", shouldSort ? "Enabled" : "Disabled")), false);
                return 1;
            }));
        }));
    }

    private static int getRarityValue(ItemStack s) {
        if (s.isEmpty()) return 0;
        Rarity r = s.getRarity();
        return switch (r) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
        };
    }

    private static int getGroupValue(ItemStack item) {
        ItemGroup i = item.getItem().getGroup();
        if (i == null) return 0;
        if (i.equals(ItemGroup.TOOLS)) return 6;
        else if (i.equals(ItemGroup.COMBAT)) return 5;
        else if (i.equals(ItemGroup.FOOD)) return 4;
        else if (i.equals(ItemGroup.BUILDING_BLOCKS)) return 3;
        else if (i.equals(ItemGroup.MATERIALS)) return 2;
        else return 1;
    }

    public static void sortInventory(ScreenHandler handler) {
        int size = handler.slots.size();
        int starting = 0;
        Inventory firstInventory = null;
        for (int i = 0; i < size; ++i) {
            Slot s = handler.slots.get(i);
            if (s.inventory.size() > 10) { // Bigger than crafting table
                firstInventory = s.inventory;
                starting = i;
                size = firstInventory.size();
                break;
            }
        }

        if (firstInventory == null) return;
        if (size == 41) {
            size = 27;
        }
        if (handler instanceof PlayerScreenHandler) {
            starting += 4;
        }
        List<ItemStack> toSort = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            if (!handler.getSlot(starting+i).getStack().isEmpty())
                toSort.add(firstInventory.removeStack(handler.getSlot(starting+i).getIndex()));
        }
        toSort.sort(STACK_COMPARATOR);
        int sortSize = toSort.size();
        for (int i = 0; i < size; ++i) {
            handler.getSlot(starting+i).insertStack(i < sortSize ? toSort.get(sortSize-i-1) : ItemStack.EMPTY);
        }
        handler.syncState();
        handler.updateToClient();
    }

    private static void onPlayerJoined(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        PlayerEntity p = handler.player;
        PLAYER_CLICKS.put(p, System.currentTimeMillis());
        boolean shouldSort = TweaksMod.CONFIG.getDataInt(handler.player, "bShouldSort") == 1;
        PLAYER_SHOULD_SORT.put(p, shouldSort);
    }

    private static void onPlayerLeft(ServerPlayNetworkHandler handler, MinecraftServer server) {
        PlayerEntity p = handler.player;
        PLAYER_CLICKS.remove(p);
        PLAYER_SHOULD_SORT.remove(p);
    }
}