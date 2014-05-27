package com.nixielabs.friendcraft.tasks;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bukkit.scheduler.BukkitRunnable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.UUIDCallback;

public class UUIDTask extends BukkitRunnable
{
    private static final String apiUrl = "https://api.mojang.com/profiles/minecraft";
    private final List<String> playerNames;
    private final UUIDCallback callback;
    
    public UUIDTask(List<String> playerNames, UUIDCallback callback)
    {
        this.playerNames = playerNames;
        this.callback = callback;
    }
    
    public void run()
    {
        ObjectMapper mapper = new ObjectMapper();
        StringEntity body = null;
        try {
            body = new StringEntity(mapper.writeValueAsString(playerNames));
        } catch (Exception e) {
            FriendCraft.error("Unable to serialize player name for canonical UUID request", e);
            callback.onResult(new HashMap<String, UUID>());
            return;
        }
        
        HttpPost postRequest = new HttpPost(apiUrl);
        postRequest.addHeader("Content-Type", "application/json");
        postRequest.addHeader("Accept", "application/json");
        postRequest.setEntity(body);
        
        HttpClient httpClient = new DefaultHttpClient();
        List<Map<String, String>> jsonResponse = null;
        try {
            HttpResponse response = httpClient.execute(postRequest);
            
            if (response.getStatusLine().getStatusCode() != 200) {
                FriendCraft.error("Profile API error: " + response.getStatusLine().toString());
                callback.onResult(new HashMap<String, UUID>());
                return;
            }
            
            try {
                jsonResponse = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<Map<String, String>>>(){});
            } catch (Exception e) {
                FriendCraft.error("Unable to parse Mojang Profile API response", e);
                callback.onResult(new HashMap<String, UUID>());
                return;
            }
            
        } catch (Exception e) {
            FriendCraft.error("Unable to reach Mojang Profile API", e);
            callback.onResult(new HashMap<String, UUID>());
            return;
            
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        
        // build the uuid map
        Map<String, UUID> uuids = new HashMap<String, UUID>();
        
        Iterator<Map<String, String>> it = jsonResponse.iterator();
        while (it.hasNext()) {
            Map<String, String> profileData = it.next();
            String playerName = profileData.get("name");
            String playerId = profileData.get("id");
            
            if (playerId != null) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerId);
                } catch (Exception e) {
                    uuid = new UUID(
                        new BigInteger(playerId.substring(0, 16), 16).longValue(),
                        new BigInteger(playerId.substring(16), 16).longValue()
                    );
                }
                
                if (playerName != null) {
                    uuids.put(playerName, uuid);
                }
            }
        }
        
        callback.onResult(uuids);
    }
}
