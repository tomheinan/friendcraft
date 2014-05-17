package com.tomheinan.friendcraft;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthListener;
import com.firebase.client.Firebase.CompletionListener;
import com.firebase.client.FirebaseError;

public class FriendCraft extends JavaPlugin {
    // public static String firebaseRoot = "https://friendcraft.firebaseio.com"; // production
    public static String firebaseRoot = "https://friendcraft-dev.firebaseio.com"; // development

    // public static String authRoot = "https://friendcraft.herokuapp.com"; // production
    public static String authRoot = "http://localhost:3000"; // development

    protected static Configuration configuration;
    protected static Logger logger = null;

    protected Server server;
    protected File dataFolder;
    protected EventListener eventListener;
    protected Object authData;
    public static FriendCraft sharedInstance;

    public void onEnable() {
        if (sharedInstance == null) {
            sharedInstance = this;
        }

        logger = this.getLogger();
        this.server = this.getServer();
        this.dataFolder = this.getDataFolder();
        this.dataFolder.mkdirs();

        // plugin configuration shenanigans
        configuration = this.getConfig();
        configuration.options().copyDefaults(true);
        this.saveConfig();
        
        // set up firebase auth listener
        AuthListener authListener = new AuthListener() {

            public void onAuthError(FirebaseError err) {
                error("Authentication failed: " + err.getMessage());
                disable();
            }

            public void onAuthRevoked(FirebaseError err) {
                warn("Authentication revoked: " + err.getMessage());
                AuthTask authTask = new AuthTask(this);
                authTask.runTaskAsynchronously(FriendCraft.sharedInstance);
            }

            public void onAuthSuccess(Object authData) {
                FriendCraft.sharedInstance.authData = authData;
                
                // register for bukkit events
                if (eventListener == null) { eventListener = new EventListener(); }
                server.getPluginManager().registerEvents(eventListener, FriendCraft.sharedInstance);
                
                // link commands to their executors
                getCommand("fc").setExecutor(new PrimaryCommandExecutor());
                
                log("Version " + getDescription().getVersion() + " enabled");
            }  
        };
        
        AuthTask authTask = new AuthTask(authListener);
        authTask.runTaskAsynchronously(this);
    }

    public void onDisable() {
        // deregister events
        HandlerList.unregisterAll(eventListener);

        // disconnect from firebase
        if (authData != null) {
            log("Disconnecting from Firebase");
            Firebase rootRef = new Firebase(firebaseRoot);

            // note: this is currently throwing a ConcurrentModificationException in 1.7.2
            // i don't know why, but let's just let it do so...
            rootRef.unauth(new CompletionListener() {

                public void onComplete(FirebaseError err, Firebase ref) {
                    log("Version " + getDescription().getVersion() + " disabled");
                }
            });
        } else {
            log("Version " + getDescription().getVersion() + " disabled");
        }
    }

    public void disable() {
        server.getPluginManager().disablePlugin(this);
    }

    public static void log(String msg) { logger.log(Level.INFO, msg); }
    public static void warn(String msg) { logger.log(Level.WARNING, msg); }
    public static void error(String msg) { logger.log(Level.SEVERE, msg); }
    public static void error(String msg, Throwable t) { logger.log(Level.SEVERE, msg, t); }
}
