package com.nixielabs.friendcraft.models;

import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.nixielabs.friendcraft.FriendCraft;

public class Friend implements Comparable<Friend>
{
    public enum Status
    {
        UNKNOWN, OFFLINE, PLUGIN, APP
    }
    
    private final FriendsList list;
    private final UUID uuid;
    private String name;
    private Status status;
    private String source;
    
    private final Firebase playerRef;
    private final ValueEventListener playerListener;
    
    public Friend(FriendsList list, Firebase playerRef, UUID uuid, String playerName)
    {
        this.list = list;
        this.uuid = uuid;
        this.name = playerName;
        
        this.status = Status.UNKNOWN;
        this.source = new String();
        
        this.playerRef = playerRef;
        this.playerListener = new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                Friend.this.name = (String) snapshot.child("name").getValue();
                Status oldStatus = Friend.this.status;
                
                if (snapshot.child("presence").getValue() == null) {
                    Friend.this.status = Status.OFFLINE;
                    Friend.this.source = null;
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> presence = (Map<String, Object>) snapshot.child("presence").getValue();
                    
                    if (presence.get("plugin") != null) {
                        Friend.this.status = Status.PLUGIN;
                        Friend.this.source = (String) presence.get("plugin");
                    } else if (presence.get("app") != null) {
                        Friend.this.status = Status.APP;
                        Friend.this.source = (String) presence.get("app");
                    }
                }
                
                if (oldStatus != Friend.this.status) {
                    if (Friend.this.status == Status.PLUGIN) {
                        String pluginId = (String) FriendCraft.sharedInstance.getConfig().getConfigurationSection("authentication").getValues(false).get("id");
                        if (Friend.this.source.equalsIgnoreCase(pluginId)) {
                            Friend.this.list.getOwner().sendMessage(Friend.this.getDisplayName() + ChatColor.YELLOW + " has joined this server.");
                            
                        } else {
                            Firebase pluginNameRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + Friend.this.source + "/status/name");
                            pluginNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                public void onDataChange(DataSnapshot snapshot) {
                                    String pluginName = (String) snapshot.getValue();
                                    Friend.this.list.getOwner().sendMessage(Friend.this.getDisplayName() + ChatColor.YELLOW + " has joined " + pluginName + ".");
                                }
                            });
                        }
                        
                    } else if (Friend.this.status == Status.APP) {
                        Friend.this.list.getOwner().sendMessage(Friend.this.getDisplayName() + ChatColor.YELLOW + " is online via the " + Friend.this.source + " app.");
                    }
                }
            }
        };
        
        this.playerRef.addValueEventListener(playerListener);
    }
    
    public UUID getUniqueId() { return uuid; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public String getSource() { return source; }
    public Firebase getPlayerRef() { return playerRef; }
    
    public String getDisplayName()
    {
        String displayName;

        switch (status) {
        case PLUGIN:
            displayName = ChatColor.GREEN + name;
            break;
        case OFFLINE:
            displayName = ChatColor.GRAY + name;
            break;
        default:
            displayName = ChatColor.WHITE + name;
            break;
        }

        return displayName;
    }
    
    public String[] getAllDisplayNames()
    {
        return new String[] {name, ChatColor.GREEN + name, ChatColor.GRAY + name, ChatColor.WHITE + name};
    }
    
    // TODO move this to some messaging class, maybe the command executor
    /*public void sendMessage(final Player sender, final String message)
    {
        playerRef.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                Friend.this.name = (String) snapshot.child("name").getValue();
                
                if (snapshot.child("presence").getValue() == null) {
                    sender.sendMessage(Friend.this.getDisplayName() + ChatColor.YELLOW + " is currently offline and unable to receive messages.");
                    
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> presence = (Map<String, Object>) snapshot.child("presence").getValue();
                    
                    if (presence.get("plugin") != null) {
                        sender.sendMessage(" " + ChatColor.LIGHT_PURPLE + "=> " + Friend.this.name + ": " + message);
                        
                    } else if (presence.get("app") != null) {
                        // TODO app
                    }
                }
            }
        });
    }*/
    
    public int compareTo(Friend other)
    {
        return this.name.compareToIgnoreCase(other.name);
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (!(obj instanceof Friend)) { return false; }
        
        Friend other = (Friend) obj;
        if (uuid == null) {
            if (other.uuid != null) {
                return false;
            }
        } else if (!uuid.equals(other.uuid)) {
            return false;
        }
        
        return true;
    }

    public void recycle()
    {
        playerRef.removeEventListener(playerListener);
    }
}
