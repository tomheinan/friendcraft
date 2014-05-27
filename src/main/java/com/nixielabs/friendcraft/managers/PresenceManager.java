package com.nixielabs.friendcraft.managers;

import java.util.UUID;

import org.bukkit.entity.Player;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.nixielabs.friendcraft.FriendCraft;

public abstract class PresenceManager
{
    public static void noteArrival(final Player player, final UUID uuid)
    {
        String pluginId = (String) FriendCraft.sharedInstance.getConfig().get("authentication.id");
        
        Firebase playerRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + uuid.toString());
        Firebase pluginPlayersRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + pluginId + "/players");
        
        // update the player's current friendly name
        playerRef.child("name").setValue(player.getName(), new Firebase.CompletionListener() {

            public void onComplete(FirebaseError error, Firebase ref) {
                if (error == null) {
                    // index this player by name
                    Firebase indexPlayerByNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name");
                    indexPlayerByNameRef.child(player.getName().toLowerCase()).setValue(uuid.toString());
                } else {
                    FriendCraft.error(error.getMessage());
                }
            }
        });
        
        // add the player to this plugin's list of players
        pluginPlayersRef.child(uuid.toString()).setValue(Boolean.TRUE);
        
        // add this plugin to the player's presence state
        playerRef.child("presence").child("plugin").setValue(pluginId);
    }
    
    public static void noteDeparture(UUID uuid)
    {
        String pluginId = (String) FriendCraft.sharedInstance.getConfig().get("authentication.id");
        
        Firebase pluginPlayerRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + pluginId + "/players/" + uuid.toString());
        Firebase presenceRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + uuid.toString() + "/presence");
        
        // remove the player from the plugin's list of players
        pluginPlayerRef.removeValue();
        
        // remove this plugin from the player's presence state
        presenceRef.child("plugin").removeValue();
    }
}
