package directory.justin.minecraft.tweaks.mixin;

import directory.justin.minecraft.tweaks.TweaksMod;
import directory.justin.minecraft.tweaks.util.SortUtility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class InventorySorter  {
    @Inject(method = "internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void slotClicked(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo callbackInfo) {

        ScreenHandler currentHandler = player.currentScreenHandler;
        if (!SortUtility.PLAYER_SHOULD_SORT.get(player)) {
            TweaksMod.LOGGER.info("Did not sort because player was set to not sort in the database!");
            return;

        }
        if (slotIndex != -999 || button != 0 || !currentHandler.getCursorStack().isEmpty()) return;
        if (!SortUtility.PLAYER_CLICKS.containsKey(player)) {
            System.err.println("Could not find a player associated with the one currently clicking in inventory!");
            return;
        }
        long lastTime = SortUtility.PLAYER_CLICKS.get(player);
        long timeNow = System.currentTimeMillis();
        SortUtility.PLAYER_CLICKS.put(player, timeNow);
        if (timeNow - lastTime > 500) return;
        SortUtility.PLAYER_CLICKS.put(player, timeNow - 500);
        // Now we sort.
        SortUtility.sortInventory(currentHandler);
    }
}

