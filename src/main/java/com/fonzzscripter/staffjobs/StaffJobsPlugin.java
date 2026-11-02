package com.fonzzscripter.staffjobs;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StaffJobsPlugin extends JavaPlugin implements Listener {
    private static final String PREFIX = ChatColor.YELLOW + "[ʜᴇʟᴘᴇʀꜱᴛᴀꜰꜰ] ";
    private static final String MENU = "§8STAFFJOBS";
    private static final String PLAYERS = "§8Pilih Pemain";
    private static final String ACTIONS = "§8Kelola Pemain";
    private static final String CONFIRM = "§8Konfirmasi";
    private static final String REPORTS = "§8Daftar Laporan";
    private static final String REPORT_DETAIL = "§8Detail Laporan";

    private final Set<UUID> frozen = new HashSet<>();
    private final Map<UUID, UUID> selected = new HashMap<>();
    private final Map<UUID, String> pendingAction = new HashMap<>();
    private final Map<UUID, Integer> reportPages = new HashMap<>();
    private final Map<UUID, String> selectedReports = new HashMap<>();

    private File frozenFile, reportsFile, notificationsFile;
    private YamlConfiguration frozenData, reportsData, notificationsData;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadFrozen();
        loadReports();
        loadNotifications();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("staffmenu")).setExecutor(this);
        Objects.requireNonNull(getCommand("report")).setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::removeExpiredReports, 20L * 60L, 20L * 60L);
        getLogger().info("STAFFJOBS berhasil diaktifkan!");
    }

    @Override
    public void onDisable() {
        saveFrozen();
        saveReports();
        saveNotifications();
        selected.clear();
        pendingAction.clear();
        reportPages.clear();
        selectedReports.clear();
    }

    // DATA STORAGE

    private void loadFrozen() {
        frozenFile = new File(getDataFolder(), "frozen.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        frozenData = YamlConfiguration.loadConfiguration(frozenFile);
        for (String value : frozenData.getStringList("frozen-players")) {
            try {
                frozen.add(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("UUID freeze tidak valid: " + value);
            }
        }
    }

    private void saveFrozen() {
        if (frozenData == null) return;
        List<String> values = new ArrayList<>();
        for (UUID uuid : frozen) values.add(uuid.toString());
        frozenData.set("frozen-players", values);
        saveFile(frozenData, frozenFile);
    }

    private void loadReports() {
        reportsFile = new File(getDataFolder(), "reports.yml");
        reportsData = YamlConfiguration.loadConfiguration(reportsFile);
        if (!reportsData.contains("reports")) {
            reportsData.set("reports", new ArrayList<>());
            saveReports();
        }
    }

    private void saveReports() {
        if (reportsData != null) saveFile(reportsData, reportsFile);
    }

    private void loadNotifications() {
        notificationsFile = new File(getDataFolder(), "notifications.yml");
        notificationsData = YamlConfiguration.loadConfiguration(notificationsFile);
    }

    private void saveNotifications() {
        if (notificationsData != null) saveFile(notificationsData, notificationsFile);
    }

    private void saveFile(YamlConfiguration data, File file) {
        try {
            data.save(file);
        } catch (IOException exception) {
            getLogger().severe("Gagal menyimpan " + file.getName());
            exception.printStackTrace();
        }
    }

    // COMMANDS

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("staffmenu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Command ini hanya bisa digunakan pemain.");
                return true;
            }
            if (!player.hasPermission("staffjobs.use")) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kamu tidak memiliki izin.");
                return true;
            }
            openStaffMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("report")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Command ini hanya bisa digunakan pemain.");
                return true;
            }
            if (!player.hasPermission("staffjobs.report")) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kamu tidak memiliki izin.");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(PREFIX + ChatColor.RED + "Gunakan: /report <pesan>");
                return true;
            }
            String message = String.join(" ", args).trim();
            if (message.isEmpty()) {
                player.sendMessage(PREFIX + ChatColor.RED + "Pesan laporan tidak boleh kosong.");
                return true;
            }
            submitReport(player, message);
            return true;
        }
        return false;
    }

    // REPORT SYSTEM

    private List<Map<?, ?>> getReports() {
        return reportsData.getMapList("reports");
    }

    private String reportValue(Map<?, ?> report, String key, String fallback) {
        Object value = report.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private void submitReport(Player player, String message) {
        for (Map<?, ?> report : getReports()) {
            if (player.getUniqueId().toString().equals(reportValue(report, "uuid", ""))) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kamu masih memiliki laporan yang belum diselesaikan.");
                return;
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", UUID.randomUUID().toString());
        report.put("uuid", player.getUniqueId().toString());
        report.put("player", player.getName());
        report.put("message", message);
        report.put("timestamp", System.currentTimeMillis());
        report.put("date", LocalDateTime.now().format(dateFormatter));

        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        reports.add(report);
        int max = Math.max(1, getConfig().getInt("settings.max-reports", 100));
        while (reports.size() > max) reports.remove(0);
        reportsData.set("reports", reports);
        saveReports();

        player.sendMessage(PREFIX + ChatColor.GREEN + "Laporan berhasil dikirim kepada STAFF.");
        if (getConfig().getBoolean("settings.reports.notify-staff", true)) {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission("staffjobs.use")) {
                    staff.sendMessage(PREFIX + ChatColor.YELLOW + player.getName() + " mengirim laporan baru!");
                    staff.sendMessage(ChatColor.GRAY + "Gunakan /staffmenu untuk melihat laporan.");
                }
            }
        }
    }

    private void openReportLogs(Player player, int page) {
        List<Map<?, ?>> reports = getReports();
        int maxPages = Math.max(1, (reports.size() + 44) / 45);
        page = Math.max(0, Math.min(page, maxPages - 1));
        reportPages.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(null, 54, REPORTS + " §7(" + (page + 1) + "/" + maxPages + ")");
        int start = page * 45;
        int end = Math.min(start + 45, reports.size());

        for (int i = start; i < end; i++) {
            Map<?, ?> report = reports.get(i);
            inventory.setItem(i - start, createItem(
                    Material.PAPER,
                    ChatColor.YELLOW + reportValue(report, "player", "Unknown"),
                    ChatColor.GRAY + "Waktu: " + reportValue(report, "date", "Unknown"),
                    ChatColor.GRAY + "Laporan: " + reportValue(report, "message", ""),
                    "",
                    ChatColor.GREEN + "Klik untuk melihat detail."
            ));
        }
        inventory.setItem(45, createItem(Material.ARROW, "§eHalaman Sebelumnya"));
        inventory.setItem(49, createItem(Material.BARRIER, "§cKembali"));
        inventory.setItem(53, createItem(Material.ARROW, "§eHalaman Berikutnya"));
        player.openInventory(inventory);
    }

    private void openReportDetail(Player player, String id) {
        Map<?, ?> report = findReport(id);
        if (report == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Laporan tidak ditemukan.");
            openReportLogs(player, 0);
            return;
        }
        selectedReports.put(player.getUniqueId(), id);
        Inventory inventory = Bukkit.createInventory(null, 27, REPORT_DETAIL);
        inventory.setItem(13, createItem(
                Material.PAPER,
                ChatColor.YELLOW + "Laporan dari " + reportValue(report, "player", "Unknown"),
                ChatColor.GRAY + "Waktu: " + reportValue(report, "date", "Unknown"),
                ChatColor.GRAY + "Pesan:",
                ChatColor.WHITE + reportValue(report, "message", "")
        ));
        inventory.setItem(15, createItem(Material.LIME_WOOL, "§aSELESAIKAN LAPORAN", "§7Klik untuk menyelesaikan laporan ini."));
        inventory.setItem(11, createItem(Material.ARROW, "§eKembali ke daftar laporan"));
        player.openInventory(inventory);
    }

    private Map<?, ?> findReport(String id) {
        for (Map<?, ?> report : getReports()) {
            if (id.equals(reportValue(report, "id", ""))) return report;
        }
        return null;
    }

    private void completeReport(Player staff, String id) {
        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        Map<?, ?> target = null;
        for (Map<?, ?> report : reports) {
            if (id.equals(reportValue(report, "id", ""))) {
                target = report;
                break;
            }
        }
        if (target == null) {
            staff.sendMessage(PREFIX + ChatColor.RED + "Laporan tidak ditemukan.");
            return;
        }

        String reporterName = reportValue(target, "player", "Unknown");
        String reporterUUID = reportValue(target, "uuid", "");
        reports.remove(target);
        reportsData.set("reports", reports);
        saveReports();

        String staffMessage = PREFIX + ChatColor.YELLOW + staff.getName()
                + " telah menyelesaikan laporan " + reporterName;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("staffjobs.use")) online.sendMessage(staffMessage);
        }

        String notification = PREFIX + ChatColor.GREEN + "Halo, STAFF telah menyelesaikan laporan anda.\n"
                + PREFIX + ChatColor.YELLOW + "Jika anda tidak merasa dibantu STAFF harap melaporkan kembali.";
        try {
            UUID uuid = UUID.fromString(reporterUUID);
            Player reporter = Bukkit.getPlayer(uuid);
            if (reporter != null && reporter.isOnline()) {
                reporter.sendMessage(notification);
            } else {
                String path = "pending." + reporterUUID;
                List<String> pending = notificationsData.getStringList(path);
                pending.add(notification);
                notificationsData.set(path, pending);
                saveNotifications();
            }
        } catch (IllegalArgumentException exception) {
            getLogger().warning("UUID pelapor tidak valid: " + reporterUUID);
        }

        staff.sendMessage(PREFIX + ChatColor.GREEN + "Laporan " + reporterName + " berhasil diselesaikan.");
        openReportLogs(staff, 0);
    }

    private void removeExpiredReports() {
        long now = System.currentTimeMillis();
        long expiration = 24L * 60L * 60L * 1000L;
        List<Map<?, ?>> reports = new ArrayList<>(getReports());
        boolean changed = false;
        Iterator<Map<?, ?>> iterator = reports.iterator();

        while (iterator.hasNext()) {
            Map<?, ?> report = iterator.next();
            long timestamp;
            try {
                timestamp = Long.parseLong(reportValue(report, "timestamp", "0"));
            } catch (NumberFormatException exception) {
                timestamp = 0;
            }
            if (timestamp > 0 && now - timestamp >= expiration) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            reportsData.set("reports", reports);
            saveReports();
            getLogger().info("Laporan kedaluwarsa berhasil dibersihkan.");
        }
    }

    // OFFLINE NOTIFICATIONS

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (frozen.contains(player.getUniqueId())) {
            player.sendMessage(PREFIX + ChatColor.RED + "Kamu masih dalam status FREEZE oleh STAFF.");
        }

        String path = "pending." + player.getUniqueId();
        List<String> pending = notificationsData.getStringList(path);
        if (!pending.isEmpty()) {
            for (String message : pending) player.sendMessage(message);
            notificationsData.set(path, null);
            saveNotifications();
        }
    }

    // STAFF MENU

    private void openStaffMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, MENU);
        inventory.setItem(10, createItem(Material.PLAYER_HEAD, "§eKelola Pemain"));
        inventory.setItem(13, createItem(Material.BOOK, "§eDaftar Laporan", "§7Total laporan: " + getReports().size()));
        inventory.setItem(31, createItem(Material.BARRIER, "§cTutup Menu"));
        player.openInventory(inventory);
    }

    private void openPlayers(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, PLAYERS);
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            inventory.setItem(slot++, createItem(Material.PLAYER_HEAD, "§e" + target.getName(), "§7Klik untuk mengelola pemain."));
        }
        inventory.setItem(49, createItem(Material.ARROW, "§eKembali"));
        player.openInventory(inventory);
    }

    private void openActions(Player staff, Player target) {
        selected.put(staff.getUniqueId(), target.getUniqueId());
        Inventory inventory = Bukkit.createInventory(null, 27, ACTIONS);
        inventory.setItem(10, createItem(Material.IRON_SWORD, "§cKick Pemain"));
        inventory.setItem(12, createItem(Material.BARRIER, "§4Ban Pemain"));
        inventory.setItem(14, createItem(
                frozen.contains(target.getUniqueId()) ? Material.LIME_WOOL : Material.ICE,
                frozen.contains(target.getUniqueId()) ? "§aUnfreeze Pemain" : "§bFreeze Pemain"
        ));
        inventory.setItem(22, createItem(Material.ARROW, "§eKembali"));
        staff.openInventory(inventory);
    }

    private void openConfirm(Player staff, String action) {
        pendingAction.put(staff.getUniqueId(), action);
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRM);
        inventory.setItem(11, createItem(Material.LIME_WOOL, "§aKONFIRMASI"));
        inventory.setItem(15, createItem(Material.RED_WOOL, "§cBATAL"));
        staff.openInventory(inventory);
    }

    // INVENTORY EVENTS

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (!title.equals(MENU) && !title.startsWith(REPORTS) && !title.equals(REPORT_DETAIL)
                && !title.equals(PLAYERS) && !title.equals(ACTIONS) && !title.equals(CONFIRM)) return;

        event.setCancelled(true);
        if (!player.hasPermission("staffjobs.use")) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (title.equals(MENU)) {
            if (slot == 10) openPlayers(player);
            else if (slot == 13) openReportLogs(player, 0);
            else if (slot == 31) player.closeInventory();
            return;
        }

        if (title.startsWith(REPORTS)) {
            if (slot == 49) {
                openStaffMenu(player);
                return;
            }
            int page = reportPages.getOrDefault(player.getUniqueId(), 0);
            if (slot == 45) {
                openReportLogs(player, page - 1);
                return;
            }
            if (slot == 53) {
                openReportLogs(player, page + 1);
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PAPER) return;
            List<Map<?, ?>> reports = getReports();
            int index = page * 45 + slot;
            if (index >= 0 && index < reports.size()) {
                openReportDetail(player, reportValue(reports.get(index), "id", ""));
            }
            return;
        }

        if (title.equals(REPORT_DETAIL)) {
            if (slot == 11) openReportLogs(player, 0);
            else if (slot == 15) {
                String id = selectedReports.get(player.getUniqueId());
                if (id != null) completeReport(player, id);
            }
            return;
        }

        if (title.equals(PLAYERS)) {
            if (slot == 49) {
                openStaffMenu(player);
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;
            List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (slot < online.size()) {
                Player target = online.get(slot);
                if (target.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Kamu tidak bisa mengelola dirimu sendiri.");
                    return;
                }
                if (!canManage(player, target)) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Rank pemain tersebut terlalu tinggi.");
                    return;
                }
                openActions(player, target);
            }
            return;
        }

        if (title.equals(ACTIONS)) {
            UUID targetUUID = selected.get(player.getUniqueId());
            if (targetUUID == null) return;
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Pemain sudah offline.");
                openPlayers(player);
                return;
            }
            if (slot == 10) openConfirm(player, "kick");
            else if (slot == 12) openConfirm(player, "ban");
            else if (slot == 14) {
                toggleFreeze(target);
                openActions(player, target);
            } else if (slot == 22) openPlayers(player);
            return;
        }

        if (title.equals(CONFIRM)) {
            if (slot == 15) {
                pendingAction.remove(player.getUniqueId());
                openStaffMenu(player);
                return;
            }
            if (slot != 11) return;
            UUID targetUUID = selected.get(player.getUniqueId());
            String action = pendingAction.remove(player.getUniqueId());
            if (targetUUID == null || action == null) return;
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null) {
                player.sendMessage(PREFIX + ChatColor.RED + "Pemain sudah offline.");
                openPlayers(player);
                return;
            }
            if (!canManage(player, target)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Kamu tidak bisa mengelola pemain tersebut.");
                openStaffMenu(player);
                return;
            }
            if (action.equals("kick")) {
                target.kickPlayer(ChatColor.RED + "Kamu dikeluarkan oleh STAFF.");
                player.sendMessage(PREFIX + ChatColor.GREEN + "Pemain berhasil di-kick.");
            } else if (action.equals("ban")) {
                Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), "Dibanned oleh STAFF", null, player.getName());
                target.kickPlayer(ChatColor.RED + "Kamu dibanned oleh STAFF.");
                player.sendMessage(PREFIX + ChatColor.GREEN + "Pemain berhasil di-ban.");
            }
            openStaffMenu(player);
        }
    }

    // FREEZE SYSTEM

    private void toggleFreeze(Player target) {
        UUID uuid = target.getUniqueId();
        if (frozen.contains(uuid)) {
            frozen.remove(uuid);
            target.sendMessage(PREFIX + ChatColor.GREEN + "Status FREEZE kamu telah dicabut.");
        } else {
            frozen.add(uuid);
            target.sendMessage(PREFIX + ChatColor.RED + "Kamu telah di-FREEZE oleh STAFF.");
        }
        saveFrozen();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!frozen.contains(player.getUniqueId()) || event.getTo() == null) return;
        if (getConfig().getBoolean("settings.freeze.prevent-movement", true)) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (frozen.contains(player.getUniqueId())
                && getConfig().getBoolean("settings.freeze.prevent-attack", true)) {
            event.setCancelled(true);
        }
    }

    // STAFF HIERARCHY

    private int getRank(Player player) {
        if (player.isOp()) return 100;
        for (int i = 99; i >= 1; i--) {
            if (player.hasPermission("staffjobs.rank." + i)) return i;
        }
        return 0;
    }

    private boolean canManage(Player staff, Player target) {
        if (staff.getUniqueId().equals(target.getUniqueId())) return false;
        if (staff.hasPermission("staffjobs.protect.bypass")) return true;
        return getRank(staff) > getRank(target);
    }

    // ITEM HELPERS

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
