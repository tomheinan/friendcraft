package com.nixielabs.friendcraft;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nixielabs.friendcraft.callbacks.FriendLookupCallback;
import com.nixielabs.friendcraft.models.Friend;

public class MessagingCommandExecutor implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (!(sender instanceof Player)) {
            FriendCraft.error("The \"msg\" command may only be used by players");
            return true;
        }
        
        final Player currentPlayer = (Player) sender;
        
        if (args.length > 1) {
            final String friendName = args[0];
            
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                String word = args[i];
                stringBuilder.append(word);
                
                if (i != args.length - 1) {
                    stringBuilder.append(' ');
                }
            }
            
            final String message = stringBuilder.toString();
            
            FriendManager.sharedInstance.getFriend(friendName, new FriendLookupCallback() {

                @Override
                public void onFound(Friend to) {
                    Friend from = FriendManager.sharedInstance.getFriend(currentPlayer.getUniqueId());
                    sendMessage(to, from, message);
                }

                @Override
                public void onNotFound() {
                    currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named \"" + friendName + "\". Sorry!");
                }
            });
            
            return true;
        }
        
        return false;
    }
    
    private void sendMessage(Friend to, Friend from, String message)
    {
        
    }
}
