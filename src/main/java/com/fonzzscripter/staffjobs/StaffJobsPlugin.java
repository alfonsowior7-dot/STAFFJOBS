package com.fonzzscripter.staffjobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class StaffJobsPlugin extends JavaPlugin {

    public static final String PREFIX =
            ChatColor.YELLOW + "[ʜᴇʟᴘᴇʀꜱᴛᴀꜰꜰ] ";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Objects.requireNonNull(
                getCommand("staffmenu")
        ).setExecutor(this::handleCommand);

        Objects.requireNonNull(
                getCommand("report")
        ).setExecutor(this::handleCommand);

        getLogger().info("STAFFJOBS by FonzzScripter enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
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
            sender.sendMessage(PREFIX + "Gunakan command dalam game.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("staffmenu")) {

            if (!player.hasPermission("staffjobs.use")) {
                player.sendMessage(PREFIX + "Kamu tidak memiliki izin.");
                return true;
            }

            player.sendMessage(
                    PREFIX + "STAFFJOBS sedang disiapkan."
            );

            return true;
        }

        if (command.getName().equalsIgnoreCase("report")) {

            if (args.length == 0) {
                player.sendMessage(
                        PREFIX + "Gunakan /report <pesan>"
                );
                return true;
            }

            String message = String.join(" ", args);

            player.sendMessage(
                    PREFIX + "Report berhasil diterima: " + message
            );

            return true;
        }

        return true;
    }
}
