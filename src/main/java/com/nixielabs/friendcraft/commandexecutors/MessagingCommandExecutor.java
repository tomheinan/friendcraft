package com.nixielabs.friendcraft.commandexecutors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nixielabs.friendcraft.FriendCraft;
import com.nixielabs.friendcraft.managers.MessagingManager;

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
            
            String message = stringBuilder.toString();
            MessagingManager.sendMessage(currentPlayer, friendName, message);
            
            return true;
        }
        
        return false;
    }
}
