package com.tomheinan.friendcraft.tasks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.callbacks.PlayerDeregistrationCallback;
import com.tomheinan.friendcraft.managers.FriendsListManager;
import com.tomheinan.friendcraft.managers.MessagingManager;
import com.tomheinan.friendcraft.managers.PlayerManager;
import com.tomheinan.friendcraft.managers.PresenceManager;

public class PlayerCleanupTask extends BukkitRunnable
{
    public void run() {
        Set<UUID> onlineUUIDs = new HashSet<UUID>();
        Set<UUID> localUUIDs = PlayerManager.sharedInstance.getLocalUUIDs();
        
        List<Player> playerList = Arrays.asList(Bukkit.getServer().getOnlinePlayers());
        Iterator<Player> itPlayer = playerList.iterator();
        while (itPlayer.hasNext()) {
            Player player = itPlayer.next();
            onlineUUIDs.add(player.getUniqueId());
        }
        
        Iterator<UUID> itLocalUUID = localUUIDs.iterator();
        int count = 0;
        while (itLocalUUID.hasNext()) {
            UUID localUUID = itLocalUUID.next();
            if (!onlineUUIDs.contains(localUUID)) {
                count++;
                PlayerManager.sharedInstance.deregister(localUUID, new PlayerDeregistrationCallback() {
                    public void onDeregistration(UUID uuid) {
                        PresenceManager.noteDeparture(uuid);
                        FriendsListManager.sharedInstance.unpin(uuid);
                        MessagingManager.sharedInstance.unpin(uuid);
                    }
                });
            }
        }
        
        if (count > 0) {
            FriendCraft.log("Cleaned up " + Integer.toString(count) + " ghost(s)");
        }
    }
}
