package com.tomheinan.friendcraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class PrimaryCommandExecutor implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getName().equalsIgnoreCase("fc")) {
            if (!(sender instanceof Player)) {
                FriendCraft.error("The \"fc\" command may only be used by players ");
                return true;
            }
            
            final Player currentPlayer = (Player) sender;
            
            if (args.length > 0) {
                String action = args[0];
                
                if (action.equalsIgnoreCase("help") && args.length > 1) {
                    String item = args[1];
                    
                    if (item.equalsIgnoreCase("add")) {
                        currentPlayer.sendMessage(new String[]{"Adds a player to your friends list", "Usage: /fc add <player name>"});
                        return true;
                    } else if (item.equalsIgnoreCase("remove")) {
                        currentPlayer.sendMessage(new String[]{"Removes a player from your friends list", "Usage: /fc remove <player name>"});
                        return true;
                    } else if (item.equalsIgnoreCase("list")) {
                        currentPlayer.sendMessage(new String[]{"Lists your friends", "Usage: /fc list"});
                        return true;
                    }
                } else if (action.equalsIgnoreCase("add") && args.length > 1) {
                    final String friendName = args[1];
                    final Firebase playerIdFromNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name/" + friendName.toLowerCase());
                    
                    // prevent a player from adding him/herself as a friend
                    // (that's just depressing)
//                    if (friendName.equalsIgnoreCase(currentPlayer.getName())) {
//                        currentPlayer.sendMessage(ChatColor.YELLOW + "You can't add yourself as a friend! Go befriend some other human beings.");
//                        return true;
//                    }
                    
                    // find the friend by his/her name from the index
                    playerIdFromNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.getValue() == null) {
                                currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named \"" + friendName + "\". Sorry!");
                            } else {
                                final String friendId = (String) snapshot.getValue();
                                final Firebase friendNameRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId + "/name");
                                
                                // get the friend's official name
                                friendNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                    public void onDataChange(DataSnapshot snapshot) {
                                        final String officialFriendName = (String) snapshot.getValue();
                                        final Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                                        
                                        // check to see if the current player already has the friend on his/her list
                                        friendsListRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {

                                            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                            public void onDataChange(DataSnapshot snapshot) {
                                                if (snapshot.getValue() != null && ((Boolean)snapshot.getValue()).booleanValue()) {
                                                    currentPlayer.sendMessage(
                                                        officialFriendName + ChatColor.YELLOW + " is already on your friends list."
                                                    );
                                                } else {
                                                    // add the friend!
                                                    friendsListRef.child(friendId).setValue(Boolean.TRUE, new Firebase.CompletionListener() {
                                                        
                                                        public void onComplete(FirebaseError error, Firebase ref) {
                                                            currentPlayer.sendMessage(
                                                                ChatColor.YELLOW + "Added " + ChatColor.WHITE + officialFriendName + ChatColor.YELLOW + " to your friends list."
                                                            );
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("remove") && args.length > 1) {
                    final String friendName = args[1];
                    final Firebase playerIdFromNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name/" + friendName.toLowerCase());
                    
                    // find the friend by his/her name from the index
                    playerIdFromNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.getValue() == null) {
                                currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named \"" + friendName + "\". Sorry!");
                            } else {
                                final String friendId = (String) snapshot.getValue();
                                final Firebase friendNameRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId + "/name");
                                
                                // get the friend's official name
                                friendNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                    public void onDataChange(DataSnapshot snapshot) {
                                        final String officialFriendName = (String) snapshot.getValue();
                                        final Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                                        
                                        // check to see if the friend is present on the player's list
                                        friendsListRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {

                                            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                            public void onDataChange(DataSnapshot snapshot) {
                                                if (snapshot.getValue() == null || !((Boolean)snapshot.getValue()).booleanValue()) {
                                                    currentPlayer.sendMessage(
                                                        officialFriendName + ChatColor.YELLOW + " is not on your friends list."
                                                    );
                                                } else {
                                                    // remove the friend from the list
                                                    friendsListRef.child(friendId).removeValue(new Firebase.CompletionListener() {
                                                        
                                                        public void onComplete(FirebaseError error, Firebase ref) {
                                                            currentPlayer.sendMessage(
                                                                ChatColor.YELLOW + "Removed " + ChatColor.WHITE + officialFriendName + ChatColor.YELLOW + " from your friends list."
                                                            );
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("list")) {
                    final Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                    
                    friendsListRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                        public void onDataChange(DataSnapshot snapshot) {
                            final long numberOfFriends = snapshot.getChildrenCount();
                            
                            if (numberOfFriends == 0) {
                                currentPlayer.sendMessage(new String[] {
                                    ChatColor.YELLOW + "You haven't added any friends yet.",
                                    ChatColor.YELLOW + "Use /fc add <player name> to add a friend to your list."
                                });
                            }
                            
                            final AtomicLong friendsRead = new AtomicLong();
                            final List<DataSnapshot> friendSnapshots = Collections.synchronizedList(new ArrayList<DataSnapshot>());
                            
                            for (DataSnapshot friendIdSnapshot : snapshot.getChildren()) {
                                final String friendId = friendIdSnapshot.getName();
                                final Firebase friendRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId);
                                
                                friendRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                    public void onDataChange(DataSnapshot friendSnapshot) {
                                        friendSnapshots.add(friendSnapshot);
                                        long currentFriendsRead = friendsRead.incrementAndGet();
                                        
                                        if (currentFriendsRead == numberOfFriends) {
                                            StringBuilder stringBuilder = new StringBuilder();
                                            Iterator<DataSnapshot> it = friendSnapshots.iterator(); 
                                            
                                            while (it.hasNext()) {
                                                DataSnapshot friend = (DataSnapshot) it.next();
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> friendData = (Map<String, Object>) friend.getValue();
                                                stringBuilder.append((String) friendData.get("name"));
                                            }
                                            
                                            currentPlayer.sendMessage(stringBuilder.toString());
                                        }
                                    }
                                });
                            }
                        }
                    });
                    
                    return true;
                    
                }
            }
        }
        
        return false;
    }
}
