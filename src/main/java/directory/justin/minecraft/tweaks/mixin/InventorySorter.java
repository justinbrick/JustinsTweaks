package directory.justin.minecraft.tweaks.mixin;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ScreenHandler.class)
public class InventorySorter {
    private final static HashMap<PlayerEntity, Long> PLAYER_CLICKS = new HashMap<>();
    private final static Comparator<ItemStack> STACK_COMPARATOR = Comparator.comparing(InventorySorter::getGroupValue)
            .thenComparing(InventorySorter::getRarityValue)
            .thenComparing(itemStack -> itemStack.getName().getString())
            .thenComparing(ItemStack::getDamage)
            .thenComparing(ItemStack::getCount);

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

    // Can't use a switch statement here, fun!
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

    private void sortInventory(ScreenHandler handler) {
        int size = handler.slots.size();
        int starting = 0;
        Inventory firstInventory = null;
        for (int i = 0; i < size; ++i) {
            Slot s = handler.slots.get(i);
            if (s.inventory.size() > 5) { // Bigger than brewing stand
                firstInventory = s.inventory;
                starting = i;
                size = firstInventory.size();
                break;
            }
        }

        if (firstInventory == null) return;
        if (size == 41) {
            size = 27;
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

    @Inject(method = "internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void slotClicked(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo callbackInfo) {
        ScreenHandler currentHandler = player.currentScreenHandler;
        if (slotIndex != -999 || button != 0) return;
        if (!PLAYER_CLICKS.containsKey(player)) {
            System.err.println("Could not find a player associated with the one currently clicking in inventory!");
            return;
        }
        long lastTime = PLAYER_CLICKS.get(player);
        long timeNow = System.currentTimeMillis();
        PLAYER_CLICKS.put(player, timeNow);
        if (timeNow - lastTime > 1000) return;
        PLAYER_CLICKS.put(player, timeNow - 1000);
        // Now we sort.
        sortInventory(currentHandler);
    }

    static{
        ServerPlayConnectionEvents.JOIN.register(InventorySorter::onPlayerJoined);
        ServerPlayConnectionEvents.DISCONNECT.register(InventorySorter::onPlayerLeft);
    }

    private static void onPlayerJoined(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        PlayerEntity p = handler.player;
        PLAYER_CLICKS.put(p, System.currentTimeMillis());
    }

    private static void onPlayerLeft(ServerPlayNetworkHandler handler, MinecraftServer server) {
        PlayerEntity p = handler.player;
        PLAYER_CLICKS.remove(p);
    }
}

