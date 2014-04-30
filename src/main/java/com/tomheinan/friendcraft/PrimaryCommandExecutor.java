package com.tomheinan.friendcraft;

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
                    
                    // find the friend by his/her name from the index
                    playerIdFromNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onCancelled(FirebaseError error) {
                            FriendCraft.error(error.getMessage());
                        }

                        public void onDataChange(DataSnapshot snap) {
                            if (snap.getValue() == null) {
                                currentPlayer.sendMessage("Player \"" + friendName + "\" not found.");
                            } else {
                                final String friendId = (String) snap.getValue();
                                final Firebase friendNameRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId + "/name");
                                
                                // get the friend's official name
                                friendNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                    public void onCancelled(FirebaseError error) {
                                        FriendCraft.error(error.getMessage());
                                    }

                                    public void onDataChange(DataSnapshot snap) {
                                        // save the friend's ID to the current player's friends list
                                        // and confirm with the official name
                                        final String officialFriendName = (String) snap.getValue();
                                        final Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                                        
                                        friendsListRef.child(friendId).setValue(Boolean.TRUE, new Firebase.CompletionListener() {
                                            
                                            public void onComplete(FirebaseError error, Firebase ref) {
                                                currentPlayer.sendMessage("Added " + officialFriendName + " to your friends list.");
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

                        public void onCancelled(FirebaseError error) {
                            FriendCraft.error(error.getMessage());
                        }

                        public void onDataChange(DataSnapshot snap) {
                            if (snap.getValue() == null) {
                                currentPlayer.sendMessage("Player \"" + friendName + "\" not found.");
                            } else {
                                final String friendId = (String) snap.getValue();
                                final Firebase friendNameRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + friendId + "/name");
                                
                                // get the friend's official name
                                friendNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                    public void onCancelled(FirebaseError error) {
                                        FriendCraft.error(error.getMessage());
                                    }

                                    public void onDataChange(DataSnapshot snap) {
                                        // save the friend's ID to the current player's friends list
                                        // and confirm with the official name
                                        final String officialFriendName = (String) snap.getValue();
                                        final Firebase friendsListRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                                        
                                        friendsListRef.child(friendId).removeValue(new Firebase.CompletionListener() {
                                            
                                            public void onComplete(FirebaseError error, Firebase ref) {
                                                currentPlayer.sendMessage("Removed " + officialFriendName + " from your friends list.");
                                            }
                                        });
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
