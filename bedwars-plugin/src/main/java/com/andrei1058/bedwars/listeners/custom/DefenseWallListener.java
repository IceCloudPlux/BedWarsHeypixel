package com.andrei1058.bedwars.listeners.custom;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 防御墙监听器
 *
 * 功能：
 * 1. 玩家右键红砖方块放置防御墙
 * 2. 在放置位置正前方一格生成3x5的砂岩墙壁
 * 3. 支持斜向45°放置
 * 4. 三级遮挡判定：通透/微扰/壅塞
 * 5. 生成的方块注册为玩家放置，可被挖掘
 * 6. 无视出生点保护、资源点保护等
 */
public class DefenseWallListener implements Listener {

    // 道具图标：红砖方块
    private static final Material WALL_ITEM_TYPE;

    static {
        Material brick = Material.matchMaterial("BRICK");
        if (brick == null) {
            brick = Material.matchMaterial("BRICKS");
        }
        WALL_ITEM_TYPE = brick;
    }

    // 墙壁方块材质：砂岩
    private static final Material WALL_BLOCK_TYPE;

    static {
        Material sandstone = Material.matchMaterial("SANDSTONE");
        if (sandstone == null) {
            sandstone = Material.matchMaterial("SAND");
        }
        WALL_BLOCK_TYPE = sandstone;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();

        if (item == null || item.getType() != WALL_ITEM_TYPE) return;

        IArena arena = Arena.getArenaByPlayer(player);
        if (arena == null || !arena.isPlayer(player)) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // 获取放置位置
        BlockFace face = event.getBlockFace();
        Block placeBlock = clickedBlock.getRelative(face);

        // 取消默认放置事件
        event.setCancelled(true);

        // 获取玩家朝向（支持8方向，含斜向）
        int[] facing = getFacingVector(player);

        // 生成墙壁
        generateWall(placeBlock.getLocation(), facing, arena);

        // 消耗物品（无提示消息）
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInHand(null);
        }
        player.updateInventory();
    }

    /**
     * 生成防御墙
     *
     * @param placementPos 放置位置 X
     * @param facing 前向量 F (x, z)，已归一化
     * @param arena 当前竞技场
     */
    private void generateWall(Location placementPos, int[] facing, IArena arena) {
        // 前向量 F
        int fx = facing[0];
        int fz = facing[1];

        // 右向量 R = cross(F, U)，U=(0,1,0)
        // cross((fx,0,fz), (0,1,0)) = (0*fz - 0*1, 0*0 - fx*0, fx*1 - 0*0) = ... 简化
        // 实际上 R = (fz, 0, -fx)
        int rx = fz;
        int rz = -fx;

        // 第一次尝试：在 X+F 处生成
        Location center = placementPos.clone().add(fx, 0, fz);
        List<Location> candidates = calculateWallLocations(center, rx, rz);

        int blockedCount = countBlocked(candidates);

        if (blockedCount >= 6) {
            // 遮挡过多，后撤到 X-F
            center = placementPos.clone().add(-fx, 0, -fz);
            candidates = calculateWallLocations(center, rx, rz);
            blockedCount = countBlocked(candidates);

            if (blockedCount >= 6) {
                // 后方也壅塞，向上递推一层：X-F+U
                center = placementPos.clone().add(-fx, 1, -fz);
                candidates = calculateWallLocations(center, rx, rz);
            }
        }

        // 执行放置（仅填补空位）
        for (Location loc : candidates) {
            Block b = loc.getBlock();
            if (b.getType() == Material.AIR) {
                b.setType(WALL_BLOCK_TYPE);
                // 注册为玩家放置的方块，使其可被挖掘
                arena.addPlacedBlock(b);
            }
        }
    }

    /**
     * 计算墙壁方块位置
     * 3层高（向上1~3格），5格宽（左右各2格+中心）
     *
     * @param center 基准面中心（X+F位置）
     * @param rx 右向量x分量
     * @param rz 右向量z分量
     * @return 15个候选位置
     */
    private List<Location> calculateWallLocations(Location center, int rx, int rz) {
        List<Location> locations = new ArrayList<>(15);

        for (int i = 1; i <= 3; i++) {
            // 向上1~3层
            for (int j = -2; j <= 2; j++) {
                // 左右各延伸2格
                locations.add(center.clone().add(
                        j * rx,
                        i,
                        j * rz
                ));
            }
        }

        return locations;
    }

    /**
     * 统计被固体方块占据的位置数量
     */
    private int countBlocked(List<Location> locations) {
        int count = 0;
        for (Location loc : locations) {
            Block b = loc.getBlock();
            if (b.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取玩家朝向向量（8方向，含斜向）
     * 返回归一化后的 (x, z) 向量
     *
     * 北 = (0, -1), 南 = (0, 1), 东 = (1, 0), 西 = (-1, 0)
     * 东北 = (1, -1), 西北 = (-1, -1), 东南 = (1, 1), 西南 = (-1, 1)
     */
    private int[] getFacingVector(Player player) {
        double rotation = (player.getLocation().getYaw() - 90) % 360;
        if (rotation < 0) {
            rotation += 360;
        }

        // 8方向判定（每45度一个区间）
        if (0 <= rotation && rotation < 22.5) {
            return new int[]{0, -1}; // 北
        } else if (22.5 <= rotation && rotation < 67.5) {
            return new int[]{1, -1}; // 东北
        } else if (67.5 <= rotation && rotation < 112.5) {
            return new int[]{1, 0}; // 东
        } else if (112.5 <= rotation && rotation < 157.5) {
            return new int[]{1, 1}; // 东南
        } else if (157.5 <= rotation && rotation < 202.5) {
            return new int[]{0, 1}; // 南
        } else if (202.5 <= rotation && rotation < 247.5) {
            return new int[]{-1, 1}; // 西南
        } else if (247.5 <= rotation && rotation < 292.5) {
            return new int[]{-1, 0}; // 西
        } else if (292.5 <= rotation && rotation < 337.5) {
            return new int[]{-1, -1}; // 西北
        } else {
            return new int[]{0, -1}; // 北（默认）
        }
    }
}
