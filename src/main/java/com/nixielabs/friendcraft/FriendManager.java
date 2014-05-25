package com.nixielabs.friendcraft;

import java.util.ArrayList;
import java.util.Collection;
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
import com.nixielabs.friendcraft.callbacks.FriendLookupCallback;
import com.nixielabs.friendcraft.models.Friend;
import com.nixielabs.friendcraft.models.FriendsList;

public class FriendManager
{
    public final static FriendManager sharedInstance = new FriendManager();
    
    private final Map<UUID, Friend> friends = Collections.synchronizedMap(new HashMap<UUID, Friend>());
    
    private FriendManager() { /* restrict direct instantiation */ }
    
    public Friend getFriend(UUID uuid)
    {
        Friend friend;
        synchronized(friends) {
            friend = friends.get(uuid);
            if (friend == null) {
                friend = new Friend(uuid);
                friends.put(uuid, friend);
            }
        }
        
        return friend;
    }
    
    public void getFriend(final String name, final FriendLookupCallback friendLookupCallback)
    {
        // find the friend by his/her name from the index
        final Firebase playerIdFromNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name/" + name.toLowerCase());
        playerIdFromNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() == null) {
                    friendLookupCallback.onNotFound();
                } else {
                    UUID uuid = UUID.fromString((String) snapshot.getValue());
                    friendLookupCallback.onFound(getFriend(uuid));
                }
            }
        });
    }
    
    public List<Friend> getFriends(FriendsList list)
    {
        List<Friend> friendsList = new ArrayList<Friend>();
        Collection<Friend> allFriends = Collections.synchronizedCollection(friends.values());
        
        synchronized(allFriends) {
            Iterator<Friend> it = allFriends.iterator();
            while (it.hasNext()) {
                Friend friend = it.next();
                if (friend.belongsToList(list)) {
                    friendsList.add(friend);
                }
            }
        }
        
        Collections.sort(friendsList);
        return friendsList;
    }
}
