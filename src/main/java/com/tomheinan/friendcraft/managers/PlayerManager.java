package com.tomheinan.friendcraft.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.callbacks.PlayerDeregistrationCallback;
import com.tomheinan.friendcraft.callbacks.PlayerRegistrationCallback;
import com.tomheinan.friendcraft.callbacks.UUIDLookupCallback;
import com.tomheinan.friendcraft.tasks.UUIDLookupTask;

public class PlayerManager
{
    public final static PlayerManager sharedInstance = new PlayerManager();
    
    private final Map<UUID, UUID> ids = Collections.synchronizedMap(new HashMap<UUID, UUID>());
    
    private PlayerManager() { /* restrict direct instantiation */ }
    
    public void register(Player player, PlayerRegistrationCallback callback)
    {
        List<Player> playerList = new ArrayList<Player>();
        playerList.add(player);
        
        lookupPlayers(playerList, callback);
    }
    
    public void registerAll(PlayerRegistrationCallback callback)
    {
        List<Player> playerList = Arrays.asList(Bukkit.getServer().getOnlinePlayers());
        lookupPlayers(playerList, callback);
    }
    
    public void deregister(Player player, PlayerDeregistrationCallback callback)
    {
        UUID uuid = null;
        synchronized(ids) {
            uuid = ids.remove(player.getUniqueId());
        }
        
        if (uuid != null) {
            callback.onDeregistration(uuid);
        }
    }
    
    public void deregister(UUID uuid, PlayerDeregistrationCallback callback)
    {
        synchronized(ids) {
            uuid = ids.remove(uuid);
        }
        
        if (uuid != null) {
            callback.onDeregistration(uuid);
        }
    }
    
    public void deregisterAll(PlayerDeregistrationCallback callback)
    {
        synchronized(ids) {
            Set<Entry<UUID, UUID>> entries = ids.entrySet();
            Iterator<Entry<UUID, UUID>> it = entries.iterator();
            
            while (it.hasNext()) {
                Entry<UUID, UUID> entry = it.next();
                callback.onDeregistration(entry.getValue());
            }
            
            ids.clear();
        }
    }
    
    public UUID getUUID(Player player)
    {
        UUID uuid = null;
        synchronized(ids) {
            uuid = ids.get(player.getUniqueId());
        }
        
        return uuid;
    }
    
    public Set<UUID> getLocalUUIDs()
    {
        Set<UUID> localUUIDs = new HashSet<UUID>();
        
        synchronized(ids) {
            Collection<UUID> col = ids.values();
            Iterator<UUID> it = col.iterator();
            
            while (it.hasNext()) {
                localUUIDs.add(it.next());
            }
        }
        
        return localUUIDs;
    }
    
    private void lookupPlayers(final List<Player> players, final PlayerRegistrationCallback callback)
    {
        List<String> playerNames = new ArrayList<String>();
        Iterator<Player> itOuter = players.iterator();
        
        while (itOuter.hasNext()) {
            Player player = itOuter.next();
            playerNames.add(player.getName());
        }
        
        UUIDLookupCallback uuidCallback = new UUIDLookupCallback() {
            
            public void onResult(Map<String, UUID> result) {
                Iterator<Player> itInner = players.iterator();
                while (itInner.hasNext()) {
                    Player player = itInner.next();
                    UUID uuid = result.get(player.getName());
                    
                    if (uuid != null) {
                        synchronized(ids) {
                            ids.put(player.getUniqueId(), uuid);
                        }
                        
                        if (!uuid.equals(player.getUniqueId())) {
                            FriendCraft.warn(
                                player.getName() + "'s server-issued UUID (" +
                                player.getUniqueId().toString() +
                                ") does not match his/her canonical UUID (" +
                                uuid.toString() + "). This is probably the result of a misconfigured server " +
                                "and may result in unexpected behaviour."
                            );
                        }
                        
                        callback.onRegistration(player, uuid);
                        
                    } else {
                        FriendCraft.warn(
                            "Canonical UUID not found for " + player.getName() + ". " +
                            "FriendCraft functionality will be unavailable for this player."
                        );
                    }
                }
            }
        };
        
        UUIDLookupTask uuidTask = new UUIDLookupTask(playerNames, uuidCallback);
        uuidTask.runTaskAsynchronously(FriendCraft.sharedInstance);
    }
}
