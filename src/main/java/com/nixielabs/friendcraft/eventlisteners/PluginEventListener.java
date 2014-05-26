package com.nixielabs.friendcraft.eventlisteners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.UUIDCallback;
import com.nixielabs.friendcraft.managers.FriendsListManager;
import com.nixielabs.friendcraft.tasks.UUIDTask;

public final class PluginEventListener implements Listener
{
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        
        
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        Player player = event.getPlayer();
        String pluginId = (String) FriendCraft.sharedInstance.getConfig().get("authentication.id");
        
        Firebase pluginPlayerRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + pluginId + "/players/" + player.getUniqueId().toString());
        Firebase presenceRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + player.getUniqueId().toString() + "/presence");
        
        // remove the player from the plugin's list of players
        pluginPlayerRef.removeValue();
        
        // remove this plugin from the player's presence state
        presenceRef.child("plugin").removeValue();
        
        // remove friends list listeners
        FriendsListManager.sharedInstance.deregister(player);
    }
}
