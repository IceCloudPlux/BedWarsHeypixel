/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.arena.tasks;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

/**
 * Handles 0.5 second respawn countdown display
 * This task runs every 0.5 seconds (10 ticks) to show precise respawn timers
 */
public class RespawnCountdownTask implements Runnable {
    
    private final IArena arena;
    private BukkitTask task;
    private int tickCounter = 0;
    
    public RespawnCountdownTask(IArena arena) {
        this.arena = arena;
        // Run every 0.5 seconds (10 ticks), delay 0.5s to sync with main task
        this.task = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this, 10L, 10L);
    }
    
    @Override
    public void run() {
        if (arena.getRespawnSessions().isEmpty()) {
            return;
        }
        
        // Increment counter
        tickCounter++;
        
        for (Player player : arena.getRespawnSessions().keySet()) {
            if (!player.isOnline()) {
                continue;
            }
            
            // Skip if player is no longer in respawn sessions
            if (!arena.getRespawnSessions().containsKey(player)) {
                continue;
            }
            
            // Get remaining time in half-seconds
            int remainingHalfSeconds = arena.getRespawnSessions().get(player);
            
            // If time is up, skip (will be handled by main task)
            if (remainingHalfSeconds <= 0) {
                continue;
            }
            
            // Convert to seconds for display
            double actualSeconds = remainingHalfSeconds / 2.0;
            
            // Format display: show "5" for 5 seconds, "4.5" for 4.5 seconds
            String timeDisplay;
            if (actualSeconds == Math.floor(actualSeconds)) {
                timeDisplay = String.valueOf((int) actualSeconds);
            } else {
                timeDisplay = String.valueOf(actualSeconds);
            }
            
            // Send title (stay for 30 ticks = 1.5 seconds to ensure smooth display)
            nms.sendTitle(player, 
                getMsg(player, Messages.PLAYER_DIE_RESPAWN_TITLE).replace("{time}", timeDisplay),
                getMsg(player, Messages.PLAYER_DIE_RESPAWN_SUBTITLE).replace("{time}", timeDisplay),
                0, 30, 0);  // No fade out to prevent flickering
        }
    }
    
    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }
    
    public BukkitTask getBukkitTask() {
        return task;
    }
}