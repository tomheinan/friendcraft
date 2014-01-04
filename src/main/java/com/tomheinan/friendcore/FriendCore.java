package com.tomheinan.friendcore;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;

public class FriendCore extends JavaPlugin
{
    protected static Logger logger = null;
    public static boolean debugMode = false;
    
    protected org.bukkit.Server bukkitServer;
    protected org.eclipse.jetty.server.Server webServer;
    protected PluginManager pm;
    protected FriendCoreConfiguration config;
    protected File dataFolder;

    public FriendCore()
    {
    }

    public void onEnable()
    {
        logger = this.getLogger();
        this.bukkitServer = this.getServer();
        this.pm = this.bukkitServer.getPluginManager();
        this.dataFolder = this.getDataFolder();

        this.dataFolder.mkdirs();
        
        Configuration configuration = this.getConfig();
        configuration.options().copyDefaults(true);
        debugMode = configuration.getBoolean("debug");
        this.config = new FriendCoreConfiguration(configuration);
        
        this.saveConfig();

        log("Version " + this.getDescription().getVersion() + " enabled");
        
        this.webServer = new Server(25566);
        this.webServer.setHandler(new FriendCoreWebHandler());
        try {
            this.webServer.start();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void onDisable()
    {
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
