package com.nixielabs.friendcraft.eventlisteners;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nixielabs.friendcraft.callbacks.PlayerDeregistrationCallback;
import com.nixielabs.friendcraft.callbacks.PlayerRegistrationCallback;
import com.nixielabs.friendcraft.managers.FriendsListManager;
import com.nixielabs.friendcraft.managers.PlayerManager;
import com.nixielabs.friendcraft.managers.PresenceManager;

public final class PluginEventListener implements Listener
{
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        PlayerManager.sharedInstance.register(player, new PlayerRegistrationCallback() {
            
            public void onRegistration(Player player, UUID uuid) {
                PresenceManager.noteArrival(player, uuid);
                FriendsListManager.sharedInstance.pin(player, uuid);
            }
        });
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        final Player player = event.getPlayer();
        PlayerManager.sharedInstance.deregister(player, new PlayerDeregistrationCallback() {
            
            public void onDeregistration(UUID uuid) {
                PresenceManager.noteDeparture(uuid);
                FriendsListManager.sharedInstance.unpin(uuid);
            }
        });
    }
}
