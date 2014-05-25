package com.nixielabs.friendcraft.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nixielabs.friendcraft.models.FriendsList;

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
    
    public void registerAll()
    {
        Player[] players = Bukkit.getServer().getOnlinePlayers();
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            register(player);
        }
    }
    
    public void deregisterAll()
    {
        synchronized(lists) {
            Collection<FriendsList> friendsLists = lists.values();
            Iterator<FriendsList> it = friendsLists.iterator();
            
            while (it.hasNext()) {
                FriendsList list = it.next();
                list.hideSidebar();
                list.unlink();
            }
            
            lists.clear();
        }
    }
    
    public FriendsList getListForPlayer(Player player)
    {
        synchronized(lists) {
            return lists.get(player.getUniqueId());
        }
    }
}
