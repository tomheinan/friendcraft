package com.tomheinan.friendcraft.message.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import com.tomheinan.friendcraft.message.EventMessage;
import com.tomheinan.friendcraft.pojo.Location;

public class PlayerJoinEventMessage extends EventMessage
{
    protected String playerName;
    protected Location location;
    
    public PlayerJoinEventMessage()
    {
        super();
        this.type = "playerJoin";
    }
    
    public PlayerJoinEventMessage(PlayerJoinEvent event)
    {
        this();
        Player player = event.getPlayer();
        this.playerName = player.getName();
        this.location = new Location(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), player.getLocation().getPitch(), player.getLocation().getYaw(), player.getWorld().getName());
    }
}
