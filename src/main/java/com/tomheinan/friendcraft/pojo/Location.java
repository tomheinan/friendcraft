package com.tomheinan.friendcraft.pojo;

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
