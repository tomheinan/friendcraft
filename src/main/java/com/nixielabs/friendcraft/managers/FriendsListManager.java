package com.nixielabs.friendcraft.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.nixielabs.friendcraft.models.FriendsList;

public class FriendsListManager
{
    public final static FriendsListManager sharedInstance = new FriendsListManager();
    
    private final Map<UUID, FriendsList> lists = Collections.synchronizedMap(new HashMap<UUID, FriendsList>());
    
    private FriendsListManager() { /* restrict direct instantiation */ }
    
    public void pin(Player player, UUID uuid)
    {
        synchronized(lists) {
            if (lists.get(uuid) == null) {
                lists.put(uuid, new FriendsList(player));
            }
        }
    }
    
    public void unpin(UUID uuid)
    {
        FriendsList list = null;
        synchronized(lists) {
            list = lists.remove(uuid);
        }
        
        if (list != null) {
            list.recycle();
        }
    }
    
    public FriendsList getList(Player player) {
        UUID uuid = PlayerManager.sharedInstance.getUUID(player);
        FriendsList list = null;
        
        if (uuid != null) {
            synchronized(lists) {
                list = lists.get(uuid);
            }
        }
        
        return list;
    }
}
