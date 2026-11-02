package com.fonzzscripter.staffjobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Objects;

public final class StaffJobsPlugin extends JavaPlugin implements Listener {

    public static final String PREFIX =
            ChatColor.YELLOW + "[ʜᴇʟᴘᴇʀꜱᴛᴀꜰꜰ] ";

    private static final String MENU_TITLE =
            ChatColor.DARK_GRAY + "✦ STAFFJOBS MENU";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("staffmenu"))
                .setExecutor(this::handleCommand);

        Objects.requireNonNull(getCommand("report"))
                .setExecutor(this::handleCommand);

        getLogger().info("STAFFJOBS by FonzzScripter enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("STAFFJOBS disabled.");
    }

    private boolean handleCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + "Gunakan command di dalam game.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("staffmenu")) {
            if (!player.hasPermission("staffjobs.use")) {
                player.sendMessage(PREFIX + "Kamu tidak memiliki izin.");
                return true;
            }

            openStaffMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("report")) {
            if (args.length == 0) {
                player.sendMessage(PREFIX + "Gunakan /report <pesan>");
                return true;
            }

            player.sendMessage(PREFIX + "Report berhasil diterima.");
            return true;
        }

        return true;
    }

    private void openStaffMenu(Player player) {
        Inventory inv = Bukkit.createInventory(
                null,
                54,
                MENU_TITLE
        );

        for (int i = 0; i < 54; i++) {
            setItem(inv, i,
                    Material.BLACK_STAINED_GLASS_PANE, " ");
        }

        setItem(inv, 4, Material.NETHER_STAR,
                ChatColor.YELLOW + "✦ STAFFJOBS",
                "",
                ChatColor.GRAY + "Name: STAFFJOBS",
                ChatColor.GRAY + "Version: 1.0.0",
                ChatColor.GRAY + "Author: FonzzScripter",
                ChatColor.GRAY + "Target: Paper 1.21.11"
        );

        setItem(inv, 10, Material.PLAYER_HEAD,
                ChatColor.YELLOW + "Player Management");

        setItem(inv, 12, Material.IRON_SWORD,
                ChatColor.RED + "Kick Player");

        setItem(inv, 14, Material.BARRIER,
                ChatColor.RED + "Ban Player");

        setItem(inv, 16, Material.ANVIL,
                ChatColor.RED + "Tempban Player");

        setItem(inv, 20, Material.REDSTONE,
                ChatColor.RED + "Ban IP");

        setItem(inv, 22, Material.ICE,
                ChatColor.AQUA + "Freeze Player");

        setItem(inv, 24, Material.EMERALD,
                ChatColor.GREEN + "OP Management");

        setItem(inv, 29, Material.BOOK,
                ChatColor.YELLOW + "Server Information");

        setItem(inv, 31, Material.CLOCK,
                ChatColor.YELLOW + "TPS Monitor");

        setItem(inv, 33, Material.SKELETON_SKULL,
                ChatColor.RED + "Last Death Logs");

        setItem(inv, 40, Material.WRITABLE_BOOK,
                ChatColor.GOLD + "Report Logs");

        setItem(inv, 49, Material.BARRIER,
                ChatColor.RED + "Close");

        player.openInventory(inv);
    }

    private void setItem(
            Inventory inv,
            int slot,
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!event.getView().getTitle().equals(MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot >= 0 && slot < 54) {
            player.sendMessage(
                    PREFIX + "Fitur ini sedang dalam pengembangan."
            );
        }
    }
}
