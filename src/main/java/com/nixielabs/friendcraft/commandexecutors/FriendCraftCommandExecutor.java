package com.nixielabs.friendcraft.commandexecutors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.callbacks.PlayerRefCallback;
import com.nixielabs.friendcraft.callbacks.UUIDCallback;
import com.nixielabs.friendcraft.managers.FriendsListManager;
import com.nixielabs.friendcraft.models.FriendsList;
import com.nixielabs.friendcraft.tasks.UUIDTask;

public class FriendCraftCommandExecutor implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getName().equalsIgnoreCase("fc")) {
            if (!(sender instanceof Player)) {
                FriendCraft.error("The \"fc\" command may only be used by players ");
                return true;
            }
            
            final Player currentPlayer = (Player) sender;
            final FriendsList friendsList = FriendsListManager.sharedInstance.getList(currentPlayer);
            
            if (friendsList == null) {
                currentPlayer.sendMessage(
                    ChatColor.YELLOW + "FriendCraft is currently disabled for your account. " +
                    "Please contact your server administrator for assistance."
                );
                
                return true;
            }
            
            if (args.length > 0) {
                String action = args[0];
                
                if (action.equalsIgnoreCase("help") && args.length > 1) {
                    String item = args[1];
                    
                    if (item.equalsIgnoreCase("add")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Adds a player to your friends list.",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc add <player name>"
                        });
                        return true;
                        
                    } else if (item.equalsIgnoreCase("remove")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Removes a player from your friends list.",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc remove <player name>"
                        });
                        return true;
                        
                    } else if (item.equalsIgnoreCase("list")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Lists your friends.",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc list"
                        });
                        return true;
                        
                    } else if (item.equalsIgnoreCase("show")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Shows the sidebar",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc show"
                        });
                        return true;
                        
                    } else if (item.equalsIgnoreCase("hide")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Hides the sidebar",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc hide"
                        });
                        return true;
                    }
                    
                } else if (action.equalsIgnoreCase("add") && args.length > 1) {
                    final String friendName = args[1];
                    
                    // prevent a player from adding him/herself as a friend
                    // (that's just depressing)
                    if (friendName.equalsIgnoreCase(currentPlayer.getName())) {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "You can't add yourself as a friend! Go befriend some other human beings.");
                        return true;
                    }
                    
                    FriendCraft.getPlayerRef(friendName, new PlayerRefCallback() {
                        
                        public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                            friendsList.add(playerId);
                        }
                        
                        public void onNotFound() {
                            List<String> friendNames = new ArrayList<String>();
                            friendNames.add(friendName);
                            
                            UUIDTask uuidTask = new UUIDTask(friendNames, new UUIDCallback() {
                                
                                public void onResult(Map<String, UUID> result) {
                                    Set<Entry<String, UUID>> entries = result.entrySet();
                                    if (entries.size() == 0) {
                                        currentPlayer.sendMessage(
                                            ChatColor.YELLOW + "FriendCraft can't find a player named " +
                                            ChatColor.WHITE + friendName + ChatColor.YELLOW + ". Sorry!"
                                        );
                                    } else {
                                        Iterator<Entry<String, UUID>> it = entries.iterator();
                                        Entry<String, UUID> entry = it.next();
                                        
                                        final String playerName = entry.getKey();
                                        final UUID playerId = entry.getValue();
                                        
                                        Firebase playerRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + playerId.toString());
                                        playerRef.child("name").setValue(playerName, new Firebase.CompletionListener() {

                                            public void onComplete(FirebaseError error, Firebase ref) {
                                                if (error == null) {
                                                    // index this player by name
                                                    Firebase indexPlayerByNameRef = new Firebase(FriendCraft.firebaseRoot + "/index/players/by-name");
                                                    indexPlayerByNameRef.child(playerName.toLowerCase()).setValue(playerId.toString());
                                                    
                                                    // add player to friends list
                                                    friendsList.add(playerId);
                                                    
                                                } else {
                                                    FriendCraft.error(error.getMessage());
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                            
                            uuidTask.runTaskAsynchronously(FriendCraft.sharedInstance);
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("remove") && args.length > 1) {
                    final String friendName = args[1];
                    
                    FriendCraft.getPlayerRef(friendName, new PlayerRefCallback() {
                        
                        public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                            friendsList.remove(playerId);
                        }
                        
                        public void onNotFound() {
                            currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named " + ChatColor.WHITE + friendName + ChatColor.YELLOW + ". Sorry!");
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("list")) {
                    if (friendsList.size() == 0) {
                        currentPlayer.sendMessage(new String[] {
                            ChatColor.YELLOW + "You haven't added any friends yet.",
                            ChatColor.YELLOW + "Use /fc add <player name> to add a friend to your list."
                        });
                        
                        return true;
                    }
                    
                    currentPlayer.sendMessage(friendsList.toString());
                    return true;
                    
                } else if (action.equalsIgnoreCase("show")) {
                    boolean enableSidebar = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar");
                    if (enableSidebar) {
                        //friendsList.showSidebar();
                    } else {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "Sorry, the FriendCraft sidebar is not currently enabled.");
                    }
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("hide")) {
                    boolean enableSidebar = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar");
                    if (enableSidebar) {
                        //friendsList.hideSidebar();
                    } else {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "Sorry, the FriendCraft sidebar is not currently enabled.");
                    }
                    
                    return true;
                    
                }
            }
        }
        
        return false;
    }
}
