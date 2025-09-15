package io.github.loliiiico.landHealthRegenTimerTask;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import io.github.loliiiico.landHealthRegenTimerTask.lands.LandsHook;
import io.github.loliiiico.landHealthRegenTimerTask.task.AsyncHealthRegenTask;

public final class LandHealthRegenTimerTask extends JavaPlugin {
    private boolean landsPresent;
    private LandsHook landsHook;
    private BukkitTask regenTask;

    @Override
    public void onLoad() {
        // Register Lands flags at the correct lifecycle phase
        Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
        landsPresent = lands != null; // at onLoad, plugin might not be enabled yet
        if (!landsPresent) return;

        try {
            landsHook = new LandsHook(this);
            landsHook.registerHealthRegenTimerTaskFlag();
        } catch (Throwable inner) {
            getLogger().severe("Failed to register Lands flags onLoad: " + inner.getMessage());
        }
    }
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        boolean enabled = getConfig().getBoolean("regen-task.enabled", true);
        long delay = getConfig().getLong("regen-task.initial-delay-ticks", 20L * 10); // default 10s
        long period = getConfig().getLong("regen-task.period-ticks", 20L * 60);       // default 60s

        if (!enabled) {
            getLogger().info("Async regen task disabled via config.");
            return;
        }

        // Schedule the repeating task asynchronously; only the final heal runs sync
        regenTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            this,
            new AsyncHealthRegenTask(this, landsHook),
            delay,
            period
        );
        getLogger().info("Async regen task scheduled: delay=" + delay + " ticks, period=" + period + " ticks.");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
            getLogger().info("Async regen task cancelled.");
        }
    }
}
