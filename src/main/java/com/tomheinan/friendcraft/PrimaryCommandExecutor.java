package com.tomheinan.friendcraft;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.tomheinan.friendcraft.callbacks.FriendLookupCallback;
import com.tomheinan.friendcraft.models.Friend;
import com.tomheinan.friendcraft.models.FriendsList;

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
            final FriendsList friendsList = FriendsListManager.sharedInstance.getListForPlayer(currentPlayer);
            
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
                    } else if (item.equalsIgnoreCase("show")) {
                        currentPlayer.sendMessage(new String[]{"Shows the sidebar", "Usage: /fc show"});
                        return true;
                    } else if (item.equalsIgnoreCase("list")) {
                        currentPlayer.sendMessage(new String[]{"Hides the sidebar", "Usage: /fc hide"});
                        return true;
                    }
                    
                } else if (action.equalsIgnoreCase("add") && args.length > 1) {
                    final String friendName = args[1];
                    
                    // prevent a player from adding him/herself as a friend
                    // (that's just depressing)
//                    if (friendName.equalsIgnoreCase(currentPlayer.getName())) {
//                        currentPlayer.sendMessage(ChatColor.YELLOW + "You can't add yourself as a friend! Go befriend some other human beings.");
//                        return true;
//                    }
                    
                    FriendManager.sharedInstance.getFriend(friendName, new FriendLookupCallback() {

                        @Override
                        public void onFound(Friend friend) {
                            friendsList.add(friend);
                        }

                        @Override
                        public void onNotFound() {
                            currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named \"" + friendName + "\". Sorry!");
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("remove") && args.length > 1) {
                    final String friendName = args[1];
                    
                    FriendManager.sharedInstance.getFriend(friendName, new FriendLookupCallback() {

                        @Override
                        public void onFound(Friend friend) {
                            friendsList.remove(friend);
                        }

                        @Override
                        public void onNotFound() {
                            currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named \"" + friendName + "\". Sorry!");
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("list")) {
                    if (friendsList.getFriends().size() == 0) {
                        currentPlayer.sendMessage(new String[] {
                            ChatColor.YELLOW + "You haven't added any friends yet.",
                            ChatColor.YELLOW + "Use /fc add <player name> to add a friend to your list."
                        });
                        
                        return true;
                    }
                    
                    currentPlayer.sendMessage(friendsList.toString());
                    return true;
                    
                } else if (action.equalsIgnoreCase("show")) {
                    friendsList.showSidebar();
                    return true;
                    
                } else if (action.equalsIgnoreCase("hide")) {
                    friendsList.hideSidebar();
                    return true;
                    
                }
            }
        }
        
        return false;
    }
}
