package com.nixielabs.friendcraft.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.firebase.client.Firebase;
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.PlayerRegistrationCallback;
import com.nixielabs.friendcraft.callbacks.UUIDCallback;
import com.nixielabs.friendcraft.tasks.UUIDTask;

public class PlayerManager
{
    public final static PlayerManager sharedInstance = new PlayerManager();
    
    private final Map<UUID, UUID> ids = Collections.synchronizedMap(new HashMap<UUID, UUID>());
    
    private PlayerManager() { /* restrict direct instantiation */ }
    
    public void register(final Player player, PlayerRegistrationCallback callback)
    {
        UUIDCallback uuidCallback = new UUIDCallback() {
            
            public void onFound(UUID uuid) {
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
                
                final String pluginId = (String) FriendCraft.sharedInstance.getConfig().get("authentication.id");
                final Firebase playerRef = getPlayerRef(player);
                final Firebase pluginPlayersRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + pluginId + "/players");
                
                
            }
            
            public void onNotFound() {
                FriendCraft.error("Canonical UUID not found for player " + player.getName());
            }
        };
        
        UUIDTask uuidTask = new UUIDTask(player, uuidCallback);
        uuidTask.runTaskAsynchronously(FriendCraft.sharedInstance);
    }
    
    public Firebase getPlayerRef(Player player)
    {
        synchronized(ids) {
            UUID uuid = ids.get(player.getUniqueId());
            if (uuid != null) {
                return new Firebase(FriendCraft.firebaseRoot + "/players/" + uuid.toString());
            }
        }
        
        return null;
    }
}
