package com.nixielabs.friendcraft;

import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthListener;
import com.firebase.client.Firebase.CompletionListener;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.nixielabs.friendcraft.callbacks.PlayerDeregistrationCallback;
import com.nixielabs.friendcraft.callbacks.PlayerRefCallback;
import com.nixielabs.friendcraft.callbacks.PlayerRegistrationCallback;
import com.nixielabs.friendcraft.commandexecutors.MessagingCommandExecutor;
import com.nixielabs.friendcraft.commandexecutors.FriendCraftCommandExecutor;
import com.nixielabs.friendcraft.eventlisteners.PluginEventListener;
import com.nixielabs.friendcraft.managers.FriendsListManager;
import com.nixielabs.friendcraft.managers.PlayerManager;
import com.nixielabs.friendcraft.managers.PresenceManager;
import com.nixielabs.friendcraft.tasks.AuthTask;

public class FriendCraft extends JavaPlugin {
    //public static String firebaseRoot = "https://friendcraft.firebaseio.com"; // production
    public static String firebaseRoot = "https://friendcraft-dev.firebaseio.com"; // development

    //public static String authRoot = "https://friendcraft.herokuapp.com"; // production
    public static String authRoot = "http://localhost:3000"; // development

    protected static Configuration configuration;
    protected static Logger logger = null;

    protected Server server;
    protected File dataFolder;
    protected PluginEventListener eventListener;
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
                
                // register all currently online players
                PlayerManager.sharedInstance.registerAll(new PlayerRegistrationCallback() {
                    
                    public void onRegistration(Player player, UUID uuid) {
                        PresenceManager.noteArrival(player, uuid);
                        FriendsListManager.sharedInstance.pin(player, uuid);
                    }
                });
                
                // register for bukkit events
                if (eventListener == null) { eventListener = new PluginEventListener(); }
                server.getPluginManager().registerEvents(eventListener, FriendCraft.sharedInstance);
                
                // link commands to their executors
                getCommand("fc").setExecutor(new FriendCraftCommandExecutor());
                //getCommand("msg").setExecutor(new MessagingCommandExecutor());
                
                // set up a disconnection callback
                Firebase connectionRef = new Firebase(FriendCraft.firebaseRoot + "/.info/connected");
                connectionRef.addListenerForSingleValueEvent(new ValueEventListener() {

                    public void onCancelled(FirebaseError error) { error(error.getMessage()); }

                    public void onDataChange(DataSnapshot snapshot) {
                        boolean connected = snapshot.getValue(Boolean.class);
                        
                        if (connected) {
                            String pluginId = (String) configuration.get("authentication.id");
                            Firebase pluginStatusRef = new Firebase(firebaseRoot + "/plugins/" + pluginId + "/status");
                            
                            pluginStatusRef.child("connected").setValue(Boolean.TRUE);
                            pluginStatusRef.child("connected").onDisconnect().setValue(Boolean.FALSE);
                            
                            pluginStatusRef.child("versions").child("minecraft").setValue(Bukkit.getServer().getBukkitVersion());
                            pluginStatusRef.child("versions").child("plugin").setValue(getDescription().getVersion());
                            pluginStatusRef.child("motd").setValue(Bukkit.getServer().getMotd());
                            
                            String serverName = configuration.getString("name");
                            if (serverName.equalsIgnoreCase("a friendly name for your server here")) {
                                pluginStatusRef.child("name").setValue(configuration.getConfigurationSection("authentication").getValues(false).get("id"));
                            } else {
                                pluginStatusRef.child("name").setValue(serverName);
                            }
                        }
                    }
                });
                
                log("Version " + getDescription().getVersion() + " enabled");
            }  
        };
        
        AuthTask authTask = new AuthTask(authListener);
        authTask.runTaskAsynchronously(this);
    }

    public void onDisable() {
        // deregister events
        HandlerList.unregisterAll(eventListener);
        
        // deregister friends lists for all currently online players
        PlayerManager.sharedInstance.deregisterAll(new PlayerDeregistrationCallback() {
            
            public void onDeregistration(UUID uuid) {
                PresenceManager.noteDeparture(uuid);
                FriendsListManager.sharedInstance.unpin(uuid);
            }
        });

        // disconnect from firebase
        if (authData != null) {
            log("Disconnecting from Firebase");
            
            String pluginId = (String) configuration.get("authentication.id");
            Firebase pluginUpRef = new Firebase(firebaseRoot + "/plugins/" + pluginId + "/status/connected");
            pluginUpRef.setValue(Boolean.FALSE);

            // note: this is currently throwing a ConcurrentModificationException in 1.7.9
            // i don't know why, but let's just let it do so...
            Firebase rootRef = new Firebase(firebaseRoot);
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
    
    public static void getPlayerRef(final UUID playerId, final PlayerRefCallback callback)
    {
        Firebase playerRef = new Firebase(firebaseRoot + "/players/" + playerId.toString());
        playerRef.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    callback.onNotFound();
                } else {
                    String playerName = (String) snapshot.child("name").getValue();
                    callback.onFound(snapshot.getRef(), playerId, playerName);
                }
            }
            
        });
    }
    
    public static void getPlayerRef(final String playerName, final PlayerRefCallback callback)
    {
        Firebase playerIdFromNameRef = new Firebase(firebaseRoot + "/index/players/by-name/" + playerName.toLowerCase());
        playerIdFromNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    callback.onNotFound();
                } else {
                    UUID playerId = UUID.fromString((String) snapshot.getValue());
                    getPlayerRef(playerId, callback);
                }
            }
        });
    }

    public static void log(String msg) { logger.log(Level.INFO, msg); }
    public static void warn(String msg) { logger.log(Level.WARNING, msg); }
    public static void error(String msg) { logger.log(Level.SEVERE, msg); }
    public static void error(String msg, Throwable t) { logger.log(Level.SEVERE, msg, t); }
}
