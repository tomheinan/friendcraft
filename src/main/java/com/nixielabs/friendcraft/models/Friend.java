package com.nixielabs.friendcraft.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
    
    private final UUID uuid;
    private String name;
    private Status status;
    private String source;
    private final Set<FriendsList> lists = Collections.synchronizedSet(new HashSet<FriendsList>());
    
    private final Firebase friendRef;
    private final ValueEventListener friendListener;
    
    public Friend(UUID uuid)
    {
        this.uuid = uuid;
        this.name = "unknown";
        this.status = Status.UNKNOWN;
        
        this.friendRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + uuid.toString());
        this.friendListener = new ValueEventListener() {

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
                
                synchronized(lists) {
                    Iterator<FriendsList> it = lists.iterator();
                    while (it.hasNext()) {
                        final FriendsList list = it.next();
                        list.render();
                        
                        if (oldStatus != Friend.this.status) {
                            if (Friend.this.status == Status.PLUGIN) {
                                String pluginId = (String) FriendCraft.sharedInstance.getConfig().getConfigurationSection("authentication").getValues(false).get("id");
                                if (Friend.this.source.equalsIgnoreCase(pluginId)) {
                                    list.notify(Friend.this.getDisplayName() + ChatColor.YELLOW + " has joined this server.");
                                } else {
                                    Firebase pluginNameRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + Friend.this.source + "/status/name");
                                    pluginNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                        public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                        public void onDataChange(DataSnapshot snapshot) {
                                            String pluginName = (String) snapshot.getValue();
                                            list.notify(Friend.this.getDisplayName() + ChatColor.YELLOW + " has joined " + pluginName + ".");
                                        }
                                    });
                                }
                                
                            } else if (Friend.this.status == Status.APP) {
                                list.notify(Friend.this.getDisplayName() + ChatColor.YELLOW + " is online via the " + Friend.this.source + " app.");
                            }
                        }
                    }
                }
            }
        };
        
        friendRef.addValueEventListener(friendListener);
    }
    
    public void addToList(FriendsList list)
    {
        synchronized(lists) {
            lists.add(list);
        }
    }
    
    public void removeFromList(FriendsList list)
    {
        synchronized(lists) {
            lists.remove(list);
        }
    }
    
    public boolean belongsToList(FriendsList list)
    {
        synchronized(lists) {
            return lists.contains(list);
        }
    }
    
    public UUID getUUID() { return uuid; }
    public String getName() { return name; }
    public Status getStatus() { return status; }
    public String getSource() { return source; }
    public Firebase getFriendRef() { return friendRef; }
    
    public void sendMessage(final Player sender, final String message)
    {
        friendRef.addListenerForSingleValueEvent(new ValueEventListener() {

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
    }
    
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
    
    public int compareTo(Friend other)
    {
        return this.name.compareToIgnoreCase(other.name);
    }
    
    public void unlink()
    {
        friendRef.removeEventListener(friendListener);
    }
}
