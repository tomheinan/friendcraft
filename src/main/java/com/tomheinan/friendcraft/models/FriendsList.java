package com.tomheinan.friendcraft.models;

import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
    
    private final Firebase friendsListRef;
    private final ChildEventListener friendsListListener;
    
    public FriendsList(Player owner)
    {
        this.owner = owner;
        
        friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + owner.getUniqueId().toString() + "/friends");
        friendsListListener = new ChildEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) { /* not relevant */}
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }

            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                UUID uuid = UUID.fromString(snapshot.getName());
                Friend friend = FriendManager.sharedInstance.getFriend(uuid);
                friend.addToList(FriendsList.this);
            }

            public void onChildRemoved(DataSnapshot snapshot) {
                UUID uuid = UUID.fromString(snapshot.getName());
                Friend friend = FriendManager.sharedInstance.getFriend(uuid);
                friend.removeFromList(FriendsList.this);
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
    
    public void render()
    {
        
    }
    
    public String toString()
    {
        return Integer.toString(getFriends().size());
    }
    
    public void unlink()
    {
        friendsListRef.removeEventListener(friendsListListener);
    }
}
