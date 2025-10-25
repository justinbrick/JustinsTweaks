package directory.justin.minecraft.tweaks;

import directory.justin.minecraft.tweaks.util.SortUtility;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TweaksMod extends JavaPlugin
{
    public static final Logger LOGGER = LoggerFactory.getLogger(TweaksMod.class);
    private static final SortUtility sortUtility = new SortUtility();
    @Override
    public void onEnable() {
        var version = getClass().getPackage().getImplementationVersion();
        LOGGER.info("Justin's Tweaks, Version {}", version == null ? "indev" : version);
        Bukkit.getPluginManager().registerEvents(sortUtility, this);
    }
}
