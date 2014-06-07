package com.tomheinan.friendcraft.models;

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
import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.callbacks.PlayerRefCallback;
import com.tomheinan.friendcraft.managers.PlayerManager;
import com.tomheinan.friendcraft.tasks.ScoreboardTask;

public class FriendsList
{
    private final Player owner;
    private final List<Friend> friends;
    
    private final Firebase friendsListRef;
    private final ChildEventListener friendsListListener;
    
    private final Firebase sidebarSettingRef;
    private final ValueEventListener sidebarSettingListener;
    
    private static final int MAX_SIDEBAR_SLOTS = 15; // client limitation
    private boolean showSidebar = false;
    
    public FriendsList(Player owner)
    {
        this.owner = owner;
        this.friends = Collections.synchronizedList(new ArrayList<Friend>());
        
        friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(owner) + "/friends");
        friendsListListener = new ChildEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }

            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                final UUID playerId = UUID.fromString(snapshot.getName());
                FriendCraft.getPlayerRef(playerId, new PlayerRefCallback() {
                    
                    public void onNotFound() { FriendCraft.error("Can't find player with ID " + playerId.toString()); }
                    
                    public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                        synchronized(friends) {
                            friends.add(new Friend(FriendsList.this, playerRef, playerId, playerName));
                            Collections.sort(friends);
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
                }
            }
        };
        
        sidebarSettingRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(owner) + "/settings/sidebar");
        sidebarSettingListener = new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                Boolean showSidebar = (Boolean) snapshot.getValue();
                if (showSidebar == null) {
                    showSidebar = Boolean.TRUE;
                }
                
                FriendsList.this.showSidebar = showSidebar.booleanValue();
                
                // update scoreboard
                boolean sidebarEnabled = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar", true);
                if (sidebarEnabled) {
                    ScoreboardTask task = new ScoreboardTask(FriendsList.this);
                    task.runTask(FriendCraft.sharedInstance);
                }
            }
        };
        
        friendsListRef.addChildEventListener(friendsListListener);
        sidebarSettingRef.addValueEventListener(sidebarSettingListener);
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
    
    public Player getOwner()
    {
        return owner;
    }
    
    public Scoreboard getScoreboard()
    {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        if (showSidebar) {
            Objective objective = scoreboard.registerNewObjective("friends", "dummy");
            objective.setDisplayName(ChatColor.YELLOW + "Friends");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            
            List<String> onlineFriends = new ArrayList<String>();
            List<String> offlineFriends = new ArrayList<String>();
            
            synchronized(friends) {
                Iterator<Friend> it = friends.iterator();
                
                while (it.hasNext()) {
                    Friend friend = it.next();
                    
                    String displayName = friend.getDisplayName();
                    displayName = displayName.substring(0, Math.min(displayName.length(), 16));
                    if (friend.getStatus() == Friend.Status.OFFLINE || friend.getStatus() == Friend.Status.UNKNOWN) {
                        offlineFriends.add(displayName);
                    } else {
                        onlineFriends.add(displayName);
                    }
                }
            }
            
            // the total number of slots we have, plus one each for the headers, if present
            int totalSlots = onlineFriends.size();
            if (onlineFriends.size() > 0) {
                totalSlots++;
            }
            if (offlineFriends.size() > 0) {
                totalSlots++;
            }
            
            // the "current slot", not to exceed the maximum number of slots available for the sidebar
            int slot = Math.min(totalSlots, MAX_SIDEBAR_SLOTS);
            
            if (onlineFriends.size() > 0) {
                String sectionHeader = ChatColor.YELLOW + "Online (" + Integer.toString(onlineFriends.size()) + ")";
                objective.getScore(sectionHeader.substring(0, Math.min(sectionHeader.length(), 16))).setScore(slot);
                slot--;
                
                Iterator<String> it = onlineFriends.iterator();
                int count = 0;
                while(it.hasNext()) {
                    String displayName = it.next();
                    count++;
                    int more = onlineFriends.size() - count;
                    
                    if (((offlineFriends.size() > 0 && slot == 2) || (offlineFriends.size() == 0 && slot == 1)) && more > 0) {
                        String moreString = "and " + ChatColor.GREEN + Integer.toString(more + 1) + " more";
                        objective.getScore(moreString.substring(0, Math.min(moreString.length(), 16))).setScore(slot);
                        slot--;
                        break;
                    } else {
                        objective.getScore(displayName).setScore(slot);
                        slot--;
                    }
                }
            }
            
            if (offlineFriends.size() > 0) {
                String sectionHeader = ChatColor.YELLOW + "Offline (" + Integer.toString(offlineFriends.size()) + ")";
                objective.getScore(sectionHeader.substring(0, Math.min(sectionHeader.length(), 16))).setScore(slot);
                slot--;
            }
        }
        
        return scoreboard;
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
        sidebarSettingRef.removeEventListener(sidebarSettingListener);
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
