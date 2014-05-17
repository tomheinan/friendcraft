package com.tomheinan.friendcraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.tomheinan.friendcraft.callbacks.FriendLookupCallback;
import com.tomheinan.friendcraft.models.Friend;
import com.tomheinan.friendcraft.models.FriendsList;

public class FriendManager
{
    public final static FriendManager sharedInstance = new FriendManager();
    
    private final Map<UUID, Friend> friends = Collections.synchronizedMap(new HashMap<UUID, Friend>());
    
    private FriendManager() { /* restrict direct instantiation */ }
    
    public Friend getFriend(UUID uuid)
    {
        Friend friend = friends.get(uuid);
        if (friend == null) {
            friend = new Friend(uuid);
            friends.put(uuid, friend);
        }
        
        return friend;
    }
    
    public void getFriend(final String name, final FriendLookupCallback callback)
    {
        // find the friend by his/her name from the index
        final Firebase playerIdFromNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name/" + name.toLowerCase());
        playerIdFromNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    callback.onNotFound();
                } else {
                    UUID uuid = UUID.fromString((String) snapshot.getValue());
                    callback.onFound(getFriend(uuid));
                }
            }
        });
    }
    
    public List<Friend> getFriends(FriendsList list)
    {
        List<Friend> friendsList = new ArrayList<Friend>();
        
        Iterator<Friend> it = friends.values().iterator();
        while (it.hasNext()) {
            Friend friend = it.next();
            if (friend.belongsToList(list)) {
                friendsList.add(friend);
            }
        }
        
        return friendsList;
    }
}
