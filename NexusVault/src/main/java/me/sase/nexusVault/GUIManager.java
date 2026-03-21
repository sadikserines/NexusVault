package me.sase.nexusVault;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Handles the creation, rendering, and live updating of all plugin GUI menus.
 * Manages paginations and active visual sessions for players.
 */
public class GUIManager {
    private final NexusVault plugin;

    // Stores the active transaction session object for players
    public final HashMap<UUID, VaultSession> activeSessions = new HashMap<>();

    // Keeps track of the current page number a player is viewing in the main menu
    public final HashMap<UUID, Integer> playerPages = new HashMap<>();

    // Tracks which block location a player is currently configuring
    public final HashMap<UUID, Location> activeBlockSettings = new HashMap<>();

    public GUIManager(NexusVault plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the main Nexus Vault GUI for the player.
     * Draws the borders and static buttons.
     *
     * @param player The player to open the menu for.
     */
    public void openVault(Player player) {
        Inventory vault = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Nexus Vault");

        playerPages.putIfAbsent(player.getUniqueId(), 1);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        int[] glassSlots = {0, 1, 3, 5, 7, 8, 45, 47, 48, 50, 51, 53};
        for (int slot : glassSlots) {
            vault.setItem(slot, glass);
        }

        vault.setItem(46, createCustomHead("http://textures.minecraft.net/texture/bc0e6d9e242735481918c5fd14498bd760bb9f4ff6430ad4696b38e8a883da97", ChatColor.GREEN + "Sell All"));
        vault.setItem(49, createCustomHead("http://textures.minecraft.net/texture/97f57e7aa8de86591bb0bc52cba30a49d931bfabbd47bbc80bdd662251392161", ChatColor.GRAY + "Settings"));
        vault.setItem(52, createCustomHead("http://textures.minecraft.net/texture/bb78fa5defe72debcd9c76ab9f4e114250479bb9b44f42887bbf6f738612b", ChatColor.RED + "Close"));

        refreshVault(player, vault);
        player.openInventory(vault);
    }

    /**
     * Dynamically populates the main Vault menu.
     * Handles the pagination logic and applies pricing logic to the items.
     *
     * @param player The player viewing the inventory.
     * @param vault The Inventory object to update.
     */
    public void refreshVault(Player player, Inventory vault) {
        String uuid = player.getUniqueId().toString();
        FileConfiguration config = plugin.getDataManager().getItemsConfig();

        for (int i = 9; i <= 44; i++) vault.setItem(i, null);

        List<String> validItems = new ArrayList<>();

        if (config.contains(uuid + ".items")) {
            for (String materialName : config.getConfigurationSection(uuid + ".items").getKeys(false)) {
                if (config.getInt(uuid + ".items." + materialName) > 0) {
                    validItems.add(materialName);
                }
            }
        }

        int itemsPerPage = 36;
        int maxPage = Math.max(1, (int) Math.ceil(validItems.size() / (double) itemsPerPage));
        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

        if (currentPage > maxPage) {
            currentPage = maxPage;
            playerPages.put(player.getUniqueId(), currentPage);
        }

        if (currentPage > 1) {
            vault.setItem(2, createCustomHead("http://textures.minecraft.net/texture/a185c97dbb8353de652698d24b64327b793a3f32a98be67b719fbedab35e", ChatColor.YELLOW + "Previous Page"));
        } else {
            vault.setItem(2, createColorPane(Material.GRAY_STAINED_GLASS_PANE, " ", 1));
        }

        vault.setItem(4, createCustomHead("http://textures.minecraft.net/texture/8375534f873f7cbd516084a208bad30546008622f5e3792b36b58538aa156943", ChatColor.GRAY + "Page: " + ChatColor.YELLOW + currentPage + "/" + maxPage));

        if (currentPage < maxPage) {
            vault.setItem(6, createCustomHead("http://textures.minecraft.net/texture/31c0ededd7115fc1b23d51ce966358b27195daf26ebb6e45a66c34c69c34091", ChatColor.YELLOW + "Next Page"));
        } else {
            vault.setItem(6, createColorPane(Material.GRAY_STAINED_GLASS_PANE, " ", 1));
        }

        int startIndex = (currentPage - 1) * itemsPerPage;
        int slotIndex = 9;

        for (int i = startIndex; i < Math.min(startIndex + itemsPerPage, validItems.size()); i++) {
            String materialName = validItems.get(i);
            Material material = Material.getMaterial(materialName);
            if (material == null) continue;

            int amount = config.getInt(uuid + ".items." + materialName);

            ItemStack displayItem = new ItemStack(material);
            ItemMeta meta = displayItem.getItemMeta();

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Total Amount: " + ChatColor.YELLOW + amount);

            ItemStack singleItem = new ItemStack(material, 1);
            Double unitPrice = EconomyShopGUIHook.getItemSellPrice(singleItem);

            if (unitPrice != null && unitPrice > 0) {
                double totalPrice = unitPrice * amount;
                lore.add(ChatColor.GRAY + "Total Profit: " + ChatColor.GREEN + "$" + String.format("%,.2f", totalPrice));
            } else {
                lore.add(ChatColor.RED + "Cannot be sold!");
            }

            lore.add("");
            lore.add(ChatColor.GRAY + "'Left Click' " + ChatColor.AQUA + "to withdraw");
            lore.add(ChatColor.GRAY + "'Right Click' " + ChatColor.YELLOW + "to sell");

            meta.setLore(lore);
            displayItem.setItemMeta(meta);

            vault.setItem(slotIndex, displayItem);
            slotIndex++;
        }
    }

    /**
     * Opens the dynamic action menu where a player can specify quantities for withdrawing or selling.
     *
     * @param player The player initializing the action.
     */
    public void openActionMenu(Player player) {
        VaultSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        String title = ChatColor.DARK_GRAY + (session.isSelling ? "Sell: " : "Withdraw: ") + session.material.name();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta(); bgMeta.setDisplayName(" "); bg.setItemMeta(bgMeta);

        for(int i = 0; i < 9; i++) inv.setItem(i, bg);
        for(int i = 45; i < 54; i++) {
            if (i == 47 || i == 51) continue;
            inv.setItem(i, bg);
        }

        inv.setItem(47, createCustomHead("http://textures.minecraft.net/texture/bc0e6d9e242735481918c5fd14498bd760bb9f4ff6430ad4696b38e8a883da97", ChatColor.GREEN + "Confirm"));
        inv.setItem(51, createCustomHead("http://textures.minecraft.net/texture/bb78fa5defe72debcd9c76ab9f4e114250479bb9b44f42887bbf6f738612b", ChatColor.RED + "Go Back"));

        updateActionMenu(player, inv);

        player.openInventory(inv);
    }

    /**
     * Updates the action menu dynamically when modifier buttons (+/-) are clicked.
     *
     * @param player The player engaged in the session.
     * @param inv The action menu inventory to be updated.
     */
    public void updateActionMenu(Player player, Inventory inv) {
        VaultSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        for(int i = 9; i <= 44; i++) inv.setItem(i, null);

        Material decMat = session.isStackMode ? Material.PURPLE_STAINED_GLASS : Material.PURPLE_STAINED_GLASS_PANE;
        Material incMat = session.isStackMode ? Material.LIGHT_BLUE_STAINED_GLASS : Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        String sTxt = session.isStackMode ? " Stack" : "";

        inv.setItem(19, createColorPane(decMat, ChatColor.RED + "-32" + sTxt, 32));
        inv.setItem(20, createColorPane(decMat, ChatColor.RED + "-16" + sTxt, 16));
        inv.setItem(21, createColorPane(decMat, ChatColor.RED + "-1" + sTxt, 1));

        inv.setItem(23, createColorPane(incMat, ChatColor.GREEN + "+1" + sTxt, 1));
        inv.setItem(24, createColorPane(incMat, ChatColor.GREEN + "+16" + sTxt, 16));
        inv.setItem(25, createColorPane(incMat, ChatColor.GREEN + "+32" + sTxt, 32));

        int displayAmount = session.isStackMode ? (session.amount / 64) : session.amount;
        if (displayAmount < 1) displayAmount = 1;
        if (displayAmount > 64) displayAmount = 64;

        ItemStack center = new ItemStack(session.material, displayAmount);
        ItemMeta centerMeta = center.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (session.isStackMode) {
            lore.add(ChatColor.GRAY + "Selected Amount: " + ChatColor.YELLOW + displayAmount + " Stack " + ChatColor.GRAY + "(" + session.amount + " Items)");
        } else {
            lore.add(ChatColor.GRAY + "Selected Amount: " + ChatColor.YELLOW + session.amount + " Items");
        }

        if (session.isSelling) {
            Double price = EconomyShopGUIHook.getItemSellPrice(new ItemStack(session.material, 1));
            double total = (price != null ? price : 0.0) * session.amount;
            lore.add(ChatColor.GRAY + "Profit: " + ChatColor.GREEN + "$" + String.format("%,.2f", total));
        }
        centerMeta.setLore(lore);
        center.setItemMeta(centerMeta);
        inv.setItem(22, center);

        if (session.isStackMode) {
            inv.setItem(31, createCustomHead("http://textures.minecraft.net/texture/bda911437b4ecfaa3c1894162217c01b68a55c89bb2f4d4927345ce5c794", ChatColor.YELLOW + "Switch to Normal Mode"));
        } else {
            inv.setItem(31, createCustomHead("http://textures.minecraft.net/texture/d5c6dc2bbf51c36cfc7714585a6a5683ef2b14d47d8ff714654a893f5da622", ChatColor.YELLOW + "Switch to Stack Mode"));
        }
    }

    /**
     * Opens the setting menu specific to a single Nexus Box block.
     *
     * @param player The owner accessing the settings.
     * @param loc The location of the target Nexus Box.
     */
    public void openBlockSettingsMenu(Player player, Location loc) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Nexus Box Settings");

        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta(); bgMeta.setDisplayName(" "); bg.setItemMeta(bgMeta);

        for (int i = 0; i < 27; i++) {
            if (i == 13) continue;
            inv.setItem(i, bg);
        }

        updateBlockSettingsMenu(player, inv, loc);

        activeBlockSettings.put(player.getUniqueId(), loc);
        player.openInventory(inv);
    }

