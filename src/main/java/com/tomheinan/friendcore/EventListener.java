package com.tomheinan.friendcore;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.tomheinan.friendcore.message.event.PlayerMoveEventMessage;

public final class EventListener implements Listener
{   
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        PlayerMoveEventMessage message = new PlayerMoveEventMessage(event);
        MessageDispatcher.broadcast(message);
    }
}
