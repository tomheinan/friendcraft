package com.nixielabs.friendcraft.tasks;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.UUIDCallback;

public class UUIDTask extends BukkitRunnable
{
    private static final String apiUrl = "https://api.mojang.com/profiles/minecraft";
    private final Player player;
    private final UUIDCallback callback;
    
    public UUIDTask(Player player, UUIDCallback callback)
    {
        this.player = player;
        this.callback = callback;
    }
    
    public void run()
    {
        ObjectMapper mapper = new ObjectMapper();
        List<String> jsonPayload = new ArrayList<String>();
        jsonPayload.add(player.getName());
        
        StringEntity body = null;
        try {
            body = new StringEntity(mapper.writeValueAsString(jsonPayload));
        } catch (Exception e) {
            FriendCraft.error("Unable to serialize player name for canonical UUID request", e);
            callback.onNotFound();
            return;
        }
        
        HttpPost postRequest = new HttpPost(apiUrl);
        postRequest.addHeader("Content-Type", "application/json");
        postRequest.addHeader("Accept", "application/json");
        postRequest.setEntity(body);
        
        HttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;
        try {
            response = httpClient.execute(postRequest);
        } catch (Exception e) {
            FriendCraft.error("Unable to reach Mojang Profile API", e);
            httpClient.getConnectionManager().shutdown();
            callback.onNotFound();
            return;
        }
        
        if (response.getStatusLine().getStatusCode() != 200) {
            FriendCraft.error("Unknown Profile API error");
            callback.onNotFound();
            return;
        }
        
        List<Map<String, String>> jsonResponse = null;
        try {
            jsonResponse = mapper.readValue(EntityUtils.toString(response.getEntity()), new TypeReference<List<Map<String, String>>>(){});
        } catch (Exception e) {
            FriendCraft.error("Unable to parse Mojang Profile API response", e);
            callback.onNotFound();
            return;
        }
        
        if (jsonResponse.size() == 0) {
            FriendCraft.warn(player.getName() + " does not have a Mojang account. Possible hacked client?");
            callback.onNotFound();
        } else {
            Map<String, String> profileData = jsonResponse.get(0);
            String idString = profileData.get("id");
            
            if (idString == null) {
                callback.onNotFound();
            } else {
                UUID uuid;
                try {
                    uuid = UUID.fromString(idString);
                } catch (Exception e) {
                    uuid = new UUID(
                        new BigInteger(idString.substring(0, 16), 16).longValue(),
                        new BigInteger(idString.substring(16), 16).longValue()
                    );
                }
                
                callback.onFound(uuid);
            }
        }
        
        httpClient.getConnectionManager().shutdown();
    }
}
