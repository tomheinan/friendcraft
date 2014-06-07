package com.tomheinan.friendcraft.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import com.tomheinan.friendcraft.models.FriendsList;

public class ScoreboardTask extends BukkitRunnable
{
    private final FriendsList list;
    
    public ScoreboardTask(FriendsList list)
    {
        this.list = list;
    }
    
    public void run()
    {
        if (list.getOwner().isOnline()) {
            list.getOwner().setScoreboard(list.getScoreboard());
        }
    }
}
