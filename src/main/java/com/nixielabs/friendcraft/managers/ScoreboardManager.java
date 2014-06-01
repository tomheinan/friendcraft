package com.nixielabs.friendcraft.managers;

import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.tasks.ScoreboardTask;

public class ScoreboardManager
{
    public final static ScoreboardManager sharedInstance = new ScoreboardManager();
    
    private ScoreboardTask task;
    private final long period = 1; // seconds
    
    private ScoreboardManager() { /* restrict direct instantiation */ }
    
    public void start()
    {
        if (FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar", true)) {
            task = new ScoreboardTask();
            task.runTaskTimer(FriendCraft.sharedInstance, 0L, (long) (20 * period));
        }
    }
    
    public void stop()
    {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
