package com.tomheinan.friendcraft.models;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.managers.PlayerManager;

public class Inbox
{
    private final Player owner;
    
    private final Firebase inboxRef;
    private final ChildEventListener inboxListener;
    
    public Inbox(Player owner)
    {
        this.owner = owner;
        
        inboxRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(owner) + "/notifications");
        inboxListener = new ChildEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) { /* not relevant */ }
            public void onChildRemoved(DataSnapshot snapshot) { /* not relevant */ }

            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                String notificationType = (String) snapshot.child("type").getValue();
                if (notificationType != null && notificationType.equalsIgnoreCase("message")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> sender = (Map<String, String>) snapshot.child("sender").getValue();
                    
                    String senderName = sender.get("name");
                    String text = (String) snapshot.child("text").getValue();
                    Long timestamp = (Long) snapshot.child("timestamp").getValue();
                    
                    // TODO do some fuzzy formatting here for older unread messages
                    //Date date = new Date(timestamp.longValue());
                    //FriendCraft.log(date.toString());
                    
                    if (Inbox.this.owner.isOnline()) {
                        Inbox.this.owner.sendMessage(ChatColor.LIGHT_PURPLE + "<" + senderName + "> " + text);
                        
                        inboxRef.child("read").child(snapshot.getName()).setValue(snapshot.getValue(), timestamp);
                        inboxRef.child("unread").child(snapshot.getName()).removeValue();
                    }
                }
            }
        };
        
        inboxRef.child("unread").addChildEventListener(inboxListener);
    }
    
    public void recycle()
    {
        inboxRef.child("unread").removeEventListener(inboxListener);
    }
}