    /**
     * Dynamically updates the block setting menu to reflect Auto-Sell status.
     */
    public void updateBlockSettingsMenu(Player player, Inventory inv, Location loc) {
        boolean isAutoSell = plugin.getDataManager().isAutoSellEnabled(loc);

        if (isAutoSell) {
            ItemStack onBtn = createCustomHead("http://textures.minecraft.net/texture/921928ea67d3a8b97d212758f15cccac1024295b185b319264844f4c5e1e61e", ChatColor.GREEN + "" + ChatColor.BOLD + "Auto Sell: ON");
            ItemMeta meta = onBtn.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to disable.");
            meta.setLore(lore);
            onBtn.setItemMeta(meta);
            inv.setItem(13, onBtn);
        } else {
            ItemStack offBtn = createCustomHead("http://textures.minecraft.net/texture/25ef68dcbd58234ba7aee2ad91ca6fa7ce23f9a32345b48d6e5f5b86a68b5b", ChatColor.RED + "" + ChatColor.BOLD + "Auto Sell: OFF");
            ItemMeta meta = offBtn.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to enable.");
            meta.setLore(lore);
            offBtn.setItemMeta(meta);
            inv.setItem(13, offBtn);
        }
    }

    /**
     * Utility method to generate custom player heads using a base64 texture URL.
     */
    private ItemStack createCustomHead(String urlStr, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(displayName);

        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "GUIHead");
        PlayerTextures textures = profile.getTextures();
        try { textures.setSkin(new URL(urlStr)); } catch (MalformedURLException e) { e.printStackTrace(); }
        profile.setTextures(textures);
        meta.setPlayerProfile((com.destroystokyo.paper.profile.PlayerProfile) profile);
        head.setItemMeta(meta);

        return head;
    }

    /**
     * Utility method to generate custom colored glass panes/blocks with defined quantities.
     */
    private ItemStack createColorPane(Material mat, String name, int amount) {
        ItemStack item = new ItemStack(mat, Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}