package com.nixielabs.friendcraft.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.PlayerRefCallback;

public class FriendsList
{
    private final Player owner;
    private final List<Friend> friends;
    
    private Scoreboard sidebar;
    private final Scoreboard blank = Bukkit.getScoreboardManager().getNewScoreboard();
    private final boolean enableSidebar;
    private boolean showSidebar = true;
    
    private final Firebase friendsListRef;
    private final ChildEventListener friendsListListener;
    
    public FriendsList(Player owner)
    {
        this.owner = owner;
        this.friends = Collections.synchronizedList(new ArrayList<Friend>());
        this.enableSidebar = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar");
        
        friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + owner.getUniqueId().toString() + "/friends");
        friendsListListener = new ChildEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) { /* not relevant */}
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }

            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                final UUID playerId = UUID.fromString(snapshot.getName());
                FriendCraft.getPlayerRef(playerId, new PlayerRefCallback() {
                    
                    public void onNotFound() { FriendCraft.error("Can't find player with ID " + playerId.toString()); }
                    
                    public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                        synchronized(friends) {
                            friends.add(new Friend(FriendsList.this, playerRef, playerId, playerName));
                            Collections.sort(friends);
                            render();
                        }
                    }
                });
            }

            public void onChildRemoved(DataSnapshot snapshot) {
                UUID playerId = UUID.fromString(snapshot.getName());
                Friend friend = getFriend(playerId);
                
                synchronized(friends) {
                    friends.remove(friend);
                    friend.recycle();
                    render();
                }
            }
        };
        
        friendsListRef.addChildEventListener(friendsListListener);
    }
    
    public void add(final UUID playerId)
    {
        // check to see if the current player already has the friend on his/her list
        friendsListRef.child(playerId.toString()).addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null && ((Boolean)snapshot.getValue()).booleanValue()) {
                    FriendCraft.getPlayerRef(playerId, new PlayerRefCallback() {
                        
                        public void onNotFound() { FriendCraft.error("Can't find player with ID " + playerId.toString()); }
                        
                        public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                            owner.sendMessage(playerName + ChatColor.YELLOW + " is already on your friends list.");
                        }
                    });
                    
                } else {
                    // add the friend
                    friendsListRef.child(playerId.toString()).setValue(Boolean.TRUE, new Firebase.CompletionListener() {
                        
                        public void onComplete(FirebaseError error, Firebase ref) {
                            FriendCraft.getPlayerRef(playerId, new PlayerRefCallback() {
                                
                                public void onNotFound() { FriendCraft.error("Can't find player with ID " + playerId.toString()); }
                                
                                public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                                    owner.sendMessage(ChatColor.YELLOW + "Added " + ChatColor.WHITE + playerName + ChatColor.YELLOW + " to your friends list.");
                                }
                            });
                        }
                    });
                }
            }
        });
    }
    
    public void remove(final UUID playerId)
    {
        // check to see if the friend is present on the player's list
        friendsListRef.child(playerId.toString()).addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null || !((Boolean)snapshot.getValue()).booleanValue()) {
                    FriendCraft.getPlayerRef(playerId, new PlayerRefCallback() {
                        
                        public void onNotFound() { FriendCraft.error("Can't find player with ID " + playerId.toString()); }
                        
                        public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                            owner.sendMessage(playerName + ChatColor.YELLOW + " is not on your friends list.");
                        }
                    });
                    
                } else {
                    // remove the friend from the list
                    friendsListRef.child(playerId.toString()).removeValue(new Firebase.CompletionListener() {
                        
                        public void onComplete(FirebaseError error, Firebase ref) {
                            FriendCraft.getPlayerRef(playerId, new PlayerRefCallback() {
                                
                                public void onNotFound() { FriendCraft.error("Can't find player with ID " + playerId.toString()); }
                                
                                public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                                    owner.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.WHITE + playerName + ChatColor.YELLOW + " from your friends list.");
                                }
                            });
                        }
                    });
                }
            }
        });
    }
    
    public int size()
    {
        synchronized(friends) {
            return friends.size();
        }
    }
    
    public Friend getFriend(UUID playerId)
    {
        synchronized(friends) {
            Iterator<Friend> it = friends.iterator();
            while (it.hasNext()) {
                Friend friend = it.next();
                if (friend.getUniqueId().equals(playerId)) {
                    return friend;
                }
            }
        }
        
        return null;
    }
    
    public void showSidebar()
    {
        showSidebar = true;
        render();
    }
    
    public void hideSidebar()
    {
        showSidebar = false;
        render();
    }
    
    public void render()
    {
        if (enableSidebar && owner.isOnline()) {
            if (showSidebar) {
                sidebar = Bukkit.getScoreboardManager().getNewScoreboard();
                Objective friendsObjective = sidebar.registerNewObjective("friends", "dummy");
                friendsObjective.setDisplayName(ChatColor.YELLOW + "Friends");
                friendsObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
                
                synchronized(friends) {
                    Iterator<Friend> it = friends.iterator();
                    while (it.hasNext()) {
                        Friend friend = it.next();
                        friendsObjective.getScore(friend.getDisplayName()).setScore(0);
                    }
                }
                
                owner.setScoreboard(sidebar);
            } else {
                owner.setScoreboard(blank);
            }
        }
    }
    
    public Player getOwner()
    {
        return owner;
    }
    
    @Override
    public String toString()
    {
        Friend[] friendArray;
        synchronized(friends) {
            friendArray = new Friend[friends.size()];
            friendArray = friends.toArray(friendArray);
        }
        StringBuilder stringBuilder = new StringBuilder();
        
        if (friendArray.length == 0) {
            stringBuilder.append(ChatColor.YELLOW + "No friends yet.");
        } else {
            stringBuilder.append(ChatColor.YELLOW + "Friends: ");
            
            if (friendArray.length == 1) {
                stringBuilder.append(friendArray[0].getDisplayName() + ChatColor.YELLOW + ".");
            } else if (friendArray.length == 2) {
                stringBuilder.append(friendArray[0].getDisplayName() + ChatColor.YELLOW + " and " + friendArray[1].getDisplayName() + ChatColor.YELLOW + ".");
            } else {
                for (int i = 0; i < friendArray.length - 1; i++) {
                    Friend friend = friendArray[i];
                    stringBuilder.append(friend.getDisplayName() + ChatColor.YELLOW + ", ");
                }
                stringBuilder.append(ChatColor.YELLOW + "and " + friendArray[friendArray.length - 1].getDisplayName() + ChatColor.YELLOW + ".");
            }
        }
        
        return stringBuilder.toString();
    }
    
    public void recycle()
    {
        friendsListRef.removeEventListener(friendsListListener);
        
        synchronized(friends) {
            Iterator<Friend> it = friends.iterator();
            while (it.hasNext()) {
                Friend friend = it.next();
                friend.recycle();
            }
            
            friends.clear();
        }
    }
}
