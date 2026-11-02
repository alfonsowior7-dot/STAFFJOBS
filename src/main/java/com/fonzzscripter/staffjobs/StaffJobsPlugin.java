package com.fonzzscripter.staffjobs;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class StaffJobsPlugin extends JavaPlugin implements Listener {

    public static final String PREFIX =
            ChatColor.YELLOW + "[ʜᴇʟᴘᴇʀꜱᴛᴀꜰꜰ] ";

    private static final String MENU =
            ChatColor.DARK_GRAY + "✦ STAFFJOBS MENU";

    private static final String PLAYERS =
            ChatColor.DARK_GRAY + "✦ PLAYER MANAGEMENT";

    private static final String ACTIONS =
            ChatColor.DARK_GRAY + "✦ PLAYER ACTIONS";

    private static final String CONFIRM =
            ChatColor.DARK_GRAY + "✦ CONFIRM ACTION";

    private final Set<UUID> frozen = new HashSet<>();

    private final Map<UUID, UUID> selected = new HashMap<>();

    private final Map<UUID, String> pendingAction = new HashMap<>();

    private File frozenFile;
    private YamlConfiguration frozenData;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        loadFrozen();

        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("staffmenu"))
                .setExecutor(this::handleCommand);

        Objects.requireNonNull(getCommand("report"))
                .setExecutor(this::handleCommand);

        getLogger().info("STAFFJOBS by FonzzScripter enabled!");
    }

    @Override
    public void onDisable() {

        saveFrozen();

        selected.clear();
        pendingAction.clear();

        getLogger().info("STAFFJOBS disabled.");
    }

    // FREEZE DATA STORAGE

    private void loadFrozen() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        frozenFile = new File(getDataFolder(), "frozen.yml");

        frozenData = YamlConfiguration.loadConfiguration(frozenFile);

        frozen.clear();

        for (String uuid : frozenData.getStringList("frozen-players")) {

            try {
                frozen.add(UUID.fromString(uuid));

            } catch (IllegalArgumentException exception) {
                getLogger().warning("Invalid UUID in frozen.yml: " + uuid);
            }
        }

        getLogger().info("Loaded " + frozen.size() + " frozen players.");
    }

    private void saveFrozen() {

        if (frozenData == null || frozenFile == null) {
            return;
        }

        List<String> uuids = new ArrayList<>();

        for (UUID uuid : frozen) {
            uuids.add(uuid.toString());
        }

        frozenData.set("frozen-players", uuids);

        try {

            frozenData.save(frozenFile);

        } catch (IOException exception) {

            getLogger().severe("Failed to save frozen.yml!");
            exception.printStackTrace();
        }
    }

    // COMMANDS

    private boolean handleCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Use this command in-game.");
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

    // MAIN MENU

    private void openStaffMenu(Player player) {

        Inventory inv = Bukkit.createInventory(null, 54, MENU);

        fill(inv);

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

    // PLAYER MANAGEMENT

    private void openPlayerManagement(Player player) {

        Inventory inv = Bukkit.createInventory(null, 54, PLAYERS);

        fill(inv);

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int index = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {

            if (index >= slots.length) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);

            SkullMeta meta = (SkullMeta) head.getItemMeta();

            meta.setOwningPlayer(target);

            meta.setDisplayName(
                    ChatColor.YELLOW + target.getName()
            );

            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Click to manage player",
                    ChatColor.GRAY + "UUID: " + target.getUniqueId()
            ));

            head.setItemMeta(meta);

            inv.setItem(slots[index], head);

            index++;
        }

        setItem(inv, 49, Material.ARROW,
                ChatColor.YELLOW + "Back");

        player.openInventory(inv);
    }

    // PLAYER ACTION MENU

    private void openActions(Player admin, Player target) {

        selected.put(admin.getUniqueId(), target.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27, ACTIONS);

        fill(inv);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta skull = (SkullMeta) head.getItemMeta();

        skull.setOwningPlayer(target);

        skull.setDisplayName(
                ChatColor.YELLOW + target.getName()
        );

        skull.setLore(Arrays.asList(
                ChatColor.GRAY + "Selected Player",
                ChatColor.GRAY + "UUID: " + target.getUniqueId()
        ));

        head.setItemMeta(skull);

        inv.setItem(4, head);

        setItem(inv, 10, Material.IRON_SWORD,
                ChatColor.RED + "KICK PLAYER");

        setItem(inv, 12, Material.BARRIER,
                ChatColor.RED + "BAN PLAYER");

        setItem(inv, 14, Material.ICE,
                frozen.contains(target.getUniqueId())
                        ? ChatColor.GREEN + "UNFREEZE PLAYER"
                        : ChatColor.AQUA + "FREEZE PLAYER");

        setItem(inv, 22, Material.ARROW,
                ChatColor.YELLOW + "BACK");

        admin.openInventory(inv);
    }

    // CONFIRMATION MENU

    private void openConfirmation(
            Player admin,
            Player target,
            String action
    ) {

        pendingAction.put(admin.getUniqueId(), action);

        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM);

        fill(inv);

        setItem(inv, 11, Material.LIME_WOOL,
                ChatColor.GREEN + "CONFIRM " + action);

        setItem(inv, 15, Material.RED_WOOL,
                ChatColor.RED + "CANCEL");

        setItem(inv, 4, Material.PLAYER_HEAD,
                ChatColor.YELLOW + target.getName());

        admin.openInventory(inv);
    }

    // INVENTORY CLICK

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        String title = event.getView().getTitle();

        if (!title.equals(MENU)
                && !title.equals(PLAYERS)
                && !title.equals(ACTIONS)
                && !title.equals(CONFIRM)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 0 ||
                slot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (!admin.hasPermission("staffjobs.use")) {
            admin.closeInventory();
            return;
        }

        // MAIN MENU

        if (title.equals(MENU)) {

            if (slot == 49) {
                admin.closeInventory();
            }

            if (slot == 10) {
                openPlayerManagement(admin);
            }

            return;
        }

        // PLAYER LIST

        if (title.equals(PLAYERS)) {

            if (slot == 49) {
                openStaffMenu(admin);
                return;
            }

            ItemStack clicked = event.getCurrentItem();

            if (clicked == null ||
                    clicked.getType() != Material.PLAYER_HEAD) {
                return;
            }

            if (!(clicked.getItemMeta() instanceof SkullMeta meta)) {
                return;
            }

            if (meta.getOwningPlayer() == null) return;

            Player target = Bukkit.getPlayer(
                    meta.getOwningPlayer().getUniqueId()
            );

            if (target == null) {

                admin.sendMessage(PREFIX + "Pemain sudah offline.");

                openPlayerManagement(admin);

                return;
            }

            openActions(admin, target);

            return;
        }

        // ACTION MENU

        if (title.equals(ACTIONS)) {

            if (slot == 22) {

                selected.remove(admin.getUniqueId());

                openPlayerManagement(admin);

                return;
            }

            UUID targetId = selected.get(admin.getUniqueId());

            if (targetId == null) return;

            Player target = Bukkit.getPlayer(targetId);

            if (target == null) {

                admin.sendMessage(PREFIX + "Pemain sudah offline.");

                admin.closeInventory();

                return;
            }

            if (!canManage(admin, target)) {

                admin.sendMessage(
                        PREFIX + "Kamu tidak bisa menindak staff setara atau lebih tinggi."
                );

                admin.closeInventory();

                return;
            }

            if (slot == 10) {

                if (!admin.hasPermission("staffjobs.kick")) return;

                openConfirmation(admin, target, "KICK");
            }

            if (slot == 12) {

                if (!admin.hasPermission("staffjobs.ban")) return;

                openConfirmation(admin, target, "BAN");
            }

            if (slot == 14) {

                if (!admin.hasPermission("staffjobs.freeze")) return;

                toggleFreeze(admin, target);

                openActions(admin, target);
            }

            return;
        }

        // CONFIRMATION

        if (title.equals(CONFIRM)) {

            if (slot == 15) {

                pendingAction.remove(admin.getUniqueId());

                UUID id = selected.get(admin.getUniqueId());

                Player target = id == null ? null : Bukkit.getPlayer(id);

                if (target != null) {
                    openActions(admin, target);
                } else {
                    admin.closeInventory();
                }

                return;
            }

            if (slot != 11) return;

            UUID id = selected.get(admin.getUniqueId());

            if (id == null) {
                admin.closeInventory();
                return;
            }

            Player target = Bukkit.getPlayer(id);

            String action = pendingAction.remove(admin.getUniqueId());

            if (target == null || action == null) {

                admin.sendMessage(PREFIX + "Target tidak ditemukan.");

                admin.closeInventory();

                return;
            }

            if (!canManage(admin, target)) {

                admin.sendMessage(PREFIX + "Akses ditolak.");

                admin.closeInventory();

                return;
            }

            if (action.equals("KICK")
                    && admin.hasPermission("staffjobs.kick")) {

                target.kickPlayer(
                        ChatColor.RED + "Kamu dikeluarkan oleh staff."
                );

                admin.sendMessage(
                        PREFIX + target.getName() + " berhasil di-kick."
                );
            }

            if (action.equals("BAN")
                    && admin.hasPermission("staffjobs.ban")) {

                Bukkit.getBanList(BanList.Type.NAME).addBan(
                        target.getName(),
                        "Banned by STAFFJOBS",
                        null,
                        admin.getName()
                );

                target.kickPlayer(
                        ChatColor.RED + "Kamu telah di-ban."
                );

                admin.sendMessage(
                        PREFIX + target.getName() + " berhasil di-ban."
                );
            }

            admin.closeInventory();
        }
    }

    // STAFF HIERARCHY

    private int getRank(Player player) {

        if (player.isOp()) return 100;

        for (int rank = 99; rank >= 1; rank--) {

            if (player.hasPermission("staffjobs.rank." + rank)) {
                return rank;
            }
        }

        return 0;
    }

    private boolean canManage(Player admin, Player target) {

        if (admin.getUniqueId().equals(target.getUniqueId())) {
            return false;
        }

        if (admin.hasPermission("staffjobs.protect.bypass")) {
            return true;
        }

        return getRank(admin) > getRank(target);
    }

    // PERMANENT FREEZE SYSTEM

    private void toggleFreeze(Player admin, Player target) {

        UUID id = target.getUniqueId();

        if (frozen.contains(id)) {

            frozen.remove(id);

            saveFrozen();

            target.sendMessage(
                    PREFIX + "Kamu sudah tidak dibekukan."
            );

            admin.sendMessage(
                    PREFIX + target.getName() + " berhasil di-unfreeze."
            );

        } else {

            frozen.add(id);

            saveFrozen();

            target.sendMessage(
                    PREFIX + "Kamu sedang dibekukan oleh staff."
            );

            admin.sendMessage(
                    PREFIX + target.getName() + " berhasil di-freeze."
            );
        }
    }

    // MOVEMENT BLOCK

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (!frozen.contains(event.getPlayer().getUniqueId())) {
            return;
        }

        if (event.getTo() == null) return;

        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

            event.setTo(event.getFrom());
        }
    }

    // ATTACK BLOCK

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player player
                && frozen.contains(player.getUniqueId())) {

            event.setCancelled(true);
        }
    }

    // REJOIN CHECK

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (frozen.contains(player.getUniqueId())) {

            player.sendMessage(
                    PREFIX + ChatColor.RED +
                    "Status Freeze kamu masih aktif!"
            );
        }
    }

    // GUI UTILITIES

    private void fill(Inventory inv) {

        for (int i = 0; i < inv.getSize(); i++) {

            setItem(
                    inv,
                    i,
                    Material.BLACK_STAINED_GLASS_PANE,
                    " "
            );
        }
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
                    }
