package io.github.loliiiico.landHealthRegenTimerTask.lands;

import io.github.loliiiico.landHealthRegenTimerTask.LandHealthRegenTimerTask;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.UUID;

// Lands API imports aligned with 7.15.x
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Area;
import me.angeschossen.lands.api.land.LandWorld;
import me.angeschossen.lands.api.player.LandPlayer;
import static me.angeschossen.lands.api.flags.type.Flags.*;

public class LandsHook {
    private final LandHealthRegenTimerTask plugin;

    private RoleFlag regenRoleFlag;
    private LandsIntegration landsApi;

    public LandsHook(LandHealthRegenTimerTask plugin) {
        this.plugin = plugin;
    }

    /**
     * Must be called during plugin onLoad, after Lands loaded but before it enables.
     */
    public void registerHealthRegenTimerTaskFlag() {
        try {
            this.landsApi = LandsIntegration.of(plugin);
            // Reuse existing flag if it already exists (e.g., after reload)
            this.regenRoleFlag = landsApi.getFlagRegistry().getRole("health_regen_timer");
            if (this.regenRoleFlag != null) {
                plugin.getLogger().info("Using existing Lands role flag 'health_regen_timer'.");
            }
            // Create role flag for players under ACTION category
            if (this.regenRoleFlag == null) {
            this.regenRoleFlag = RoleFlag.of(landsApi, FlagTarget.PLAYER, RoleFlagCategory.ACTION, "health_regen_timer")
                .setDisplayName("Health Regen")
                .setIcon(new ItemStack(Material.NOTE_BLOCK))
                .setDescription(List.of("§f允许在PVP关闭时，定时恢复血量和饱和度吗？"))
                .setDisplay(true);
            landsApi.getFlagRegistry().register(regenRoleFlag);
            }
            // Do not resolve built-in flags here; resolve lazily when needed after Lands enables
            plugin.getLogger().info("Registered Lands role flag 'health_regen_timer'.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Lands flag 注册失败: " + t.getMessage());
        }
    }

    public boolean canPlayerRegen(Player player) {
        if (landsApi == null || regenRoleFlag == null) {
            return true;
        }
        Location loc = player.getLocation();
        try {
            LandWorld lWord = landsApi.getWorld(player.getWorld());
            //plugin.getLogger().info("恢复是否启用？"+player.getName()+lWord.hasRoleFlag(player.getUniqueId(),loc,regenRoleFlag));
            return lWord.hasRoleFlag(player.getUniqueId(),loc,regenRoleFlag);
        } catch (Throwable t) {
            return true;
        }
    }

    public boolean isPvPEnabledAt(Player player) {
        if (landsApi == null) {
            return false;
        }
        Location loc = player.getLocation();
        LandWorld lWord = landsApi.getWorld(player.getWorld());
        try {
            if (lWord == null) return false; // World not managed by Lands -> treat as PvP disabled
            //plugin.getLogger().info("pvp是否开启？"+player.getName()+lWord.hasRoleFlag(player.getUniqueId(),loc,ATTACK_PLAYER));
            return lWord.hasRoleFlag(player.getUniqueId(),loc,ATTACK_PLAYER);
        } catch (Throwable t) {
            plugin.getLogger().warning("Lands role flag check failed: " + t.getMessage());
            return false; // On error, don't block regen
        }
    }

}
