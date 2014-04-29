package com.tomheinan.friendcraft;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthListener;
import com.firebase.client.Firebase.CompletionListener;
import com.firebase.client.FirebaseError;

public class FriendCraft extends JavaPlugin
{
    //public static String firebaseRoot = "https://friendcraft.firebaseio.com"; // production
    public static String firebaseRoot = "https://friendcraft-dev.firebaseio.com"; // development
    
    //public static String authRoot = "https://friendcraft.io"; // production
    public static String authRoot = "http://localhost:3000"; // development
    
    protected static Configuration configuration;
    protected static Logger logger = null;
    
    protected Server server;
    protected File dataFolder;
    protected EventListener eventListener;
    protected Object authData;

    public void onEnable()
    {
        logger = this.getLogger();
        this.server = this.getServer();
        this.dataFolder = this.getDataFolder();
        this.dataFolder.mkdirs();
        
        // plugin configuration shenanigans
        configuration = this.getConfig();
        configuration.options().copyDefaults(true);
        this.saveConfig();
        
        // authenticate with firebase
        Map<String, Object> credentials = configuration.getConfigurationSection("authentication").getValues(false);
        String id = (String) credentials.get("id");
        String secret = (String) credentials.get("secret");
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> jsonPayload = new HashMap<String, String>();
        jsonPayload.put("type", "plugin");
        jsonPayload.put("id", id);
        jsonPayload.put("secret", secret);
        
        StringEntity body = null;
        try {
            body = new StringEntity(mapper.writeValueAsString(jsonPayload));
        } catch (Exception e) {
            error("Unable to serialize FriendCraft plugin credentials", e);
            disable();
            return;
        }
        
        HttpPost postRequest = new HttpPost(authRoot + "/auth");
        postRequest.addHeader("Content-Type", "application/json");
        postRequest.addHeader("Accept", "application/json");
        postRequest.setEntity(body);
        
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            log("Authenticating as \"" + id + "\"");
            response = httpClient.execute(postRequest);
        } catch (Exception e) {
            error("Unable to reach FriendCraft authentication server", e);
            disable();
            return;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        
        if (response.getStatusLine().getStatusCode() != 200) {
            error("Your plugin credentials are incorrect; please check them in config.yml");
            disable();
            return;
        }
        
        Map<String, String> jsonResponse = null;
        try {
            jsonResponse = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<HashMap<String, String>>(){});
        } catch (Exception e) {
            error("Unable to parse FriendCraft authorization API response", e);
            disable();
            return;
        }
        
        String token = jsonResponse.get("token");
        //TODO get the expiry here too so we can set a timer to renew
        
        log("Connecting to Firebase");
        Firebase rootRef = new Firebase(firebaseRoot);
        rootRef.auth(token, new AuthListener() {

            public void onAuthError(FirebaseError err) {
                error("Authentication failed: " + err.getMessage());
                disable();
            }

            public void onAuthRevoked(FirebaseError err) {
                error("Authentication revoked: " + err.getMessage());
                disable();
            }

            public void onAuthSuccess(Object authData) {
                FriendCraft.this.authData = authData;
                
                // register for bukkit events
                if (eventListener == null) { eventListener = new EventListener(); }
                server.getPluginManager().registerEvents(eventListener, FriendCraft.this);
                
                // link commands to their executors
                getCommand("fc").setExecutor(new PrimaryCommandExecutor());
                
                log("Version " + getDescription().getVersion() + " enabled");
            }
            
        });
    }

    public void onDisable()
    {
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
    
    public void disable()
    {
        server.getPluginManager().disablePlugin(this);
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
}
