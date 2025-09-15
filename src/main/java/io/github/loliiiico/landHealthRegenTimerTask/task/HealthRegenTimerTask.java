package io.github.loliiiico.landHealthRegenTimerTask.task;

import io.github.loliiiico.landHealthRegenTimerTask.lands.LandsHook;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN;

public class HealthRegenTimerTask implements Runnable {
    private final JavaPlugin plugin;
    private final LandsHook landsHook;

    public HealthRegenTimerTask(JavaPlugin plugin, LandsHook landsHook) {
        this.plugin = plugin;
        this.landsHook = landsHook;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                if (!player.isOnline() || player.isDead()) continue;

                // Lands checks: custom regen flag must allow; PvP flag blocks regen
                if (landsHook != null) {
                    if (!landsHook.canPlayerRegen(player)) continue;
                    if (landsHook.isPvPEnabledAt(player)) continue;
                }

                evaluateHealth(player);
            } catch (Throwable t) {
                plugin.getLogger().warning("HealthRegenTimerTask player iteration error: " + t.getMessage());
            }
        }
    }

    private void evaluateHealth(Player player) {
        // Prevent saturation loss (optional via config)
        boolean preventSaturationLoss = plugin.getConfig().getBoolean("regen-task.prevent-saturation-loss", true);
        if (preventSaturationLoss && player.getSaturation() != 1F) {
            player.setSaturation(1F);
        }

        // Plan to regain 1.0 HP (0.5 heart)
        final double currentHP = player.getHealth();
        final double futureHP = currentHP + 1.0D;

        final AttributeInstance maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttr == null) return;
        final double maxHP = maxHealthAttr.getValue();

        final double gained = futureHP > maxHP ? 1.0D - (futureHP - maxHP) : 1.0D;
        if (gained <= 0) return;

        tryIncreaseHealth(player, currentHP, maxHP, gained);
    }

    private void tryIncreaseHealth(Player player, double currentHealth, double maxHealth, double gained) {
        // Fire event so other plugins can intervene
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, gained, REGEN);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        double newHealth = Math.min(maxHealth, currentHealth + event.getAmount());
        player.setHealth(newHealth);
    }
}
