package com.nixielabs.friendcraft.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.nixielabs.friendcraft.managers.FriendsListManager;
import com.nixielabs.friendcraft.models.FriendsList;

public class ScoreboardTask extends BukkitRunnable
{
    public void run()
    {
        Player[] players = Bukkit.getServer().getOnlinePlayers();
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            if (player != null && player.isOnline()) {
                FriendsList list = FriendsListManager.sharedInstance.getList(player);
                if (list != null) {
                    player.setScoreboard(list.getScoreboard());
                }
            }
        }
    }
}
