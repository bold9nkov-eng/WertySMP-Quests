package ru.voidmain.voidclear;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VoidClear extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final String PREFIX = "&#33204A[&#A855F7VoidClear&#33204A] &#F3E8FF";
    private NamespacedKey deathKey;
    private NamespacedKey damageKey;
    private BukkitTask timerTask;
    private boolean paused;
    private int secondsLeft;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        deathKey = new NamespacedKey(this, "death_protected_until");
        damageKey = new NamespacedKey(this, "last_damage_at");
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand command = getCommand("voidclear");
        if (command == null) {
            getLogger().severe("Command 'voidclear' is missing from plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);

        restoreLoadedDeathDrops();
        resetTimer();
        timerTask = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        getLogger().info("VoidClear 1.0.2 enabled. Paper API 1.21.11; no ads or telemetry.");
    }

    @Override
    public void onDisable() {
        if (timerTask != null) timerTask.cancel();
    }

    private String color(String input) {
        Matcher matcher = HEX.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder legacy = new StringBuilder("§x");
            for (char ch : hex.toCharArray()) legacy.append('§').append(ch);
            matcher.appendReplacement(out, Matcher.quoteReplacement(legacy.toString()));
        }
        matcher.appendTail(out);
        return ChatColor.translateAlternateColorCodes('&', out.toString());
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(color(PREFIX + text));
    }

    private void broadcast(String text) {
        if (!getConfig().getBoolean("settings.broadcast-results", false)) return;
        String message = color(PREFIX + text);
        for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(message);
    }

    private String configured(String path, String fallback) {
        return getConfig().getString(path, fallback);
    }

    private void resetTimer() {
        secondsLeft = Math.max(60, getConfig().getInt("auto-clear.interval-seconds", 600));
    }

    private void tick() {
        if (!getConfig().getBoolean("auto-clear.enabled", true) || paused) return;
        secondsLeft = Math.max(0, secondsLeft - 1);
        List<Integer> warnings = getConfig().getIntegerList("auto-clear.warning-seconds");
        if (warnings.contains(secondsLeft) && secondsLeft > 0) {
            String message = configured("messages.warning", "Очистка через &#C084FC%seconds% сек.")
                    .replace("%seconds%", String.valueOf(secondsLeft));
            for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(color(PREFIX + message));
        }
        if (secondsLeft == 0) {
            int items = getConfig().getBoolean("auto-clear.clear-items", true) ? clearItems(false) : 0;
            int mobs = getConfig().getBoolean("auto-clear.clear-mobs", true) ? clearMobClusters(false) : 0;
            broadcast(configured("messages.auto-result", "Удалено предметов: &#C084FC%items% &#FFFFFF| мобов: &#C084FC%mobs%")
                    .replace("%items%", String.valueOf(items)).replace("%mobs%", String.valueOf(mobs)));
            resetTimer();
        }
    }

    private boolean ignoredWorld(String path, World world) {
        return getConfig().getStringList(path).contains(world.getName());
    }

    private Set<Material> itemWhitelist() {
        Set<Material> values = new HashSet<>();
        for (String name : getConfig().getStringList("items.whitelist")) {
            try {
                values.add(Material.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Unknown material in items.whitelist: " + name);
            }
        }
        return values;
    }

    private boolean isDeathProtected(Item item) {
        Long until = item.getPersistentDataContainer().get(deathKey, PersistentDataType.LONG);
        if (until == null) return false;
        if (until > System.currentTimeMillis()) return true;
        item.getPersistentDataContainer().remove(deathKey);
        if (getConfig().getBoolean("items.prevent-natural-despawn", true)) {
            item.setWillAge(true);
            item.setTicksLived(1);
        }
        return false;
    }

    private void markDeathDrop(Item item) {
        long seconds = Math.max(0L, getConfig().getLong("items.death-drop-protection-seconds", 1800L));
        long until = System.currentTimeMillis() + seconds * 1000L;
        item.getPersistentDataContainer().set(deathKey, PersistentDataType.LONG, until);
        if (getConfig().getBoolean("items.prevent-natural-despawn", true)) item.setWillAge(false);
        new BukkitRunnable() {
            @Override public void run() {
                if (!item.isValid()) return;
                Long saved = item.getPersistentDataContainer().get(deathKey, PersistentDataType.LONG);
                if (saved != null && saved <= System.currentTimeMillis()) {
                    item.getPersistentDataContainer().remove(deathKey);
                    if (getConfig().getBoolean("items.prevent-natural-despawn", true)) {
                        item.setWillAge(true);
                        item.setTicksLived(1);
                    }
                }
            }
        }.runTaskLater(this, Math.max(1L, seconds * 20L));
    }

    private void restoreLoadedDeathDrops() {
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) restoreDeathDrop(item);
        }
    }

    private void restoreDeathDrop(Item item) {
        Long until = item.getPersistentDataContainer().get(deathKey, PersistentDataType.LONG);
        if (until == null) return;
        if (until > System.currentTimeMillis()) {
            if (getConfig().getBoolean("items.prevent-natural-despawn", true)) item.setWillAge(false);
        } else {
            item.getPersistentDataContainer().remove(deathKey);
            if (getConfig().getBoolean("items.prevent-natural-despawn", true)) {
                item.setWillAge(true);
                item.setTicksLived(1);
            }
        }
    }

    private int clearItems(boolean force) {
        if (!getConfig().getBoolean("items.enabled", true)) return 0;
        int removed = 0;
        long minAge = Math.max(0L, getConfig().getLong("items.minimum-age-seconds", 180L)) * 20L;
        Set<Material> whitelist = itemWhitelist();
        for (World world : Bukkit.getWorlds()) {
            if (ignoredWorld("items.ignored-worlds", world)) continue;
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (!item.isValid() || item.isDead() || isDeathProtected(item)) continue;
                ItemStack stack = item.getItemStack();
                Material type = stack.getType();
                if (type.isAir() || whitelist.contains(type) || type.name().endsWith("_SHULKER_BOX")) continue;
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    if (getConfig().getBoolean("items.protect-named-items", true)
                            && (meta.hasDisplayName() || meta.hasItemName())) continue;
                    if (getConfig().getBoolean("items.protect-enchanted-items", true) && meta.hasEnchants()) continue;
                }
                if (!force && item.getTicksLived() < minAge) continue;
                item.remove();
                removed++;
            }
        }
        return removed;
    }

    private boolean protectedMob(Monster mob, boolean force) {
        if (mob instanceof EnderDragon || mob instanceof Wither || mob instanceof Warden
                || mob instanceof ElderGuardian || mob instanceof Ravager) return true;
        if (getConfig().getBoolean("mobs.skip-named-mobs", true)
                && (mob.getCustomName() != null || mob.isCustomNameVisible())) return true;
        if (getConfig().getBoolean("mobs.skip-non-despawning-mobs", true) && !mob.getRemoveWhenFarAway()) return true;
        if (getConfig().getBoolean("mobs.skip-targeting-mobs", true) && mob.getTarget() != null) return true;
        long minAge = Math.max(0L, getConfig().getLong("mobs.minimum-age-seconds", 120L)) * 20L;
        if (!force && mob.getTicksLived() < minAge) return true;
        Long damageAt = mob.getPersistentDataContainer().get(damageKey, PersistentDataType.LONG);
        long damageWindow = Math.max(0L, getConfig().getLong("mobs.recent-damage-protection-seconds", 30L)) * 1000L;
        return damageAt != null && System.currentTimeMillis() - damageAt < damageWindow;
    }

    private boolean clusterNearPlayer(Collection<Monster> cluster, double radius) {
        if (radius <= 0) return false;
        for (Monster mob : cluster) {
            if (!mob.getWorld().getNearbyEntities(mob.getLocation(), radius, radius, radius,
                    entity -> entity instanceof Player).isEmpty()) return true;
        }
        return false;
    }

    private int clearMobClusters(boolean force) {
        if (!getConfig().getBoolean("mobs.enabled", true)) return 0;
        int threshold = Math.max(1, getConfig().getInt("mobs.threshold", 15));
        int percent = Math.max(1, Math.min(80, getConfig().getInt("mobs.remove-percent", 40)));
        int minRemaining = Math.max(1, getConfig().getInt("mobs.min-remaining", 10));
        double radius = Math.max(3.0, getConfig().getDouble("mobs.cluster-radius", 12.0));
        double playerRadius = Math.max(0.0, getConfig().getDouble("mobs.player-protection-radius", 28.0));
        Set<UUID> processed = new HashSet<>();
        int removed = 0;

        for (World world : Bukkit.getWorlds()) {
            if (ignoredWorld("mobs.ignored-worlds", world)) continue;
            for (Monster anchor : world.getEntitiesByClass(Monster.class)) {
                if (!anchor.isValid() || anchor.isDead() || processed.contains(anchor.getUniqueId())) continue;
                List<Monster> cluster = new ArrayList<>();
                for (org.bukkit.entity.Entity entity : world.getNearbyEntities(anchor.getLocation(), radius, radius, radius,
                        candidate -> candidate instanceof Monster)) {
                    Monster mob = (Monster) entity;
                    if (mob.isValid() && !mob.isDead()) cluster.add(mob);
                }
                if (cluster.isEmpty()) continue;
                for (Monster mob : cluster) processed.add(mob.getUniqueId());
                if (cluster.size() <= threshold || clusterNearPlayer(cluster, playerRadius)) continue;

                List<Monster> eligible = new ArrayList<>();
                for (Monster mob : cluster) if (!protectedMob(mob, force)) eligible.add(mob);
                int allowed = Math.min((int) Math.floor(cluster.size() * percent / 100.0),
                        Math.max(0, cluster.size() - minRemaining));
                int count = Math.min(allowed, eligible.size());
                Collections.shuffle(eligible);
                for (int i = 0; i < count; i++) {
                    eligible.get(i).remove(); // remove without drops, XP or kill credit
                    removed++;
                }
            }
        }
        return removed;
    }

    private int countItems() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) count += world.getEntitiesByClass(Item.class).size();
        return count;
    }

    private int countMobs() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) count += world.getEntitiesByClass(Monster.class).size();
        return count;
    }

    private int countProtectedDrops() {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) if (isDeathProtected(item)) count++;
        }
        return count;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!getConfig().getBoolean("items.protect-death-drops", true) || event.getDrops().isEmpty()) return;
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack stack : event.getDrops()) drops.add(stack.clone());
        Location location = event.getEntity().getLocation().clone();
        new BukkitRunnable() {
            @Override public void run() {
                Map<Material, Integer> remaining = new EnumMap<>(Material.class);
                for (ItemStack stack : drops) {
                    if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0)
                        remaining.merge(stack.getType(), stack.getAmount(), Integer::sum);
                }
                for (org.bukkit.entity.Entity entity : location.getWorld().getNearbyEntities(location, 6.0, 5.0, 6.0,
                        candidate -> candidate instanceof Item)) {
                    Item item = (Item) entity;
                    if (item.getTicksLived() > 5) continue;
                    Material type = item.getItemStack().getType();
                    int left = remaining.getOrDefault(type, 0);
                    if (left <= 0) continue;
                    markDeathDrop(item);
                    remaining.put(type, Math.max(0, left - item.getItemStack().getAmount()));
                }
            }
        }.runTaskLater(this, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMonsterDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Monster mob)
            mob.getPersistentDataContainer().set(damageKey, PersistentDataType.LONG, System.currentTimeMillis());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities())
            if (entity instanceof Item item) restoreDeathDrop(item);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("voidclear.admin")) {
            msg(sender, "У тебя нет прав на эту команду.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            msg(sender, "Команды: &#C084FCclear items|mobs|all [force]&#FFFFFF, &#C084FCstatus&#FFFFFF, &#C084FCscan&#FFFFFF, &#C084FCpause&#FFFFFF, &#C084FCresume&#FFFFFF, &#C084FCreload");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                if (args.length < 2) { msg(sender, "Использование: /voidclear clear items|mobs|all [force]"); return true; }
                boolean force = args.length >= 3 && args[2].equalsIgnoreCase("force");
                String kind = args[1].toLowerCase(Locale.ROOT);
                if (kind.equals("items") || kind.equals("all")) msg(sender, "Удалено предметов: &#C084FC" + clearItems(force));
                if (kind.equals("mobs") || kind.equals("all")) msg(sender, "Удалено мобов: &#C084FC" + clearMobClusters(force));
                if (!kind.equals("items") && !kind.equals("mobs") && !kind.equals("all")) msg(sender, "Неизвестный тип очистки.");
            }
            case "status" -> {
                msg(sender, "Автоочистка: " + (getConfig().getBoolean("auto-clear.enabled", true) && !paused ? "&#A78BFAработает" : "&#F3E8FFпауза/отключена"));
                msg(sender, "Предметы: " + (getConfig().getBoolean("items.enabled", true) ? "&#A78BFAвключены" : "&#F3E8FFвыключены")
                        + " | защита дропа смерти: " + (getConfig().getBoolean("items.protect-death-drops", true) ? "&#A78BFAвключена" : "&#F3E8FFвыключена"));
                msg(sender, "Мобы: " + (getConfig().getBoolean("mobs.enabled", true) ? "&#A78BFAвключены" : "&#F3E8FFвыключены")
                        + " | возраст предметов: " + getConfig().getInt("items.minimum-age-seconds", 180) + " сек.");
            }
            case "scan" -> msg(sender, "Миры: &#C084FC" + Bukkit.getWorlds().size() + " &#FFFFFF| предметы: &#C084FC" + countItems()
                    + " &#FFFFFF| враждебные мобы: &#C084FC" + countMobs() + " &#FFFFFF| защищённые дропы: &#C084FC" + countProtectedDrops());
            case "pause" -> { paused = true; msg(sender, "Автоочистка приостановлена."); }
            case "resume" -> { paused = false; msg(sender, "Автоочистка возобновлена."); }
            case "reload" -> { reloadConfig(); resetTimer(); paused = false; msg(sender, "Настройки перезагружены."); }
            default -> msg(sender, "Неизвестная подкоманда. Используй /voidclear help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("voidclear.admin")) return Collections.emptyList();
        if (args.length == 1) return filter(List.of("help", "clear", "status", "scan", "pause", "resume", "reload"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("clear")) return filter(List.of("items", "mobs", "all"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("clear")) return filter(List.of("force"), args[2]);
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(lower)).toList();
    }
}