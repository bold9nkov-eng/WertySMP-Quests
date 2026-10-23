
package ru.wertysmp.quests;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class WertySMPQuests extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("WertySMP-Quests включён!");
        getLogger().info("Следующим этапом подключим квесты и PlayerPoints.");
    }

    @Override
    public void onDisable() {
        getLogger().info("WertySMP-Quests выключен.");
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("wquests")) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду можно использовать только в игре.");
            return true;
        }

        player.sendMessage(ChatColor.DARK_PURPLE + "§lWertySMP §dКвесты");
        player.sendMessage(ChatColor.GRAY + "Система еженедельных квестов готовится.");
        player.sendMessage(ChatColor.YELLOW + "Следующее обновление добавит задания и награды WertyCoin.");
        return true;
    }
}
