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
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.events.player.PlayerInvisibilityPotionEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.tasks.PlayingTask;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class GamePlayingTask implements Runnable, PlayingTask {

    private Arena arena;
    private BukkitTask task;
    private int beds_destroy_countdown, dragon_spawn_countdown, game_end_countdown;
    private RespawnCountdownTask respawnCountdownTask; // Separate task for 0.5s respawn display

    public GamePlayingTask(Arena arena) {
        this.arena = arena;
        this.beds_destroy_countdown = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_BEDS_DESTROY_COUNTDOWN);
        this.dragon_spawn_countdown = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_DRAGON_SPAWN_COUNTDOWN);
        this.game_end_countdown = BedWars.config.getInt(ConfigPath.GENERAL_CONFIGURATION_GAME_END_COUNTDOWN);
        this.task = Bukkit.getScheduler().runTaskTimer(BedWars.plugin, this, 0, 20L); // Run every second (20 ticks)
        
        // Start separate respawn countdown task for 0.5s display
        this.respawnCountdownTask = new RespawnCountdownTask(arena);
    }

    public Arena getArena() {
        return arena;
    }

    @Override
    public BukkitTask getBukkitTask() {
        return task;
    }

    /**
     * Get task ID
     */
    public int getTask() {
        return task.getTaskId();
    }

    public int getBedsDestroyCountdown() {
        return beds_destroy_countdown;
    }

    public int getDragonSpawnCountdown() {
        return dragon_spawn_countdown;
    }

    public int getGameEndCountdown() {
        return game_end_countdown;
    }

    @Override
    public void run() {
        switch (getArena().getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
            case EMERALD_GENERATOR_TIER_III:
            case EMERALD_GENERATOR_TIER_IV:
            case EMERALD_GENERATOR_TIER_V:
            case DIAMOND_GENERATOR_TIER_II:
            case DIAMOND_GENERATOR_TIER_III:
            case DIAMOND_GENERATOR_TIER_IV:
            case DIAMOND_GENERATOR_TIER_V:
                if (getArena().upgradeDiamondsCount > 0) {
                    getArena().upgradeDiamondsCount--;
                    if (getArena().upgradeDiamondsCount == 0) {
                        getArena().updateNextEvent();
                    }
                }
                if (getArena().upgradeEmeraldsCount > 0) {
                    getArena().upgradeEmeraldsCount--;
                    if (getArena().upgradeEmeraldsCount == 0) {
                        getArena().updateNextEvent();
                    }
                }
                break;
            case BEDS_DESTROY:
                beds_destroy_countdown--;
                if (getBedsDestroyCountdown() == 0) {
                    for (Player p : getArena().getPlayers()) {
                        nms.sendTitle(p, getMsg(p, Messages.NEXT_EVENT_TITLE_ANNOUNCE_BEDS_DESTROYED), getMsg(p, Messages.NEXT_EVENT_SUBTITLE_ANNOUNCE_BEDS_DESTROYED), 0, 40, 10);
                        p.sendMessage(getMsg(p, Messages.NEXT_EVENT_CHAT_ANNOUNCE_BEDS_DESTROYED));
                    }
                    for (Player p : getArena().getSpectators()) {
                        nms.sendTitle(p, getMsg(p, Messages.NEXT_EVENT_TITLE_ANNOUNCE_BEDS_DESTROYED), getMsg(p, Messages.NEXT_EVENT_SUBTITLE_ANNOUNCE_BEDS_DESTROYED), 0, 40, 10);
                        p.sendMessage(getMsg(p, Messages.NEXT_EVENT_CHAT_ANNOUNCE_BEDS_DESTROYED));
                    }
                    for (ITeam t : getArena().getTeams()) {
                        t.setBedDestroyed(true);
                    }
                    getArena().updateNextEvent();
                }
                break;
            case GAME_END:
                game_end_countdown--;
                if (getGameEndCountdown() == 0) {
                    getArena().checkWinner();
                    getArena().changeStatus(GameState.restarting);
                }
                break;
        }

        int distance = 0;
        for (ITeam t : getArena().getTeams()) {
            if (t.getSize() > 1) {
                for (Player p : t.getMembers()) {
                    for (Player p2 : t.getMembers()) {
                        if (p2 == p) continue;
                        if (distance == 0) {
                            distance = (int) p.getLocation().distance(p2.getLocation());
                        } else if ((int) p.getLocation().distance(p2.getLocation()) < distance) {
                            distance = (int) p.getLocation().distance(p2.getLocation());
                        }
                    }
                    nms.playAction(p, getMsg(p, Messages.FORMATTING_ACTION_BAR_TRACKING).replace("{team}", t.getColor().chat() + t.getDisplayName(Language.getPlayerLanguage(p)))
                            .replace("{distance}", t.getColor().chat().toString() + distance).replace("&", "§"));
                }
            }

            // spawn items
            for (IGenerator o : t.getGenerators()) {
                o.spawn();
            }
        }

        /* RESPAWN SESSION */
        if (!getArena().getRespawnSessions().isEmpty()) {
            for (Map.Entry<Player, Integer> e : getArena().getRespawnSessions().entrySet()) {
                if (e.getValue() <= 0) {
                    IArena a = Arena.getArenaByPlayer(e.getKey());
                    if (a == null) {
                        getArena().getRespawnSessions().remove(e.getKey());
                        continue;
                    }
                    ITeam t = a.getTeam(e.getKey());
                    if (t == null){
                        a.addSpectator(e.getKey(), true, null);
                    } else {
                        t.respawnMember(e.getKey());
                        e.getKey().setAllowFlight(false);
                        e.getKey().setFlying(false);
                    }
                } else {
                    // Decrease by 2 (half-seconds) - RespawnCountdownTask handles the display
                    getArena().getRespawnSessions().replace(e.getKey(), e.getValue() - 2);
                }
            }
        }

        /* INVISIBILITY FOR ARMOR */
        if (!getArena().getShowTime().isEmpty()) {
            for (Map.Entry<Player, Integer> e : getArena().getShowTime().entrySet()) {
                if (e.getValue() <= 0) {
                    for (Player p : e.getKey().getWorld().getPlayers()) {
                        nms.showArmor(e.getKey(), p);
                        //nms.showPlayer(e.getKey(), p);
                    }
                    e.getKey().removePotionEffect(PotionEffectType.INVISIBILITY);
                    getArena().getShowTime().remove(e.getKey());
                    Bukkit.getPluginManager().callEvent(new PlayerInvisibilityPotionEvent(PlayerInvisibilityPotionEvent.Type.REMOVED, getArena().getTeam(e.getKey()), e.getKey(), getArena()));
                } else {
                    // Update invisibility time
                    getArena().getShowTime().replace(e.getKey(), e.getValue() - 1);
                }
            }
        }

        /* SPAWN ITEMS */
        for (IGenerator o : getArena().getOreGenerators()) {
            o.spawn();
        }
    }

    public void cancel() {
        task.cancel();
        if (respawnCountdownTask != null) {
            respawnCountdownTask.cancel();
        }
    }

    /**
     * Add elapsed time to the current game phase
     * This will make events happen sooner by reducing countdown timers
     * @param seconds the amount of seconds to add to elapsed time
     */
    public void addTime(int seconds) {
        switch (getArena().getNextEvent()) {
            case EMERALD_GENERATOR_TIER_II:
            case EMERALD_GENERATOR_TIER_III:
            case EMERALD_GENERATOR_TIER_IV:
            case EMERALD_GENERATOR_TIER_V:
            case DIAMOND_GENERATOR_TIER_II:
            case DIAMOND_GENERATOR_TIER_III:
            case DIAMOND_GENERATOR_TIER_IV:
            case DIAMOND_GENERATOR_TIER_V:
                // Reduce upgrade countdowns to make upgrades happen sooner
                getArena().upgradeDiamondsCount = Math.max(0, getArena().upgradeDiamondsCount - seconds);
                getArena().upgradeEmeraldsCount = Math.max(0, getArena().upgradeEmeraldsCount - seconds);
                break;
            case BEDS_DESTROY:
                // Limit to current countdown to prevent overflow
                int bedSeconds = Math.min(seconds, beds_destroy_countdown);
                beds_destroy_countdown = Math.max(0, beds_destroy_countdown - bedSeconds);
                break;
            case ENDER_DRAGON:
                // Limit to current countdown to prevent overflow
                int dragonSeconds = Math.min(seconds, dragon_spawn_countdown);
                dragon_spawn_countdown = Math.max(0, dragon_spawn_countdown - dragonSeconds);
                break;
            case GAME_END:
                // Limit to current countdown to prevent overflow
                int endSeconds = Math.min(seconds, game_end_countdown);
                game_end_countdown = Math.max(0, game_end_countdown - endSeconds);
                break;
        }
    }
}


