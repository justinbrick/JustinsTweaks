package directory.justin.minecraft.tweaks;

import directory.justin.minecraft.tweaks.mixin.InventorySorter;
import directory.justin.minecraft.tweaks.util.TweaksDatabase;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TweaksMod implements ModInitializer
{
    public static final Logger LOGGER = LoggerFactory.getLogger(TweaksMod.class);
    public static final TweaksDatabase CONFIG = new TweaksDatabase("jTweaksPlayerData.db");
    private static InventorySorter sorter;
    @Override
    public void onInitialize() {
        String version = getClass().getPackage().getImplementationVersion();
        LOGGER.info(String.format("Justin's Tweaks, Version %s", version == null ? "indev" : version));
        CONFIG.createPropertyInt("bShouldSort", 0);
        sorter = new InventorySorter();
    }
}
