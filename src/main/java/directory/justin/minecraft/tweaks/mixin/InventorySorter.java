package directory.justin.minecraft.tweaks.mixin;

import directory.justin.minecraft.tweaks.TweaksMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static directory.justin.minecraft.tweaks.mixin.SortHandler.*;

@Mixin(ScreenHandler.class)
public class InventorySorter  {
    @Inject(method = "internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
    private void slotClicked(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo callbackInfo) {
        ScreenHandler currentHandler = player.currentScreenHandler;
        if (!PLAYER_SHOULD_SORT.get(player)) {
            TweaksMod.LOGGER.info("Did not sort because player was set to not sort in the database!");
            return;

        }
        if (slotIndex != -999 || button != 0 || !currentHandler.getCursorStack().isEmpty()) return;
        if (!PLAYER_CLICKS.containsKey(player)) {
            System.err.println("Could not find a player associated with the one currently clicking in inventory!");
            return;
        }
        long lastTime = PLAYER_CLICKS.get(player);
        long timeNow = System.currentTimeMillis();
        PLAYER_CLICKS.put(player, timeNow);
        if (timeNow - lastTime > 500) return;
        PLAYER_CLICKS.put(player, timeNow - 500);
        // Now we sort.
        sortInventory(currentHandler);
    }
}

