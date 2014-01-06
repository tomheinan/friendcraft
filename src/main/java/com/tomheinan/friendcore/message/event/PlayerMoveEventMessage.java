package com.tomheinan.friendcore.message.event;

import org.bukkit.event.player.PlayerMoveEvent;

import com.tomheinan.friendcore.message.EventMessage;

public class PlayerMoveEventMessage extends EventMessage
{
    protected String player;
    protected Location oldLocation;
    protected Location newLocation;
    
    public PlayerMoveEventMessage()
    {
        super();
        this.type = "playerMove";
    }
    
    public PlayerMoveEventMessage(PlayerMoveEvent event)
    {
        this();
        this.player = event.getPlayer().getName();
        this.oldLocation = new Location(event.getFrom().getX(), event.getFrom().getY(), event.getFrom().getZ(), event.getFrom().getPitch(), event.getFrom().getYaw(), event.getFrom().getWorld().getName());
        this.newLocation = new Location(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getPitch(), event.getTo().getYaw(), event.getTo().getWorld().getName());
    }
    
    public class Location
    {
        protected double x;
        protected double y;
        protected double z;
        protected float pitch;
        protected float yaw;
        protected String world;
        
        public Location() {}
        
        public Location(double x, double y, double z, float pitch, float yaw, String world)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pitch = pitch;
            this.yaw = yaw;
            this.world = world;
        }
    }
}
