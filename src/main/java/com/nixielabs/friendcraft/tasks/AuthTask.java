package com.nixielabs.friendcraft.tasks;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bukkit.scheduler.BukkitRunnable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthListener;
import com.nixielabs.friendcraft.FriendCraft;

public class AuthTask extends BukkitRunnable
{
    private final AuthListener authListener;
    
    public AuthTask(AuthListener authListener)
    {
        this.authListener = authListener;
    }
    
    public void run()
    {
        // authenticate with firebase via the friendcraft auth server
        Map<String, Object> credentials = FriendCraft.sharedInstance.getConfig().getConfigurationSection("authentication").getValues(false);
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
            FriendCraft.error("Unable to serialize FriendCraft plugin credentials", e);
            FriendCraft.sharedInstance.disable();
            return;
        }
        
        HttpPost postRequest = new HttpPost(FriendCraft.authRoot + "/auth");
        postRequest.addHeader("Content-Type", "application/json");
        postRequest.addHeader("Accept", "application/json");
        postRequest.setEntity(body);
        
        HttpClient httpClient = new DefaultHttpClient();
        String token = null;
        try {
            FriendCraft.log("Authenticating as \"" + id + "\"");
            HttpResponse response = httpClient.execute(postRequest);
            
            if (response.getStatusLine().getStatusCode() != 200) {
                FriendCraft.error("Your plugin credentials are incorrect; please check them in config.yml");
                FriendCraft.sharedInstance.disable();
                return;
            }
            
            Map<String, String> jsonResponse = null;
            try {
                jsonResponse = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<HashMap<String, String>>(){});
            } catch (Exception e) {
                FriendCraft.error("Unable to parse FriendCraft authorization API response", e);
                FriendCraft.sharedInstance.disable();
                return;
            }
            
            // this is our actual firebase auth token
            token = jsonResponse.get("token");
            
        } catch (Exception e) {
            FriendCraft.error("Unable to reach FriendCraft authentication server", e);
            FriendCraft.sharedInstance.disable();
            return;
            
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        
        FriendCraft.log("Connecting to Firebase");
        Firebase rootRef = new Firebase(FriendCraft.firebaseRoot);
        rootRef.auth(token, authListener);
    }
}
