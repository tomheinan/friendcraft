package com.tomheinan.friendcraft;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class FriendCraft extends JavaPlugin
{
    public static boolean debugMode = false;
    protected static Logger logger = null;
    
    protected Server server;
    protected File dataFolder;
    protected EventListener eventListener;

    public void onEnable()
    {
        logger = this.getLogger();
        this.server = this.getServer();
        this.dataFolder = this.getDataFolder();
        this.dataFolder.mkdirs();
        
        // plugin configuration shenanigans
        Configuration configuration = this.getConfig();
        configuration.options().copyDefaults(true);
        debugMode = configuration.getBoolean("debug");
        int port = configuration.getInt("port", 25566);
        this.saveConfig();
        
        // register for bukkit events
        if (this.eventListener == null) { this.eventListener = new EventListener(); }
        this.server.getPluginManager().registerEvents(this.eventListener, this);
    }

    public void onDisable()
    {
        // unregister events
        HandlerList.unregisterAll(this.eventListener);
        log("Version " + this.getDescription().getVersion() + " disabled");
    }

    public static void log(String msg)
    {
        logger.log(Level.INFO, msg);
    }

    public static void error(String msg)
    {
        logger.log(Level.SEVERE, msg);
    }

    public static void error(String msg, Throwable t)
    {
        logger.log(Level.SEVERE, msg, t);
    }

    public static void debug(String msg)
    {
        if (debugMode)
        {
            log("[debug] " + msg);
        }
    }
}
