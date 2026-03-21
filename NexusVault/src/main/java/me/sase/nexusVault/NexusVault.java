package me.sase.nexusVault;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The core initialization class for the Nexus Vault plugin.
 * Bootstraps dependencies, registers event listeners, commands, and automated scheduler tasks.
 */
public class NexusVault extends JavaPlugin {

    private ItemManager itemManager;
    private DataManager dataManager;
    private GUIManager guiManager;

    private static Economy econ = null;

    private boolean griefPreventionEnabled = false;

    /**
     * Executed when the server enables the plugin.
     * Responsible for asserting required API hooks and activating background tasks.
     */
    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Error: Vault plugin not found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            griefPreventionEnabled = true;
            getLogger().info("GriefPrevention found! Claim protection enabled.");
        } else {
            getLogger().info("GriefPrevention not found! Claim protection disabled.");
        }

        dataManager = new DataManager(this);
        itemManager = new ItemManager(this);
        guiManager = new GUIManager(this);

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperListener(this), this);

        getCommand("nv").setExecutor(new VaultCommand(this));

        // Live UI Updater: Synchronizes the vault UI for players viewing their inventory.
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.getOpenInventory().getTitle().equals(ChatColor.DARK_GRAY + "Nexus Vault")) {
                    guiManager.refreshVault(player, player.getOpenInventory().getTopInventory());
                }
            }
        }, 20L, 20L);

        // Auto-Save Mechanism: Commits RAM cache into the disk every 5 minutes to prevent I/O latency.
        getServer().getScheduler().runTaskTimer(this, () -> {
            dataManager.saveItemsData();
            getLogger().info("Items successfully saved to disk. [Auto-Save]");
        }, 6000L, 6000L);

        getLogger().info("Nexus Vault plugin successfully activated!");
    }

    /**
     * Executed during server shutdown or plugin reload.
     * Forces an emergency commit of all cached data arrays to prevent any data loss.
     */
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveItemsData();
            dataManager.saveBlocksData();
        }
        getLogger().info("Nexus Vault plugin disabled. All data safely secured!");
    }

    /**
     * Hooks into the Vault economy API.
     *
     * @return true if the economy provider was successfully found, false otherwise.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public ItemManager getItemManager() { return itemManager; }
    public DataManager getDataManager() { return dataManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public Economy getEconomy() { return econ; }

    public boolean isGriefPreventionEnabled() { return griefPreventionEnabled; }
}