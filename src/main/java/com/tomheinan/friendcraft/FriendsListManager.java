package com.tomheinan.friendcraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class FriendsListManager
{
    public final static FriendsListManager sharedInstance = new FriendsListManager();
    
    private final Map<UUID, Scoreboard> lists = new HashMap<UUID, Scoreboard>();
    private final Map<UUID, ChildEventListener> listListeners = new HashMap<UUID, ChildEventListener>();
    private final Map<UUID, Map<String, ValueEventListener>> playerListeners = new HashMap<UUID, Map<String, ValueEventListener>>();
    private final Scoreboard blankList = Bukkit.getScoreboardManager().getNewScoreboard();
    
    private FriendsListManager() { /* restrict direct instantiation */ }
    
    public void addPlayer(Player player)
    {
        final Player currentPlayer = player;
        final Scoreboard friendsList = Bukkit.getScoreboardManager().getNewScoreboard();
        
        final Map<String, ValueEventListener> currentPlayerListeners = new HashMap<String, ValueEventListener>();
        playerListeners.put(currentPlayer.getUniqueId(), currentPlayerListeners);
        
        lists.put(currentPlayer.getUniqueId(), friendsList);
        listListeners.put(currentPlayer.getUniqueId(), new ChildEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) { /* not relevant */}
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }

            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String friendId = snapshot.getName();
                Firebase friendRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId);
                
                ValueEventListener playerUpdateListener = new ValueEventListener() {

                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                    public void onDataChange(DataSnapshot friendSnapshot) {
                        String friendName = (String) friendSnapshot.child("name").getValue();
                        FriendCraft.PresenceState state = FriendCraft.PresenceState.OFFLINE;
                        
                        if (friendSnapshot.child("presence").getValue() != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> presence = (Map<String, Object>) friendSnapshot.child("presence").getValue();
                            
                            if (presence.get("plugin") != null) {
                                state = FriendCraft.PresenceState.PLUGIN;
                            }
                        }
                        
                        // add or update the player's name in the sidebar
                        updateFriend(friendsList, friendName, state);
                        FriendsListManager.sharedInstance.showList(currentPlayer);
                    }
                };
                
                // monitor this player for presence state changes
                currentPlayerListeners.put(friendId, friendRef.addValueEventListener(playerUpdateListener));
            }

            public void onChildRemoved(DataSnapshot snapshot) {
                String friendId = snapshot.getName();
                Firebase friendRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId);
                
                // stop monitoring this player for presence state changes
                friendRef.removeEventListener(currentPlayerListeners.get(friendId));
                currentPlayerListeners.remove(friendId);
                
                // explicitly remove the player from the sidebar list
                friendRef.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                    public void onDataChange(DataSnapshot friendSnapshot) {
                        String friendName = (String) friendSnapshot.child("name").getValue();
                        removeFriend(friendsList, friendName);
                    }
                });
            }
        });
        
        final Objective friends = friendsList.registerNewObjective("friends", "dummy");
        friends.setDisplayName(ChatColor.YELLOW + "Friends");
        friends.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        final Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
        friendsListRef.addChildEventListener(listListeners.get(currentPlayer.getUniqueId()));
        
        
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(FriendCraft.sharedInstance, new Runnable() {

            public void run() {
                showList(currentPlayer);
            }
        }, 10L);
    }
    
    public void removePlayer(Player player)
    {
        Map<String, ValueEventListener> currentPlayerListeners = playerListeners.get(player.getUniqueId());
        Iterator<String> it = currentPlayerListeners.keySet().iterator();
        
        // stop monitoring value change events for all this player's friends
        while (it.hasNext()) {
            String friendId = it.next();
            Firebase friendRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId);
            
            friendRef.removeEventListener(currentPlayerListeners.get(friendId));
        }
        
        // stop monitoring friend list changes
        Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + player.getUniqueId().toString() + "/friends");
        friendsListRef.removeEventListener(listListeners.get(player.getUniqueId()));
        
        // clean up
        playerListeners.remove(player.getUniqueId());
        listListeners.remove(player.getUniqueId());
        lists.remove(player.getUniqueId());
    }
    
    public void showList(Player player)
    {
        Scoreboard friendsList = lists.get(player.getUniqueId());
        if (friendsList != null) {
            player.setScoreboard(blankList);
            player.setScoreboard(friendsList);
        }
    }
    
    public void hideList(Player player)
    {
        player.setScoreboard(blankList);
    }
    
    private void removeFriend(Scoreboard friendsList, String playerName)
    {
        friendsList.resetScores(FriendCraft.color(playerName, FriendCraft.PresenceState.OFFLINE));
        friendsList.resetScores(FriendCraft.color(playerName, FriendCraft.PresenceState.PLUGIN));
    }
    
    private void updateFriend(Scoreboard friendsList, String playerName, FriendCraft.PresenceState state)
    {
        removeFriend(friendsList, playerName);
        
        Objective friends = friendsList.getObjective("friends");
        friends.getScore(FriendCraft.color(playerName, state)).setScore(0);
    }
}
