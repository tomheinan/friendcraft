package com.tomheinan.friendcraft;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthListener;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.tomheinan.friendcraft.callbacks.PlayerDeregistrationCallback;
import com.tomheinan.friendcraft.callbacks.PlayerRefCallback;
import com.tomheinan.friendcraft.callbacks.PlayerRegistrationCallback;
import com.tomheinan.friendcraft.callbacks.UUIDLookupCallback;
import com.tomheinan.friendcraft.commandexecutors.FriendCraftCommandExecutor;
import com.tomheinan.friendcraft.commandexecutors.MessagingCommandExecutor;
import com.tomheinan.friendcraft.eventlisteners.PluginEventListener;
import com.tomheinan.friendcraft.managers.FriendsListManager;
import com.tomheinan.friendcraft.managers.MessagingManager;
import com.tomheinan.friendcraft.managers.PlayerManager;
import com.tomheinan.friendcraft.managers.PresenceManager;
import com.tomheinan.friendcraft.tasks.AuthTask;
import com.tomheinan.friendcraft.tasks.PlayerCleanupTask;
import com.tomheinan.friendcraft.tasks.UUIDLookupTask;

public class FriendCraft extends JavaPlugin {
    //public static String firebaseRoot = "https://friendcraft.firebaseio.com"; // production
    public static String firebaseRoot = "https://friendcraft-dev.firebaseio.com"; // development

    //public static String authRoot = "https://friendcraft.herokuapp.com"; // production
    public static String authRoot = "http://localhost:3000"; // development

    protected static Configuration configuration;
    protected static Logger logger = null;
    protected static int cleanUpInterval = 60; // seconds

    protected Server server;
    protected File dataFolder;
    protected PluginEventListener eventListener;
    protected Object authData;
    protected PlayerCleanupTask playerCleanupTask;
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
                
                // occasionally clean up ghosts
                playerCleanupTask = new PlayerCleanupTask();
                playerCleanupTask.runTaskTimerAsynchronously(FriendCraft.sharedInstance, 20 * cleanUpInterval, 20 * cleanUpInterval);
                
                // link commands to their executors
                getCommand("fc").setExecutor(new FriendCraftCommandExecutor());
                getCommand("msg").setExecutor(new MessagingCommandExecutor());
                getCommand("r").setExecutor(new MessagingCommandExecutor());
                
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
        // stop cleanup task
        playerCleanupTask.cancel();
        
        // deregister events
        HandlerList.unregisterAll(eventListener);
        
        // deregister friends lists for all currently online players
        PlayerManager.sharedInstance.deregisterAll(new PlayerDeregistrationCallback() {
            
            public void onDeregistration(UUID uuid) {
                PresenceManager.noteDeparture(uuid);
                FriendsListManager.sharedInstance.unpin(uuid);
                MessagingManager.sharedInstance.unpin(uuid);
            }
        });

        // disconnect from firebase
        log("Disconnecting from Firebase");
        
        String pluginId = (String) configuration.get("authentication.id");
        Firebase pluginUpRef = new Firebase(firebaseRoot + "/plugins/" + pluginId + "/status/connected");
        pluginUpRef.setValue(Boolean.FALSE);
        
        log("Version " + getDescription().getVersion() + " disabled");
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
                    List<String> friendNames = new ArrayList<String>();
                    friendNames.add(playerName);
                    
                    UUIDLookupTask uuidTask = new UUIDLookupTask(friendNames, new UUIDLookupCallback() {
                        
                        public void onResult(Map<String, UUID> result) {
                            Set<Entry<String, UUID>> entries = result.entrySet();
                            if (entries.size() == 0) {
                                callback.onNotFound();
                                
                            } else {
                                Iterator<Entry<String, UUID>> it = entries.iterator();
                                Entry<String, UUID> entry = it.next();
                                
                                final String name = entry.getKey();
                                final UUID uuid = entry.getValue();
                                
                                Firebase playerRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + uuid.toString());
                                playerRef.child("name").setValue(name, new Firebase.CompletionListener() {

                                    public void onComplete(FirebaseError error, Firebase ref) {
                                        if (error == null) {
                                            // index this player by name
                                            Firebase indexPlayerByNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name");
                                            indexPlayerByNameRef.child(name.toLowerCase()).setValue(uuid.toString());
                                            
                                            getPlayerRef(uuid, callback);
                                            
                                        } else {
                                            FriendCraft.error(error.getMessage());
                                        }
                                    }
                                });
                            }
                        }
                    });
                    
                    uuidTask.runTaskAsynchronously(FriendCraft.sharedInstance);
                    
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
