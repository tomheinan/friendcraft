package com.tomheinan.friendcraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.tomheinan.friendcraft.models.FriendsList;

public class FriendsListManager
{
    public final static FriendsListManager sharedInstance = new FriendsListManager();
    
    private final Map<UUID, FriendsList> lists = Collections.synchronizedMap(new HashMap<UUID, FriendsList>());
    
    private FriendsListManager() { /* restrict direct instantiation */ }
    
    public void register(Player player)
    {
        if (getListForPlayer(player) == null) {
            synchronized(lists) {
                FriendsList list = new FriendsList(player);
                lists.put(player.getUniqueId(), list);
            }
        }
    }
    
    public void deregister(Player player)
    {
        FriendsList list;
        synchronized(lists) {
            list = lists.remove(player.getUniqueId());
        }
        
        if (list != null) {
            list.unlink();
        }
    }
    
    public FriendsList getListForPlayer(Player player)
    {
        synchronized(lists) {
            return lists.get(player.getUniqueId());
        }
    }
}
