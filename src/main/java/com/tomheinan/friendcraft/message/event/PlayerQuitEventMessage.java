package com.tomheinan.friendcraft.message.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

import com.tomheinan.friendcraft.message.EventMessage;
import com.tomheinan.friendcraft.pojo.Location;

public class PlayerQuitEventMessage extends EventMessage
{
    protected String playerName;
    protected Location location;
    
    public PlayerQuitEventMessage()
    {
        super();
        this.type = "playerQuit";
    }
    
    public PlayerQuitEventMessage(PlayerQuitEvent event)
    {
        this();
        Player player = event.getPlayer();
        this.playerName = player.getName();
    }
}
