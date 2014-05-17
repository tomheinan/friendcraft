package com.tomheinan.friendcraft;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

public final class EventListener implements Listener
{
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        final String pluginId = (String) FriendCraft.configuration.get("authentication.id");
        
        final Firebase playerRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + player.getUniqueId().toString());
        final Firebase pluginPlayersRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + pluginId + "/players");
        
        // update the player's current friendly name
        playerRef.child("name").setValue(player.getName(), new Firebase.CompletionListener() {

            public void onComplete(FirebaseError error, Firebase ref) {
                if (error == null) {
                    // index this player by name
                    Firebase indexPlayerByNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name");
                    indexPlayerByNameRef.child(player.getName().toLowerCase()).setValue(player.getUniqueId().toString());
                } else {
                    FriendCraft.error(error.getMessage());
                }
            }
        });
        
        // add the player to this plugin's list of players
        pluginPlayersRef.child(player.getUniqueId().toString()).setValue(Boolean.TRUE, new Firebase.CompletionListener() {
            
            public void onComplete(FirebaseError error, Firebase ref) {
                if (error == null) {
                    // add this plugin to the player's presence state
                    playerRef.child("presence").child("plugin").setValue(pluginId);
                } else {
                    FriendCraft.error(error.getMessage());
                }
            }
        });
        
        // set up and track a friends list for this player
        FriendsListManager.sharedInstance.register(player);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        final Player player = event.getPlayer();
        final String pluginId = (String) FriendCraft.configuration.get("authentication.id");
        
        final Firebase pluginPlayerRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + pluginId + "/players/" + player.getUniqueId().toString());
        final Firebase presenceRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + player.getUniqueId().toString() + "/presence");
        
        // remove the player from the plugin's list of players
        pluginPlayerRef.removeValue(new Firebase.CompletionListener() {
            
            public void onComplete(FirebaseError error, Firebase ref) {
                if (error == null) {
                    // remove this plugin from the player's presence state
                    presenceRef.child("plugin").removeValue();
                } else {
                    FriendCraft.error(error.getMessage());
                }
            }
        });
        
        // remove friends list listeners
        FriendsListManager.sharedInstance.deregister(player);
    }
}
