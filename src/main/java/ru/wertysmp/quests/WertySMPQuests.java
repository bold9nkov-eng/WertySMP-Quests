package ru.wertysmp.quests;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

public final class WertySMPQuests extends JavaPlugin implements Listener {
 private record Q(String id,String name,String desc,Material icon,int target,int reward,String type){}
 private final List<Q> all=new ArrayList<>();
 private final Map<UUID,Map<String,Integer>> prog=new HashMap<>();
 private final Map<UUID,Set<String>> claimed=new HashMap<>(), pvp=new HashMap<>();
 private final Map<UUID,Set<String>> seen=new HashMap<>();
 private final Map<UUID,Map<String,Set<String>>> sets=new HashMap<>();
 private List<String> weekly=new ArrayList<>(); private String week=""; private YamlConfiguration db; private File file; private NamespacedKey key;
 public void onEnable(){getDataFolder().mkdirs();file=new File(getDataFolder(),"players.yml");db=YamlConfiguration.loadConfiguration(file);key=new NamespacedKey(this,"quest");load();catalog();rotate();getServer().getPluginManager().registerEvents(this,this);Objects.requireNonNull(getCommand("wquests")).setExecutor(this);getLogger().info("WertySMP-Quests: weekly quests loaded");}
 public void onDisable(){save();}
 private void add(String i,String n,String d,Material m,int t,int r,String type){all.add(new Q(i,n,d,m,t,r,type));}
 private void catalog(){
 add("pick","Сломать алмазную кирку","Сломай её при использовании",Material.DIAMOND_PICKAXE,1,2,"pick");
 add("dragon","Убить Дракона Края","Убей дракона; возрождение не требуется",Material.DRAGON_HEAD,1,5,"dragon");
 add("netherite","Скрафтить незеритовую броню","Скрафти предмет незеритовой брони",Material.NETHERITE_CHESTPLATE,1,2,"craft");
 add("shovel","Сломать алмазную лопату","Сломай её при использовании",Material.DIAMOND_SHOVEL,1,2,"shovel");
 add("mobs","Убить зомби и скелетов","100 зомби + 70 скелетов",Material.ZOMBIE_HEAD,170,5,"mobs");
 add("fish","Поймать 50 рыб","Только улов удочкой, сундуки не считаются",Material.FISHING_ROD,50,2,"fish");
 add("pvp","Победить 5 разных игроков","Каждый игрок засчитывается один раз",Material.IRON_SWORD,5,2,"pvp");
 add("loot","Собрать гнилую плоть и паучьи глаза","64 гнилой плоти + 64 паучьих глаза",Material.ROTTEN_FLESH,128,5,"loot");
 add("warden","Убить Хранителя","Победи Warden",Material.SCULK_CATALYST,1,5,"warden");
 add("debris","Добыть древний обломок","В аду киркой",Material.ANCIENT_DEBRIS,1,2,"debris");
 add("logs","Срубить 500 брёвен","Любой тип брёвен",Material.OAK_LOG,500,5,"logs");
 add("ores","Добыть руды","64 железной + 32 угольной руды",Material.RAW_IRON,96,2,"ores");
 add("cooked","Приготовить мясо","32 жареной свинины + 32 курицы",Material.COOKED_PORKCHOP,64,2,"cooked");
 add("chests","Открыть сундуки структур","5 сгенерированных сундуков с лутом",Material.CHEST,5,2,"chests");
 add("potions","Сварить 3 зелья","Используй варочную стойку",Material.BREWING_STAND,3,2,"potions");
 add("animals","Убить животных мечом","50 коров, 50 свиней, 50 овец",Material.IRON_SWORD,150,5,"animals");
 add("wither","Убить Визера в аду","Призови и убей его в Нижнем мире",Material.NETHER_STAR,1,5,"wither");
 add("shulkers","Убить 25 шалкеров","В Энде",Material.SHULKER_SHELL,25,2,"shulkers");
 add("phantoms","Убить 50 фантомов","Победи фантомов",Material.PHANTOM_MEMBRANE,50,2,"phantoms");
 add("night","150 мобов за ночь","Убийства за одну ночь",Material.ZOMBIE_HEAD,150,5,"night");
 add("glass","Переплавить 64 песка","Забери стекло из печи",Material.GLASS,64,1,"glass");
 add("mushrooms","Собрать грибы","32 красных + 32 коричневых",Material.RED_MUSHROOM,64,2,"mushrooms");
 add("bow","Убить 35 скелетов из лука","Последний удар стрелой",Material.BOW,35,2,"bow");
 add("endermen","Убить 20 эндерменов","В Энде, без тыквы на голове",Material.ENDER_PEARL,20,2,"endermen");
 add("creepers","Убить 25 криперов","Не дать им взорваться",Material.GUNPOWDER,25,2,"creepers");
 add("drowned","Убить 10 утопленников","Последний удар трезубцем",Material.TRIDENT,10,2,"drowned");
 add("leather","Скрафтить кожаный сет","Шлем, нагрудник, штаны, ботинки",Material.LEATHER_CHESTPLATE,4,2,"leather");
 add("crops","Собрать урожай","32 пшеницы, моркови и картофеля",Material.WHEAT,96,2,"crops");
 add("iron","Скрафтить железное оружие","Железный меч и топор",Material.IRON_AXE,2,1,"iron");
 add("diamonds","Добыть 32 алмаза/руды","Только киркой, без TNT",Material.DIAMOND,32,2,"diamonds");
 add("cure","Вылечить зомби-жителя","Слабость + золотое яблоко",Material.GOLDEN_APPLE,1,5,"cure");
 add("archaeology","Найти черепок кистью","Подозрительный песок + кисть",Material.BRUSH,1,2,"archaeology");
 add("pets","Приручить трёх животных","Волк, кошка и попугай должны быть рядом",Material.BONE,3,2,"pets");
 add("honor","Честная PvP-победа","Каменный/деревянный меч против алмазной/незеритовой брони",Material.STONE_SWORD,1,10,"honor");
 }
 private void rotate(){String now=LocalDate.now(ZoneId.of("Europe/Moscow")).with(DayOfWeek.MONDAY).toString();if(now.equals(week)&&!weekly.isEmpty())return;week=now;weekly.clear();List<Q> pool=new ArrayList<>(all);Collections.shuffle(pool);int sum=0;for(Q q:pool)if(weekly.size()<12&&sum+q.reward<=45){weekly.add(q.id);sum+=q.reward;}for(Q q:pool)if(sum<40&&!weekly.contains(q.id)&&sum+q.reward<=50){weekly.add(q.id);sum+=q.reward;}save();}
 private Q q(String id){return all.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
 private void load(){week=db.getString("week","");weekly=db.getStringList("weekly");for(String k:db.getKeys(false))if(k.startsWith("u_")){UUID id;try{id=UUID.fromString(k.substring(2));}catch(Exception e){continue;}Map<String,Integer> p=new HashMap<>();if(db.getConfigurationSection(k+".progress")!=null)for(String s:db.getConfigurationSection(k+".progress").getKeys(false))p.put(s,db.getInt(k+".progress."+s));prog.put(id,p);claimed.put(id,new HashSet<>(db.getStringList(k+".claimed")));pvp.put(id,new HashSet<>(db.getStringList(k+".pvp")));}}
 private void save(){if(db==null)return;db.set("week",week);db.set("weekly",weekly);Set<UUID> ids=new HashSet<>(prog.keySet());ids.addAll(claimed.keySet());for(UUID id:ids){String k="u_"+id;db.set(k+".progress",prog.getOrDefault(id,Map.of()));db.set(k+".claimed",new ArrayList<>(claimed.getOrDefault(id,Set.of())));db.set(k+".pvp",new ArrayList<>(pvp.getOrDefault(id,Set.of())));}try{db.save(file);}catch(Exception e){getLogger().warning(e.getMessage());}}
 private void inc(Player p,String id,int n){if(!weekly.contains(id)||n<=0)return;Q q=q(id);if(q==null)return;Map<String,Integer> m=prog.computeIfAbsent(p.getUniqueId(),z->new HashMap<>());m.put(id,Math.min(q.target,m.getOrDefault(id,0)+n));}
 public boolean onCommand(CommandSender s,Command c,String label,String[] args){if(!(s instanceof Player p)){s.sendMessage("Только в игре.");return true;}rotate();Inventory inv=Bukkit.createInventory(null,54,"§5§lWertyCoin §8• §dКвесты");int slot=0,total=0;for(String id:weekly){Q q=q(id);if(q==null||slot>=45)continue;int n=prog.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(id,0);boolean done=n>=q.target,got=claimed.getOrDefault(p.getUniqueId(),Set.of()).contains(id);total+=q.reward;ItemStack it=new ItemStack(got?Material.LIME_STAINED_GLASS_PANE:done?Material.EMERALD:q.icon);ItemMeta im=it.getItemMeta();im.setDisplayName((got?"§a✓ ":done?"§e★ ":"§d")+q.name);im.setLore(List.of("§7"+q.desc,"§7Прогресс: §f"+n+"§7/§f"+q.target,"§d["+"▌".repeat(Math.min(10,n*10/q.target))+"§8"+"▌".repeat(10-Math.min(10,n*10/q.target))+"§d]","§7Награда: §d"+q.reward+" WertyCoin",got?"§aПолучено":done?"§eНажми, чтобы забрать":"§cВыполни задание"));im.getPersistentDataContainer().set(key,PersistentDataType.STRING,id);it.setItemMeta(im);inv.setItem(slot++,it);}ItemStack info=new ItemStack(Material.NETHER_STAR);ItemMeta im=info.getItemMeta();im.setDisplayName("§d§lЕженедельные задания");im.setLore(List.of("§7Неделя: §f"+week,"§7Всего заданий: §f"+weekly.size(),"§7Бюджет наград: §d"+total+" WertyCoin","§7Нажми на выполненное задание, чтобы забрать награду."));info.setItemMeta(im);inv.setItem(49,info);p.openInventory(inv);return true;}
 @EventHandler public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p)||!e.getView().getTitle().contains("Квесты"))return;e.setCancelled(true);ItemStack it=e.getCurrentItem();if(it==null||!it.hasItemMeta())return;String id=it.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING);if(id==null)return;Q q=q(id);if(q==null)return;int n=prog.getOrDefault(p.getUniqueId(),Map.of()).getOrDefault(id,0);if(n<q.target){p.sendMessage("§cКвест не выполнен.");return;}Set<String> cs=claimed.computeIfAbsent(p.getUniqueId(),z->new HashSet<>());if(cs.contains(id)){p.sendMessage("§cНаграда уже получена.");return;}if(!points(p,q.reward)){p.sendMessage("§cPlayerPoints не смог начислить WertyCoin.");return;}cs.add(id);save();p.sendMessage("§aПолучено §d"+q.reward+" WertyCoin§a!");p.closeInventory();}
 private boolean points(Player p,int amount){try{Plugin pl=Bukkit.getPluginManager().getPlugin("PlayerPoints");if(pl==null)return false;Object api=pl.getClass().getMethod("getAPI").invoke(pl);for(Method m:api.getClass().getMethods())if(m.getName().equals("give")&&m.getParameterCount()==2&&m.getParameterTypes()[0]==UUID.class&&m.getParameterTypes()[1]==int.class){Object result=m.invoke(api,p.getUniqueId(),amount);return !(result instanceof Boolean b)||b;}return false;}catch(Exception e){getLogger().warning("PlayerPoints: "+e.getMessage());return false;}}
 @EventHandler public void breakBlock(BlockBreakEvent e){Player p=e.getPlayer();Material m=e.getBlock().getType();String hand=p.getInventory().getItemInMainHand().getType().name();if(m.name().endsWith("_LOG"))inc(p,"logs",1);if(m==Material.ANCIENT_DEBRIS&&p.getWorld().getEnvironment()==World.Environment.NETHER&&hand.endsWith("PICKAXE"))inc(p,"debris",1);if(m.name().contains("IRON_ORE")||m.name().contains("COAL_ORE"))inc(p,"ores",1);if(m.name().contains("DIAMOND_ORE")&&hand.endsWith("PICKAXE"))inc(p,"diamonds",1);if(m==Material.RED_MUSHROOM||m==Material.BROWN_MUSHROOM)inc(p,"mushrooms",1);save();}
 @EventHandler public void fish(PlayerFishEvent e){if(e.getState()==PlayerFishEvent.State.CAUGHT_FISH&&e.getCaught() instanceof Item i){inc(e.getPlayer(),"fish",1);Material m=i.getItemStack().getType();if(m==Material.COD||m==Material.SALMON||m==Material.TROPICAL_FISH||m==Material.PUFFERFISH)save();}}
 @EventHandler public void death(EntityDeathEvent e){Player p=e.getEntity().getKiller();if(p==null)return;EntityType t=e.getEntityType();switch(t){case ZOMBIE->inc(p,"mobs",1);case SKELETON->{inc(p,"mobs",1);if(p.getInventory().getItemInMainHand().getType()==Material.BOW)inc(p,"bow",1);}case ENDER_DRAGON->inc(p,"dragon",1);case WARDEN->inc(p,"warden",1);case SHULKER->inc(p,"shulkers",1);case PHANTOM->inc(p,"phantoms",1);case CREEPER->inc(p,"creepers",1);case DROWNED->{if(p.getInventory().getItemInMainHand().getType()==Material.TRIDENT)inc(p,"drowned",1);}case ENDERMAN->{if(p.getWorld().getEnvironment()==World.Environment.THE_END&&p.getInventory().getHelmet().getType()!=Material.CARVED_PUMPKIN)inc(p,"endermen",1);}case COW,PIG,SHEEP->{if(p.getInventory().getItemInMainHand().getType().name().endsWith("_SWORD"))inc(p,"animals",1);}case WITHER->{if(p.getWorld().getEnvironment()==World.Environment.NETHER)inc(p,"wither",1);}default->{}}if(e.getEntity() instanceof Player victim){Set<String> s=pvp.computeIfAbsent(p.getUniqueId(),z->new HashSet<>());if(s.add(victim.getUniqueId().toString()))inc(p,"pvp",1);}save();}

 @EventHandler public void collectItems(InventoryClickEvent e) {
   if (!(e.getWhoClicked() instanceof Player p) || e.getView().getTitle().contains("Квесты")) return;
   ItemStack item=e.getCurrentItem(); if(item==null||item.getType().isAir()) return;
   // Count only real pickup/movement into the player's inventory, not clicks in quest GUI.
   if(e.getClickedInventory()!=null && e.getClickedInventory().equals(p.getInventory()) &&
      (e.getAction()==InventoryAction.PICKUP_ALL || e.getAction()==InventoryAction.PICKUP_HALF ||
       e.getAction()==InventoryAction.PICKUP_ONE || e.getAction()==InventoryAction.PICKUP_SOME)) {
      Material m=item.getType();
      if(m==Material.ROTTEN_FLESH) inc(p,"loot",item.getAmount());
      if(m==Material.SPIDER_EYE) inc(p,"loot",item.getAmount());
      if(m==Material.RED_MUSHROOM||m==Material.BROWN_MUSHROOM) inc(p,"mushrooms",item.getAmount());
      if(m==Material.WHEAT||m==Material.CARROT||m==Material.POTATO) inc(p,"crops",item.getAmount());
      save();
   }
 }
 @EventHandler public void craftQuest(CraftItemEvent e) {
   if(!(e.getWhoClicked() instanceof Player p)) return;
   Material m=e.getCurrentItem().getType();
   if(m.name().startsWith("NETHERITE_") && (m.name().endsWith("_HELMET")||m.name().endsWith("_CHESTPLATE")||m.name().endsWith("_LEGGINGS")||m.name().endsWith("_BOOTS"))) inc(p,"netherite",1);
   if(m==Material.IRON_SWORD||m==Material.IRON_AXE){Set<String> s=seen.computeIfAbsent(p.getUniqueId(),k->new HashSet<>());s.add(m.name());if(s.contains("IRON_SWORD")&&s.contains("IRON_AXE"))inc(p,"iron",2);}
   if(m==Material.LEATHER_HELMET||m==Material.LEATHER_CHESTPLATE||m==Material.LEATHER_LEGGINGS||m==Material.LEATHER_BOOTS){Set<String>s=seen.computeIfAbsent(p.getUniqueId(),k->new HashSet<>());s.add(m.name());if(s.contains("LEATHER_HELMET")&&s.contains("LEATHER_CHESTPLATE")&&s.contains("LEATHER_LEGGINGS")&&s.contains("LEATHER_BOOTS"))inc(p,"leather",4);}
   save();
 }
 @EventHandler public void brew(org.bukkit.event.inventory.BrewEvent e) {
   for(HumanEntity h:e.getContents().getViewers()) if(h instanceof Player p) { inc(p,"potions",3); save(); }
 }
 @EventHandler public void chestOpen(InventoryOpenEvent e) {
   if(!(e.getPlayer() instanceof Player p)) return;
   InventoryHolder holder=e.getInventory().getHolder();
   if(!(holder instanceof org.bukkit.loot.Lootable loot) || loot.getLootTable()==null) return;
   if(!(holder instanceof org.bukkit.block.Container c)) return;
   String k=c.getWorld().getName()+":"+c.getX()+":"+c.getY()+":"+c.getZ();
   Set<String>s=seen.computeIfAbsent(p.getUniqueId(),z->new HashSet<>());
   if(s.add("chest:"+k)){inc(p,"chests",1);save();}
 }
 @EventHandler public void furnace(org.bukkit.event.inventory.FurnaceExtractEvent e) {
   Player p=e.getPlayer(); if(e.getItemType()==Material.GLASS) inc(p,"glass",e.getItemAmount());
   if(e.getItemType()==Material.COOKED_PORKCHOP || e.getItemType()==Material.COOKED_CHICKEN) inc(p,"cooked",e.getItemAmount());
   save();
 }
 @EventHandler public void harvest(BlockBreakEvent e) {
   Material m=e.getBlock().getType();
   if(m==Material.WHEAT && e.getBlock().getBlockData() instanceof org.bukkit.block.data.Ageable a && a.getAge()==a.getMaximumAge()) inc(e.getPlayer(),"crops",1);
   if(m==Material.CARROTS || m==Material.POTATOES) inc(e.getPlayer(),"crops",1);
   if(m==Material.SAND) inc(e.getPlayer(),"glass",1);
 }
 @EventHandler public void tame(org.bukkit.event.entity.EntityTameEvent e) {
   if(!(e.getOwner() instanceof Player p)) return;
   EntityType t=e.getEntityType(); if(t==EntityType.WOLF||t==EntityType.CAT||t==EntityType.PARROT) {
     Set<String>s=seen.computeIfAbsent(p.getUniqueId(),z->new HashSet<>());s.add("pet:"+t.name());
     if(s.contains("pet:WOLF")&&s.contains("pet:CAT")&&s.contains("pet:PARROT")) inc(p,"pets",3);
     save();
   }
 }

}
