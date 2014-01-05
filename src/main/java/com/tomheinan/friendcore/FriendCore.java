package com.tomheinan.friendcore;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
        
        // plugin configuration shenanigans
        Configuration configuration = this.getConfig();
        configuration.options().copyDefaults(true);
        debugMode = configuration.getBoolean("debug");
        int port = configuration.getInt("port", 25566);
        this.config = new FriendCoreConfiguration(configuration);
        this.saveConfig();
        
        // initialize WebSocket server
        this.webServer = new Server();
        ServerConnector connector = new ServerConnector(this.webServer);
        connector.setPort(port);
        this.webServer.addConnector(connector);
        
        // setup the basic application "context" at "/"
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        this.webServer.setHandler(context);
        
        // add a websocket to a specific path spec
        ServletHolder holder = new ServletHolder("ws-events", FriendCoreServlet.class);
        context.addServlet(holder, "/*");
        
        try {
            log("Starting WebSocket server");
            this.webServer.start();
            log("Listening for incoming connections on port " + Integer.toString(port));
            log("Version " + this.getDescription().getVersion() + " enabled");
        } catch (Exception e) {
            error("Unable to start WebSocket server on port " + Integer.toString(port), e);
        }
    }

    public void onDisable()
    {
        try {
            log("Stopping WebSocket server");
            this.webServer.stop();
        } catch (Exception e) {
            error("Unable to stop WebSocket server normally", e);
        }
        
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
