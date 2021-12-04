package directory.justin.minecraft.tweaks;

import directory.justin.minecraft.tweaks.mixin.InventorySorter;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;

public class TweaksMod implements ModInitializer
{
    @Override
    public void onInitialize() {
        System.out.println("Justin's Tweaks, Version 1.0.0");
    }
}
