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
                        currentPlayer.sendMessage(new String[]{"Adds a player to your friend list", "Usage: /fc add <player name>"});
                        return true;
                    } else if (item.equalsIgnoreCase("remove")) {
                        currentPlayer.sendMessage(new String[]{"Removes a player from your friend list", "Usage: /fc remove <player name>"});
                        return true;
                    } else if (item.equalsIgnoreCase("list")) {
                        currentPlayer.sendMessage(new String[]{"Lists your friends", "Usage: /fc list"});
                        return true;
                    }
                } else if (action.equalsIgnoreCase("add") && args.length > 1) {
                    final String name = args[1];
                    
                    Firebase playersRef = new Firebase(FriendCraft.firebaseRoot + "/players");
                    playersRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onCancelled(FirebaseError error) {
                            // TODO Auto-generated method stub
                        }

                        public void onDataChange(DataSnapshot snap) {
                            String otherPlayerName = null;
                            String otherPlayerId = null;
                            boolean found = false;
                            for (DataSnapshot otherPlayer : snap.getChildren()) {
                                otherPlayerName = (String) otherPlayer.child("name").getValue();
                                if (otherPlayerName != null && otherPlayerName.equalsIgnoreCase(name)) {
                                    otherPlayerId = (String) otherPlayer.getName();
                                    found = true;
                                    break;
                                }
                            }
                            
                            if (found) {
                                final String foundPlayerName = otherPlayerName;
                                Firebase currentPlayerFriendsRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                                currentPlayerFriendsRef.child(otherPlayerId).setValue(Boolean.TRUE, new Firebase.CompletionListener() {
                                    
                                    public void onComplete(FirebaseError error, Firebase ref) {
                                        // TODO check for error here
                                        currentPlayer.sendMessage("Added " + foundPlayerName + " to your friend list");
                                    }
                                    
                                });
                            } else {
                                currentPlayer.sendMessage("Couldn't find a player named \"" + name + "\"");
                            }
                        }
                        
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("remove") && args.length > 1) {
                    final String name = args[1];
                    
                    Firebase currentPlayerFriendsRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + currentPlayer.getUniqueId().toString() + "/friends");
                    currentPlayerFriendsRef.addListenerForSingleValueEvent(new ValueEventListener() {

                        public void onCancelled(FirebaseError error) {
                            // TODO Auto-generated method stub
                        }

                        public void onDataChange(DataSnapshot snap) {
                            boolean found = false;
                            for (DataSnapshot otherPlayer : snap.getChildren()) {
                                // TODO actually we need some kind of player -> ID lookup here or something
                            }
                        }
                        
                    });
                }
            }
        }
        
        return false;
    }
}
