package io.github.loliiiico.landHealthRegenTimerTask.lands;

import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.LandWorld;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class LandsHook {
    private final JavaPlugin plugin;

    private LandsIntegration landsApi;
    private RoleFlag regenRoleFlag;
    private RoleFlag attackPlayerFlag;

    public LandsHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Must be called during plugin onLoad, after Lands loaded but before it enables.
     */
    public void registerHealthRegenTimerTaskFlag() {
        try {
            this.landsApi = new LandsIntegration(plugin);
            this.regenRoleFlag = RoleFlag.of(landsApi, FlagTarget.PLAYER, RoleFlagCategory.ACTION, "health_regen_timer");
            regenRoleFlag
                .setDisplayName("Health Regen")
                .setIcon(new ItemStack(Material.NOTE_BLOCK))
                .setDescription(List.of("§f允许在PVP关闭时，定时恢复血量和饱和度吗？"))
                .setDisplay(true);
            landsApi.getFlagRegistry().register(regenRoleFlag);
            try {
                this.attackPlayerFlag = landsApi.getFlagRegistry().getRole("attack_player");
            } catch (Throwable ignored) {
                this.attackPlayerFlag = null;
            }
            if (this.attackPlayerFlag == null) {
                plugin.getLogger().warning("Lands 'attack_player' role flag not found. PvP check will be skipped (treated as disabled).");
            }
            plugin.getLogger().info("Registered Lands role flag 'health_regen_timer'.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Lands flag registration failed: " + t.getMessage());
        }
    }

    public boolean canPlayerRegen(Player player) {
        if (landsApi == null || regenRoleFlag == null) return true;
        Area area = getArea(player.getLocation());
        if (area == null) return true; // wilderness => allow
        try {
            return area.hasRoleFlag(player.getUniqueId(), regenRoleFlag);
        } catch (Throwable t) {
            return true;
        }
    }

    public boolean isPvPEnabledAt(Player player) {
        if (landsApi == null) return false;
        Location loc = player.getLocation();
        try {
            LandWorld lWord = landsApi.getWorld(player.getWorld());
            if (lWord == null) return false; // World not managed by Lands -> treat as PvP disabled
            if (attackPlayerFlag == null) return false; // Flag unavailable -> skip PvP (disabled)
            return lWord.hasRoleFlag(player.getUniqueId(), loc, attackPlayerFlag);
        } catch (Throwable t) {
            plugin.getLogger().warning("Lands role flag check failed: " + t.getMessage());
            return false; // On error, don't block regen
        }
    }

    private Area getArea(Location loc) {
        try {
            return landsApi.getArea(loc);
        } catch (Throwable t) {
            return null;
        }
    }
}
