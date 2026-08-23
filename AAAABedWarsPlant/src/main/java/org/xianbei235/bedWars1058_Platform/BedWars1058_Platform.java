package org.xianbei235.bedWars1058_Platform;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BedWars1058_Platform extends JavaPlugin implements Listener {
    private final Map<String, Long> cooldowns = new HashMap<>();
    private Material rescuePlatformItemType;
    private int cooldownTime;
    private final Map<Location, BlockState> originalBlocks = new HashMap<>();

    @Override
    public void onEnable() {
        if (!pluginlistener()) {
            getLogger().severe("您没有安装前置插件-BedWars1058");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("插件已启用");
    }

    private void loadConfig() {
        String itemMaterialName = getConfig().getString("platform.item.material", "BLAZE_ROD");
        rescuePlatformItemType = Material.getMaterial(itemMaterialName);
        if (rescuePlatformItemType == null) {
            getLogger().warning("配置文件中的物品类型无法使用,请检查物品类型后重启服务器");
            rescuePlatformItemType = Material.BLAZE_ROD;
        }

        String itemName = getConfig().getString("platform.item.name", "§6救援平台");

        cooldownTime = getConfig().getInt("platform.cooldown", 15);

        setItemMetaName(itemName);
    }

    private void setItemMetaName(String name) {
        ItemStack mainHandItem = new ItemStack(rescuePlatformItemType);
        ItemMeta meta = mainHandItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            mainHandItem.setItemMeta(meta);
        }
    }

    private boolean pluginlistener() {
        return Bukkit.getPluginManager().getPlugin("BedWars1058") != null;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == rescuePlatformItemType) {
            Player player = event.getPlayer();
            String playerName = player.getName();
            long currentTime = System.currentTimeMillis();

            if (cooldowns.containsKey(playerName) && (currentTime - cooldowns.get(playerName) < cooldownTime * 1000L)) {
                long remainingTime = cooldownTime * 1000L - (currentTime - cooldowns.get(playerName));
                player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.cooldown")).replace("%time%", String.valueOf(remainingTime / 1000)));
                return;
            }
            createRescuePlatform(player);
            cooldowns.put(playerName, currentTime);
        }
    }

    private void createRescuePlatform(Player player) {
        Location location = player.getLocation();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location blockLocation = location.clone().add(x, -1, z);
                Block block = blockLocation.getBlock();

                originalBlocks.put(blockLocation, block.getState());

                block.setType(Material.SLIME_BLOCK);
            }
        }

        player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.created")));

        Bukkit.getScheduler().runTaskLater(this, () -> removeRescuePlatform(location), 300L);
    }

    private void removeItem(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }
    }

    private void removeRescuePlatform(Location location) {
        for (Location blockLocation : originalBlocks.keySet()) {
            BlockState originalBlockState = originalBlocks.get(blockLocation);
            blockLocation.getBlock().setType(originalBlockState.getType());
        }

        originalBlocks.clear();
        Bukkit.getScheduler().runTask(this, () -> {
            // 移除后消息
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.removed")));
            }
        });
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Location playerLocation = player.getLocation();
            Material blockType = playerLocation.add(0, -1, 0).getBlock().getType();
            if (blockType == Material.SLIME_BLOCK) {
                event.setCancelled(true);
                player.sendMessage(Objects.requireNonNull(getConfig().getString("messages.damage_protection")));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.SLIME_BLOCK) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Objects.requireNonNull(getConfig().getString("messages.block_protection")));
        }
    }
}
