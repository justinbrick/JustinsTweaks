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
    private static HashMap<PlayerEntity, Long> playerClicks = new HashMap<PlayerEntity, Long>();
    private static Comparator<ItemStack> stackComparator = Comparator.comparing(InventorySorter::getGroupValue)
            .thenComparing(InventorySorter::getRarityValue)
            .thenComparing(itemStack -> itemStack.getName().getString())
            .thenComparing(ItemStack::getDamage)
            .thenComparing(ItemStack::getCount);

    private static int getRarityValue(ItemStack s) {
        if (s.isEmpty()) return 0;
        Rarity r = s.getRarity();
        switch (r) {
            case COMMON:
                return 1;
            case UNCOMMON:
                return 2;
            case RARE:
                return 3;
            case EPIC:
                return 4;
            default:
                return 0;
        }
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
        System.out.println(starting);
        System.out.println(size);
        List<ItemStack> toSort = new ArrayList<ItemStack>();
        for (int i = 0; i < size; ++i) {
            if (!handler.getSlot(starting+i).getStack().isEmpty())
                toSort.add(firstInventory.removeStack(handler.getSlot(starting+i).getIndex()));
        }
        toSort.sort(stackComparator);
        int sortSize = toSort.size();
        for (int i = 0; i < size; ++i) {
            if (i < sortSize) System.out.println(toSort.get(sortSize-i-1));
            handler.getSlot(starting+i).insertStack(i < sortSize ? toSort.get(sortSize-i-1) : ItemStack.EMPTY);
        }
        handler.syncState();
        handler.updateToClient();
    }

    @Inject(method = "internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void slotClicked(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo callbackInfo) {
        System.out.println(String.format("Clicked at slot index %d", slotIndex));
        ScreenHandler currentHandler = player.currentScreenHandler;
        if (slotIndex != -999 || button != 0) return;
        if (!playerClicks.containsKey(player)) {
            System.err.println("Could not find a player associated with the one currently clicking in inventory!");
            return;
        }
        long lastTime = playerClicks.get(player);
        long timeNow = System.currentTimeMillis();
        playerClicks.put(player, timeNow);
        if (timeNow - lastTime > 1000) return;
        playerClicks.put(player, timeNow - 1000);
        // Now we sort.
        sortInventory(currentHandler);
    }

    static{
        ServerPlayConnectionEvents.JOIN.register(InventorySorter::onPlayerJoined);
        ServerPlayConnectionEvents.DISCONNECT.register(InventorySorter::onPlayerLeft);
    }

    private static void onPlayerJoined(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        PlayerEntity p = handler.player;
        playerClicks.put(p, System.currentTimeMillis());
    }

    private static void onPlayerLeft(ServerPlayNetworkHandler handler, MinecraftServer server) {
        PlayerEntity p = handler.player;
        if (playerClicks.containsKey(p)) playerClicks.remove(p);
    }
}

