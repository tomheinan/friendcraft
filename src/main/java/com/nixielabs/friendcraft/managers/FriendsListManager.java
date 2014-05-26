package com.nixielabs.friendcraft.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.UUIDCallback;
import com.nixielabs.friendcraft.models.FriendsList;
import com.nixielabs.friendcraft.tasks.UUIDTask;

public class FriendsListManager
{
    public final static FriendsListManager sharedInstance = new FriendsListManager();
    
    private final Map<UUID, FriendsList> lists = Collections.synchronizedMap(new HashMap<UUID, FriendsList>());
    
    private FriendsListManager() { /* restrict direct instantiation */ }
    
    public void register(final Player player)
    {
        UUIDCallback callback = new UUIDCallback() {
            
            public void onFound(UUID uuid) {
                if (!uuid.equals(player.getUniqueId())) {
                    handleUUIDMismatch(player, uuid);
                } else {
                    handleUUIDMatch(player);
                }
            }
            
            public void onNotFound() {
                FriendCraft.error("Canonical UUID not found for player " + player.getName());
            }
        };
        
        UUIDTask uuidTask = new UUIDTask(player, callback);
        uuidTask.runTaskAsynchronously(FriendCraft.sharedInstance);
    }
    
    public void deregister(Player player)
    {
        FriendsList list;
        synchronized(lists) {
            list = lists.remove(player.getUniqueId());
        }
        
        if (list != null) {
            list.hideSidebar();
            list.recycle();
        }
    }
    
    public void registerAll()
    {
        Player[] players = Bukkit.getServer().getOnlinePlayers();
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            register(player);
        }
    }
    
    public void deregisterAll()
    {
        synchronized(lists) {
            Collection<FriendsList> friendsLists = lists.values();
            Iterator<FriendsList> it = friendsLists.iterator();
            
            while (it.hasNext()) {
                FriendsList list = it.next();
                list.hideSidebar();
                list.recycle();
            }
            
            lists.clear();
        }
    }
    
    public FriendsList getList(Player player)
    {
        synchronized(lists) {
            return lists.get(player.getUniqueId());
        }
    }
    
    private void handleUUIDMatch(final Player player)
    {
        final String pluginId = (String) FriendCraft.sharedInstance.getConfig().get("authentication.id");
        
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
        pluginPlayersRef.child(player.getUniqueId().toString()).setValue(Boolean.TRUE);
        
        // add this plugin to the player's presence state
        playerRef.child("presence").child("plugin").setValue(pluginId);
        
        // set up and track a friends list for this player
        if (getList(player) == null) {
            synchronized(lists) {
                FriendsList list = new FriendsList(player);
                lists.put(player.getUniqueId(), list);
            }
        }
    }
    
    private void handleUUIDMismatch(Player player, UUID uuid)
    {
        FriendCraft.error(
            "UUID mismatch: " +
            player.getName() + "'s canonical UUID is " +
            uuid.toString() + ", but the server has assigned " +
            player.getUniqueId().toString() + ". " +
            "This probably means the server is running in offline mode or is otherwise misconfigured. " +
            "Friends list functionality for this player is unavailable until UUID parity is restored."
        );
    }
}
