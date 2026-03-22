# 📦 Nexus Vault

**Nexus Vault** is an advanced, high-performance, and deeply secure virtual storage and auto-sell plugin designed for professional Spigot/PaperMC servers. 

Unlike traditional storage plugins that suffer from severe I/O latency and lag, Nexus Vault utilizes a custom-built **Zero-Lag RAM Caching** architecture. This ensures flawless server TPS, even with hundreds of players and automated hoppers operating simultaneously.

## ✨ Core Features

* **⚡ Zero-Lag Caching Architecture:** All hopper extractions and database queries are processed instantly via server RAM. Data is securely flushed to the disk asynchronously via automated 5-minute Auto-Save intervals, completely preventing disk lag.
* **🛒 Dynamic GUI & Pagination:** A beautifully designed interface where players can manage their infinite storage. Fully supports automated pagination handling over 36+ unique item variants.

  <img width="690" height="503" alt="image" src="https://github.com/user-attachments/assets/e7aa3dc3-3bcd-43d6-96d9-f76fc19374ab" />

* **⚙️ Advanced Action Menus:** Integrated transaction GUIs allowing players to precisely alter their withdraw or sell amounts using `1, 16, 32, or x64 (Stack)` visual modifiers.

  <img width="676" height="499" alt="image" src="https://github.com/user-attachments/assets/72d3f522-b0b1-41a9-939f-2c4e9ce0b34a" />

* **💰 Integrated Auto-Sell Mechanics:** Players can right-click their physical "Nexus Box" to toggle the Auto-Sell feature. When enabled, items injected via hoppers bypass the vault and are instantly liquidated based on the server's dynamic economy.

  <img width="722" height="289" alt="image" src="https://github.com/user-attachments/assets/11a09c37-54c1-4d25-b46a-701ed7bfa1e7" />
   
* **🛡️ Bulletproof Anti-Dupe:** Hardcoded constraints prevent GUI overlap exploits, shift-click vulnerabilities, and guarantee transactional integrity before dropping or withdrawing items.
* **🔒 GriefPrevention Support:** Natively hooks into GriefPrevention. Players are strictly prevented from deploying Nexus Boxes in claims they do not own, protecting your server's territorial integrity.

## 🔗 Dependencies

Nexus Vault requires the following plugins to operate at maximum capacity:

**Required:**
* **[Vault](https://www.spigotmc.org/resources/vault.34315/):** Powers the economy transactions and account deposits.
* **[EconomyShopGUI](https://www.spigotmc.org/resources/economyshopgui.69927/):** Acts as the dynamic pricing engine for the Auto-Sell and GUI liquidation functions.

**Optional (Soft-Depends):**
* **[GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/):** Claim protections (The plugin will operate normally if not found on the server).

## 🚀 Installation

1. Verify that the required dependencies are running on your server.
2. Download the latest `NexusVault.jar` from the **Releases** tab.
3. Drop the `.jar` file into your server's `plugins` directory.
4. Restart your server to initialize the module.
5. Nexus Vault will automatically generate its configuration matrices (`nv_blocks_data.yml` and `nv_items_data.yml`) inside the plugin folder.

## 🎮 Commands & Permissions

* `/nv` or `/nexusvault` : Opens the player's personal interactive Nexus Vault.
  * *No specific permissions required by default. Only executable by Players.*

## 🛠️ How to Play

1. **Crafting the Nexus Box:** Place 4x Gold Ingots in the corners, 4x Netherite Blocks in the center of the Gold Ingots and a Chest in the middle inside a Crafting Table.

   <img width="780" height="427" alt="image" src="https://github.com/user-attachments/assets/2ee9f22d-22b4-476d-877e-29bce81da0d1" />
   
2. **Storage Automation:** Place the Nexus Box down and route hoppers into it. It will securely pull items into your virtual UI.
3. **Management:** Right-click the physical box to toggle **Auto-Sell** settings.

   <img width="722" height="289" alt="image" src="https://github.com/user-attachments/assets/11a09c37-54c1-4d25-b46a-701ed7bfa1e7" />



---
*Developed with performance, security, and modern server architecture in mind.*
