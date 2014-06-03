package com.tomheinan.friendcraft.commandexecutors;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.callbacks.PlayerRefCallback;
import com.tomheinan.friendcraft.managers.FriendsListManager;
import com.tomheinan.friendcraft.managers.PlayerManager;
import com.tomheinan.friendcraft.models.FriendsList;

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
                        
                    } else if (item.equalsIgnoreCase("private")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Turns private mode on or off.",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc private <on/off>"
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
                            currentPlayer.sendMessage(
                                ChatColor.YELLOW + "FriendCraft can't find a player named " +
                                ChatColor.WHITE + friendName + ChatColor.YELLOW + ". Sorry!"
                            );
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
                        friendsList.setShowSidebar(true);
                    } else {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "Sorry, the FriendCraft sidebar is not currently enabled.");
                    }
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("hide")) {
                    boolean enableSidebar = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar");
                    if (enableSidebar) {
                        friendsList.setShowSidebar(false);
                    } else {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "Sorry, the FriendCraft sidebar is not currently enabled.");
                    }
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("private") && args.length > 1) {
                    String onOff = args[1];
                    boolean privateMode = false;
                    final String successMessage;
                    
                    if (onOff == null || !(onOff.equalsIgnoreCase("on") || onOff.equalsIgnoreCase("off"))) {
                        return false;
                    }
                    
                    if (onOff.equalsIgnoreCase("on")) {
                        privateMode = true;
                        successMessage = ChatColor.YELLOW + "Enabled private mode.";
                    } else {
                        privateMode = false;
                        successMessage = ChatColor.YELLOW + "Disabled private mode.";
                    }
                    
                    Firebase privateModeSettingRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(currentPlayer) + "/settings/private-mode");
                    privateModeSettingRef.setValue(Boolean.valueOf(privateMode), new Firebase.CompletionListener() {

                        public void onComplete(FirebaseError error, Firebase ref) {
                            currentPlayer.sendMessage(successMessage);
                        }
                    });
                    
                    return true;
                    
                }
            }
        }
        
        return false;
    }
}
