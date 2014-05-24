package com.tomheinan.friendcraft.models;

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
import com.tomheinan.friendcraft.FriendManager;

public class FriendsList
{
    private final Player owner;
    private Scoreboard sidebar;
    private final Scoreboard blank = Bukkit.getScoreboardManager().getNewScoreboard();
    private final boolean enableSidebar;
    private boolean showSidebar = false;
    
    private final Firebase friendsListRef;
    private final ChildEventListener friendsListListener;
    
    public FriendsList(Player owner)
    {
        this.owner = owner;
        this.enableSidebar = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar");
        
        friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + owner.getUniqueId().toString() + "/friends");
        friendsListListener = new ChildEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) { /* not relevant */}
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }

            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                UUID uuid = UUID.fromString(snapshot.getName());
                Friend friend = FriendManager.sharedInstance.getFriend(uuid);
                friend.addToList(FriendsList.this);
                render();
            }

            public void onChildRemoved(DataSnapshot snapshot) {
                UUID uuid = UUID.fromString(snapshot.getName());
                Friend friend = FriendManager.sharedInstance.getFriend(uuid);
                friend.removeFromList(FriendsList.this);
                render();
            }
        };
        
        friendsListRef.addChildEventListener(friendsListListener);
    }
    
    public void add(final Friend friend)
    {
        // check to see if the current player already has the friend on his/her list
        friendsListRef.child(friend.getUUID().toString()).addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null && ((Boolean)snapshot.getValue()).booleanValue()) {
                    owner.sendMessage(friend.getDisplayName() + ChatColor.YELLOW + " is already on your friends list.");
                    
                } else {
                    // add the friend
                    friendsListRef.child(friend.getUUID().toString()).setValue(Boolean.TRUE, new Firebase.CompletionListener() {
                        
                        public void onComplete(FirebaseError error, Firebase ref) {
                            owner.sendMessage(ChatColor.YELLOW + "Added " + friend.getDisplayName() + ChatColor.YELLOW + " to your friends list.");
                        }
                    });
                }
            }
        });
    }
    
    public void remove(final Friend friend)
    {
        // check to see if the friend is present on the player's list
        friendsListRef.child(friend.getUUID().toString()).addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null || !((Boolean)snapshot.getValue()).booleanValue()) {
                    owner.sendMessage(friend.getDisplayName() + ChatColor.YELLOW + " is not on your friends list.");
                    
                } else {
                    // remove the friend from the list
                    friendsListRef.child(friend.getUUID().toString()).removeValue(new Firebase.CompletionListener() {
                        
                        public void onComplete(FirebaseError error, Firebase ref) {
                            owner.sendMessage(ChatColor.YELLOW + "Removed " + friend.getDisplayName() + ChatColor.YELLOW + " from your friends list.");
                        }
                    });
                }
            }
        });
    }
    
    public List<Friend> getFriends()
    {
        return FriendManager.sharedInstance.getFriends(this);
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
                
                List<Friend> friends = getFriends();
                Iterator<Friend> it = friends.iterator();
                while (it.hasNext()) {
                    Friend friend = it.next();
                    friendsObjective.getScore(friend.getDisplayName()).setScore(0);
                }
                
                owner.setScoreboard(sidebar);
            } else {
                owner.setScoreboard(blank);
            }
        }
    }
    
    public void notify(String message)
    {
        if (owner.isOnline()) {
            owner.sendMessage(message);
        }
    }
    
    public String toString()
    {
        Friend[] friends = new Friend[getFriends().size()];
        friends = getFriends().toArray(friends);
        StringBuilder stringBuilder = new StringBuilder();
        
        if (friends.length == 0) {
            stringBuilder.append(ChatColor.YELLOW + "No friends yet.");
        } else {
            stringBuilder.append(ChatColor.YELLOW + "Friends: ");
            
            if (friends.length == 1) {
                stringBuilder.append(friends[0].getDisplayName() + ChatColor.YELLOW + ".");
            } else if (friends.length == 2) {
                stringBuilder.append(friends[0].getDisplayName() + ChatColor.YELLOW + " and " + friends[1].getDisplayName() + ".");
            } else {
                for (int i = 0; i < friends.length - 1; i++) {
                    Friend friend = friends[i];
                    stringBuilder.append(friend.getDisplayName() + ChatColor.YELLOW + ", ");
                }
                stringBuilder.append(ChatColor.YELLOW + ", and " + friends[friends.length - 1].getDisplayName() + ChatColor.YELLOW + ".");
            }
        }
        
        return stringBuilder.toString();
    }
    
    public void unlink()
    {
        friendsListRef.removeEventListener(friendsListListener);
    }
}
