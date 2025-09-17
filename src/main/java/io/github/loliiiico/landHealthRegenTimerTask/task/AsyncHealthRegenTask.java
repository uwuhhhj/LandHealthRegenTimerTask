package io.github.loliiiico.landHealthRegenTimerTask.task;

import io.github.loliiiico.landHealthRegenTimerTask.lands.LandsHook;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN;

/**
 * Asynchronous repeating task: scans players and decides regen off-thread.
 * Only the final event + health application runs on main thread.
 */
public class AsyncHealthRegenTask implements Runnable {
    private final JavaPlugin plugin;
    private final LandsHook landsHook;

    public AsyncHealthRegenTask(JavaPlugin plugin, LandsHook landsHook) {
        this.plugin = plugin;
        this.landsHook = landsHook;
    }

    @Override
    public void run() {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tryHandlePlayer(player);
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("AsyncHealthRegenTask error: " + t.getMessage());
        }
    }

    private void tryHandlePlayer(Player player) {
        try {
            if (player == null || !player.isOnline() || player.isDead()) return;
            if (player.getSaturation() >= 1F) {
                return;
            }

            // Read current values off-thread for computation
            final boolean preventSaturationLoss = plugin.getConfig().getBoolean("regen-task.prevent-saturation-loss", true);
            final double currentHP = player.getHealth();
            final AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) return;
            final double maxHP = maxHealthAttr.getValue();

            final double gainPlanned = 1.0D; // 0.5 heart
            final double futureHP = currentHP + gainPlanned;
            final double gained = futureHP > maxHP ? gainPlanned - (futureHP - maxHP) : gainPlanned;
            if (gained <= 0) return;

            // Lands checks: custom regen flag must allow; PvP flag blocks regen
            if (landsHook != null) {
                if (!landsHook.canPlayerRegen(player)) return;
                if (landsHook.isPvPEnabledAt(player)) return;
            }

            // Switch to main thread to fire event and apply
            Bukkit.getScheduler().runTask(plugin, () -> applyRegainSync(player, preventSaturationLoss, currentHP, maxHP, gained));
        } catch (Throwable ignored) {
        }
    }

    private void applyRegainSync(Player player, boolean preventSaturationLoss, double currentHealth, double maxHealth, double gained) {
        if (!player.isOnline() || player.isDead()) return;

        if (preventSaturationLoss && player.getSaturation() != 1F) {
            player.setSaturation(1F);
        }

        EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, gained, REGEN);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        double newHealth = Math.min(maxHealth, currentHealth + event.getAmount());
        player.setHealth(newHealth);
    }
}
