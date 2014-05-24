package com.nixielabs.friendcraft.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
        OFFLINE, PLUGIN, APP
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
        friendRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + uuid.toString());
        friendListener = new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                Friend.this.name = (String) snapshot.child("name").getValue();
                
                Status oldStatus = Friend.this.status;
                Status newStatus = Status.OFFLINE;
                if (snapshot.child("presence").getValue() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> presence = (Map<String, Object>) snapshot.child("presence").getValue();
                    
                    if (presence.get("plugin") != null) {
                        newStatus = Status.PLUGIN;
                        Friend.this.source = (String) presence.get("plugin");
                    } else if (presence.get("app") != null) {
                        newStatus = Status.APP;
                        Friend.this.source = "FriendCraft";
                    } else {
                        Friend.this.source = null;
                    }
                }
                
                if (oldStatus != newStatus) {
                    Friend.this.status = newStatus;
                    
                    synchronized(lists) {
                        Iterator<FriendsList> it = lists.iterator();
                        while (it.hasNext()) {
                            final FriendsList list = it.next();
                            list.render();
                            
                            if (newStatus == Status.PLUGIN) {
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
                                
                            } else if (newStatus == Status.APP) {
                                list.notify(Friend.this.getDisplayName() + ChatColor.YELLOW + " is online via the " + Friend.this.source + " app.");
                            }
                        }
                    }
                }
            }
        };
        
        name = uuid.toString();
        status = Status.OFFLINE;
        
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
    
    public UUID getUUID()
    {
        return uuid;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Status getStatus()
    {
        return status;
    }
    
    public String getSource()
    {
        return source;
    }
    
    public String getDisplayName()
    {
        String displayName;

        switch (status) {
        case PLUGIN:
            displayName = ChatColor.GREEN + name;
            break;
        default:
            displayName = ChatColor.GRAY + name;
            break;
        }

        return displayName;
    }
    
    public String[] getAllDisplayNames()
    {
        return new String[] {name, ChatColor.GREEN + name, ChatColor.GRAY + name};
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
