package me.sase.nexusVault;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all database operations for the plugin.
 * Manages YAML file reading/writing and utilizes RAM caching to ensure
 * high-performance block lookups during high-frequency hopper operations.
 */
public class DataManager {

    private final NexusVault plugin;

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private File itemsFile;
    private FileConfiguration itemsConfig;

    // Caches the owner UUID associated with a block location string
    private final Map<String, String> blockCache = new HashMap<>();

    // Caches the Auto-Sell status of a block location to prevent disk lag
    private final Map<String, Boolean> autoSellCache = new HashMap<>();

    public DataManager(NexusVault plugin) {
        this.plugin = plugin;
        setupFiles();
        loadCache();
    }

    /**
     * Initializes the data folder and creates the YAML files if they do not exist.
     */
    private void setupFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        blocksFile = new File(plugin.getDataFolder(), "nv_blocks_data.yml");
        if (!blocksFile.exists()) {
            try { blocksFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);

        itemsFile = new File(plugin.getDataFolder(), "nv_items_data.yml");
        if (!itemsFile.exists()) {
            try { itemsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    /**
     * Loads the block configurations from disk into the RAM cache upon server startup.
     * Ensures backward compatibility with older configuration structures.
     */
    private void loadCache() {
        blockCache.clear();
        autoSellCache.clear();

        for (String uuid : blocksConfig.getKeys(false)) {
            ConfigurationSection uuidSection = blocksConfig.getConfigurationSection(uuid);
            if (uuidSection == null) continue;

            // Backward compatibility for old list-based block storage
            if (blocksConfig.isList(uuid + ".blocks")) {
                List<String> oldBlocks = blocksConfig.getStringList(uuid + ".blocks");
                blocksConfig.set(uuid + ".blocks", null);
                for (String locStr : oldBlocks) {
                    blocksConfig.set(uuid + ".blocks." + locStr + ".autosell", false);
                    blockCache.put(locStr, uuid);
                    autoSellCache.put(locStr, false);
                }
                saveBlocksData();
                continue;
            }

            // Load new format
            ConfigurationSection blocksSection = blocksConfig.getConfigurationSection(uuid + ".blocks");
            if (blocksSection != null) {
                for (String locStr : blocksSection.getKeys(false)) {
                    blockCache.put(locStr, uuid);
                    boolean isAutoSell = blocksConfig.getBoolean(uuid + ".blocks." + locStr + ".autosell", false);
                    autoSellCache.put(locStr, isAutoSell);
                }
            }
        }
    }

    public FileConfiguration getBlocksConfig() { return blocksConfig; }
    public FileConfiguration getItemsConfig() { return itemsConfig; }

    /** Saves block data to the disk. */
    public void saveBlocksData() {
        try { blocksConfig.save(blocksFile); } catch (IOException e) { e.printStackTrace(); }
    }

    /** Saves item data to the disk. */
    public void saveItemsData() {
        try { itemsConfig.save(itemsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Converts a Bukkit Location object into a flat string for YAML configuration keys.
     *
     * @param loc The Bukkit Location.
     * @return Formatted string representing the location.
     */
    public String locationToString(Location loc) {
        if (loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    /**
     * Registers a new Nexus Box into the database and cache.
     *
     * @param loc The location of the block.
     * @param uuid The UUID of the player who owns it.
     */
    public void addBlock(Location loc, String uuid) {
        String locStr = locationToString(loc);

        if (!blocksConfig.contains(uuid + ".blocks." + locStr)) {
            blocksConfig.set(uuid + ".blocks." + locStr + ".autosell", false); // Default OFF
            saveBlocksData();
            blockCache.put(locStr, uuid);
            autoSellCache.put(locStr, false);
        }
    }

    /**
     * Removes an existing Nexus Box from the database and cache.
     *
     * @param loc The location of the block to be removed.
     */
    public void removeBlock(Location loc) {
        String locStr = locationToString(loc);
        String ownerUUID = getOwner(loc);

        if (ownerUUID != null) {
            blocksConfig.set(ownerUUID + ".blocks." + locStr, null);

            // Clean up the node if the player has no blocks left
            ConfigurationSection blocksSection = blocksConfig.getConfigurationSection(ownerUUID + ".blocks");
            if (blocksSection == null || blocksSection.getKeys(false).isEmpty()) {
                blocksConfig.set(ownerUUID, null);
            }

            saveBlocksData();
            blockCache.remove(locStr);
            autoSellCache.remove(locStr);
        }
    }

    /**
     * Retrieves the owner UUID of a Nexus Box from the high-speed RAM cache.
     *
     * @param loc The location of the block.
     * @return The UUID string of the owner, or null if unregistered.
     */
    public String getOwner(Location loc) {
        return blockCache.get(locationToString(loc));
    }

    /**
     * Checks if the Auto-Sell mode is enabled for a specific block.
     *
     * @param loc The location of the block.
     * @return true if Auto-Sell is enabled, false otherwise.
     */
    public boolean isAutoSellEnabled(Location loc) {
        return autoSellCache.getOrDefault(locationToString(loc), false);
    }

    /**
     * Toggles the Auto-Sell mode for a specific block and saves the state.
     *
     * @param loc The location of the block.
     * @param uuid The UUID of the owner.
     * @param status The new Auto-Sell status.
     */
    public void setAutoSell(Location loc, String uuid, boolean status) {
        String locStr = locationToString(loc);
        blocksConfig.set(uuid + ".blocks." + locStr + ".autosell", status);
        saveBlocksData();
        autoSellCache.put(locStr, status);
    }

    /**
     * Adds or removes an amount of a specific item from a player's virtual vault.
     *
     * @param uuid The UUID of the player.
     * @param material The item material.
     * @param amount The amount to add (can be negative to remove).
     * @param forceSave If true, immediately writes to disk to prevent dupe bugs. If false, relies on Auto-Save task.
     */
    public void addItemToVault(String uuid, Material material, int amount, boolean forceSave) {
        String path = uuid + ".items." + material.name();

        int currentAmount = itemsConfig.getInt(path, 0);
        int newAmount = currentAmount + amount;

        if (newAmount <= 0) {
            itemsConfig.set(path, null);
        } else {
            itemsConfig.set(path, newAmount);
        }

        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection(uuid + ".items");
        if (itemsSection != null && itemsSection.getKeys(false).isEmpty()) {
            itemsConfig.set(uuid + ".items", null);
        }

        if (forceSave) {
            saveItemsData();
        }
    }

    /**
     * Entirely clears the virtual vault items for a specific player.
     *
     * @param uuid The UUID of the player.
     */
    public void clearVault(String uuid) {
        itemsConfig.set(uuid + ".items", null);
        saveItemsData();
    }
}