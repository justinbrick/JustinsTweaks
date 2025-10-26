package directory.justin.minecraft.tweaks;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import directory.justin.minecraft.tweaks.util.SortUtility;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TweaksMod extends JavaPlugin
{
    public static final Logger LOGGER = LoggerFactory.getLogger(TweaksMod.class);
    public static final LiteralArgumentBuilder<CommandSourceStack> BASE_COMMAND = Commands.literal("jt");
    private static final SortUtility sortUtility = new SortUtility();
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(sortUtility, this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(BASE_COMMAND.build());
        });
    }
}
