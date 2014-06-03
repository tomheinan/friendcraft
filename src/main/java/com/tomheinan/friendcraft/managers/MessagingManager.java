package com.tomheinan.friendcraft.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ServerValue;
import com.firebase.client.ValueEventListener;
import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.callbacks.PlayerRefCallback;
import com.tomheinan.friendcraft.models.Inbox;

public class MessagingManager
{
    public final static MessagingManager sharedInstance = new MessagingManager();
    
    private final Map<UUID, Inbox> inboxes = Collections.synchronizedMap(new HashMap<UUID, Inbox>());
    
    private MessagingManager() { /* restrict direct instantiation */ }
    
    public void pin(Player player, UUID uuid)
    {
        synchronized(inboxes) {
            if (inboxes.get(uuid) == null) {
                inboxes.put(uuid, new Inbox(player));
            }
        }
    }
    
    public void unpin(UUID uuid)
    {
        Inbox inbox = null;
        synchronized(inboxes) {
            inbox = inboxes.remove(uuid);
        }
        
        if (inbox != null) {
            inbox.recycle();
        }
    }
    
    public static void sendMessage(final Player from, final String to, final String text, final String command)
    {
        FriendCraft.getPlayerRef(to, new PlayerRefCallback() {
            
            public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                final UUID senderId = PlayerManager.sharedInstance.getUUID(from);
                Firebase fromRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + senderId.toString() + "/conversations/" + playerId.toString()).push();
                Firebase toRef = playerRef.child("conversations").child(senderId.toString()).push();
                
                final Firebase notificationsRef = playerRef.child("notifications").child("unread").push();
                final Firebase replyToRef = playerRef.child("reply-to");
                final Long timestamp = new Long(System.currentTimeMillis());
                
                // set up message and notification constructs
                final Map<String, String> source = new HashMap<String, String>();
                String pluginId = (String) FriendCraft.sharedInstance.getConfig().get("authentication.id");
                String pluginName = (String) FriendCraft.sharedInstance.getConfig().get("name");
                source.put("id", pluginId);
                if (!pluginName.equalsIgnoreCase("a friendly name for your server here")) {
                    source.put("name", pluginName);
                }
                if (command != null) {
                    source.put("command", command);
                }
                
                final Map<String, String> sender = new HashMap<String, String>();
                sender.put("name", from.getName());
                sender.put("id", senderId.toString());
                
                // construct the message
                Map<String, Object> message = new HashMap<String, Object>();
                message.put("text", text);
                message.put("sender", sender);
                message.put("source", source);
                message.put("timestamp", ServerValue.TIMESTAMP);
                
                // "send" the message
                fromRef.setValue(message, timestamp);
                toRef.setValue(message, timestamp);
                from.sendMessage(ChatColor.LIGHT_PURPLE + " => <" + playerName + "> " + text);
                
                // check if the player has private mode enabled
                playerRef.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                    public void onDataChange(DataSnapshot snapshot) {
                        Boolean privateMode = (Boolean) snapshot.child("settings/private-mode").getValue();
                        if (privateMode == null) {
                            privateMode = Boolean.FALSE;
                        }
                        
                        Boolean inFriendsList = Boolean.FALSE;
                        if (privateMode.booleanValue()) {
                            Iterator<DataSnapshot> it = snapshot.child("friends").getChildren().iterator();
                            while (it.hasNext()) {
                                DataSnapshot friendEntry = it.next();
                                UUID uuid = UUID.fromString(friendEntry.getName());
                                if (senderId.equals(uuid)) {
                                    inFriendsList = Boolean.TRUE;
                                    break;
                                }
                            }
                        }
                        
                        // if private mode is off or sender is in recipient's list or it's a direct reply, notify recipient of the message
                        if (!privateMode.booleanValue() || inFriendsList.booleanValue() || command.equalsIgnoreCase("r")) {
                            // construct the notification
                            Map<String, Object> notification = new HashMap<String, Object>();
                            notification.put("type", "message");
                            notification.put("text", text);
                            notification.put("sender", sender);
                            notification.put("source", source);
                            notification.put("timestamp", ServerValue.TIMESTAMP);
                            
                            // "notify" the recipient
                            notificationsRef.setValue(notification, timestamp);
                            
                            // set the recipient's reply field to the sender's id
                            replyToRef.setValue(PlayerManager.sharedInstance.getUUID(from).toString());
                        }
                    }
                });
            }
            
            public void onNotFound() {
                from.sendMessage(
                    ChatColor.YELLOW + "FriendCraft can't find a player named " +
                    ChatColor.WHITE + to + ChatColor.YELLOW + ". Sorry!"
                );
            }
        });
    }
}
