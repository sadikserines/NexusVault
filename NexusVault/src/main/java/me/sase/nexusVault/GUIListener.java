package me.sase.nexusVault;

import me.gypopo.economyshopgui.api.EconomyShopGUIHook;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Listens and processes all custom inventory (GUI) clicks.
 * Prevents item theft, handles pagination, processes economy transactions,
 * and controls item extraction securely.
 */
public class GUIListener implements Listener {

    private final NexusVault plugin;

    public GUIListener(NexusVault plugin) {
        this.plugin = plugin;
    }

    /**
     * Triggered when a player clicks inside an inventory.
     * Determines the context of the click based on the inventory title.
     *
     * @param event The InventoryClickEvent triggered by Bukkit.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title.equals("Nexus Vault") || title.startsWith("Sell: ") || title.startsWith("Withdraw: ") || title.equals("Nexus Box Settings")) {
            event.setCancelled(true); // Prevent item stealing

            // Ensure the player is clicking the top custom inventory, not their own inventory
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String uuid = player.getUniqueId().toString();
            int slot = event.getRawSlot();

            // --- 1. MAIN VAULT MENU ---
            if (title.equals("Nexus Vault")) {
                if (slot == 2) { // Previous Page
                    int currentPage = plugin.getGuiManager().playerPages.getOrDefault(player.getUniqueId(), 1);
                    if (currentPage > 1) {
                        plugin.getGuiManager().playerPages.put(player.getUniqueId(), currentPage - 1);
                        plugin.getGuiManager().refreshVault(player, event.getInventory());
                    }
                }
                else if (slot == 6) { // Next Page
                    int currentPage = plugin.getGuiManager().playerPages.getOrDefault(player.getUniqueId(), 1);
                    plugin.getGuiManager().playerPages.put(player.getUniqueId(), currentPage + 1);
                    plugin.getGuiManager().refreshVault(player, event.getInventory());
                }
                else if (slot == 52) { // Close Menu
                    plugin.getGuiManager().activeSessions.remove(player.getUniqueId());
                    player.closeInventory();
                }
                else if (slot == 46) { // Sell All
                    FileConfiguration config = plugin.getDataManager().getItemsConfig();

                    if (config.contains(uuid + ".items") && !config.getConfigurationSection(uuid + ".items").getKeys(false).isEmpty()) {
                        double totalEarnings = 0.0;
                        for (String materialName : config.getConfigurationSection(uuid + ".items").getKeys(false)) {
                            Material material = Material.getMaterial(materialName);
                            int amount = config.getInt(uuid + ".items." + materialName);

                            if (material != null && amount > 0) {
                                ItemStack singleItem = new ItemStack(material, 1);
                                Double unitPrice = EconomyShopGUIHook.getItemSellPrice(singleItem);
                                if (unitPrice != null && unitPrice > 0) {
                                    totalEarnings += (unitPrice * amount);
                                }
                            }
                        }
                        plugin.getDataManager().clearVault(uuid);
                        if (totalEarnings > 0) {
                            plugin.getEconomy().depositPlayer(player, totalEarnings);
                            String formatted = String.format("%,.2f", totalEarnings);
                            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                    ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                    ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                    ChatColor.GREEN + " $" + formatted + ChatColor.GREEN + " deposited into your account.");
                        } else {
                            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                    ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                    ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                    ChatColor.YELLOW + " Vault cleared.");
                        }
                    } else {
                        player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                ChatColor.YELLOW + " There are no items to sell!");
                    }
                }
                // Item Click -> Action Menu
                else if (slot >= 9 && slot <= 44 && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    Material clickedMat = event.getCurrentItem().getType();
                    boolean isSelling = event.isRightClick();

                    VaultSession newSession = new VaultSession(clickedMat, isSelling);
                    plugin.getGuiManager().activeSessions.put(player.getUniqueId(), newSession);
                    plugin.getGuiManager().openActionMenu(player);
                }
            }

            // --- 2. SETTINGS MENU ---
            else if (title.equals("Nexus Box Settings")) {
                if (slot == 13) {
                    Location loc = plugin.getGuiManager().activeBlockSettings.get(player.getUniqueId());
                    if (loc != null) {
                        String ownerUUID = plugin.getDataManager().getOwner(loc);
                        if (ownerUUID != null) {
                            boolean currentState = plugin.getDataManager().isAutoSellEnabled(loc);
                            plugin.getDataManager().setAutoSell(loc, ownerUUID, !currentState);

                            plugin.getGuiManager().updateBlockSettingsMenu(player, event.getInventory(), loc);
                        }
                    }
                }
            }

            // --- 3. ACTION MENU (Sell / Withdraw) ---
            else {
                VaultSession session = plugin.getGuiManager().activeSessions.get(player.getUniqueId());

                if (session == null) {
                    player.closeInventory();
                    return;
                }

                int vaultMaxAmount = plugin.getDataManager().getItemsConfig().getInt(uuid + ".items." + session.material.name(), 0);

                if (vaultMaxAmount <= 0) {
                    plugin.getGuiManager().activeSessions.remove(player.getUniqueId());
                    player.closeInventory();
                    return;
                }

                int m1 = session.isStackMode ? 64 : 1;
                int m16 = session.isStackMode ? 16 * 64 : 16;
                int m32 = session.isStackMode ? 32 * 64 : 32;

                if (slot == 19) session.amount -= m32;
                if (slot == 20) session.amount -= m16;
                if (slot == 21) session.amount -= m1;
                if (slot == 23) session.amount += m1;
                if (slot == 24) session.amount += m16;
                if (slot == 25) session.amount += m32;

                if (slot == 31) { // Toggle Stack Mode
                    session.isStackMode = !session.isStackMode;
                    session.amount = session.isStackMode ? 64 : 1;
                }

                int minAmount = session.isStackMode ? 64 : 1;
                int maxLimit = session.isStackMode ? (64 * 64) : 64;
                int maxAmount = Math.min(maxLimit, vaultMaxAmount);

                if (session.amount > maxAmount) session.amount = maxAmount;
                if (session.amount < minAmount && maxAmount >= minAmount) session.amount = minAmount;
                if (session.amount <= 0) session.amount = 1;

                if (slot == 51) { // Go Back
                    plugin.getGuiManager().activeSessions.remove(player.getUniqueId());
                    plugin.getGuiManager().openVault(player);
                    return;
                }

                if (slot == 47) { // Confirm Action
                    if (session.amount > 0 && session.amount <= vaultMaxAmount) {
                        if (session.isSelling) {
                            Double price = EconomyShopGUIHook.getItemSellPrice(new ItemStack(session.material, 1));
                            if (price != null && price > 0) {
                                double total = price * session.amount;
                                plugin.getEconomy().depositPlayer(player, total);

                                // FORCE SAVE: Prevent duplication bugs on economy transactions
                                plugin.getDataManager().addItemToVault(uuid, session.material, -session.amount, true);

                                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                        ChatColor.GREEN + " Successfully sold: $" + String.format("%,.2f", total));
                            } else {
                                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                        ChatColor.RED + " This item cannot be sold to the market!");
                            }
                        } else {
                            HashMap<Integer, ItemStack> leftOvers = player.getInventory().addItem(new ItemStack(session.material, session.amount));
                            if (!leftOvers.isEmpty()) {
                                int actualGiven = session.amount;
                                for (ItemStack left : leftOvers.values()) {
                                    actualGiven -= left.getAmount();
                                }
                                plugin.getDataManager().addItemToVault(uuid, session.material, -actualGiven, true);
                                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                        ChatColor.RED + " Your inventory is full, remaining items were kept in the vault!");
                            } else {
                                plugin.getDataManager().addItemToVault(uuid, session.material, -session.amount, true);
                                player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "Nexus" +
                                        ChatColor.DARK_RED + ChatColor.BOLD + "Vault" +
                                        ChatColor.DARK_GRAY + ChatColor.BOLD + " > " +
                                        ChatColor.GREEN + " Items successfully withdrawn to your inventory.");
                            }
                        }
                    }
                    plugin.getGuiManager().activeSessions.remove(player.getUniqueId());
                    plugin.getGuiManager().openVault(player);
                    return;
                }

                // Update the visual representation
                plugin.getGuiManager().updateActionMenu(player, event.getInventory());
            }
        }
    }
}