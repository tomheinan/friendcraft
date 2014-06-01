package com.tomheinan.friendcraft.commandexecutors;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.tomheinan.friendcraft.FriendCraft;
import com.tomheinan.friendcraft.callbacks.PlayerRefCallback;
import com.tomheinan.friendcraft.managers.MessagingManager;
import com.tomheinan.friendcraft.managers.PlayerManager;

public class MessagingCommandExecutor implements CommandExecutor
{
    public boolean onCommand(CommandSender sender, Command command, String label, final String[] args)
    {
        if (!(sender instanceof Player)) {
            FriendCraft.error("The \"msg\" command may only be used by players");
            return true;
        }
        
        final Player currentPlayer = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length > 1) {
                final String friendName = args[0];
                String message = buildMessage(Arrays.copyOfRange(args, 1, args.length));
                
                MessagingManager.sendMessage(currentPlayer, friendName, message);
                return true;
            }
            
        } else if (command.getName().equalsIgnoreCase("r")) {
            Firebase replyToRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(currentPlayer) + "/reply-to");
            replyToRef.addListenerForSingleValueEvent(new ValueEventListener() {

                public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                public void onDataChange(DataSnapshot snapshot) {
                    String uuidString = (String) snapshot.getValue();
                    if (uuidString == null) {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "Use /r to reply to the last player who sent you a message.");
                        
                    } else {
                        final UUID uuid = UUID.fromString(uuidString);
                        FriendCraft.getPlayerRef(uuid, new PlayerRefCallback() {
                            
                            public void onNotFound() { FriendCraft.error("Unable to reply to player " + uuid.toString()); }
                            
                            public void onFound(Firebase playerRef, UUID playerId, String playerName) {
                                if (args.length == 0) {
                                    currentPlayer.sendMessage(
                                        ChatColor.YELLOW + "The last message you received was sent by " + ChatColor.WHITE + playerName + ChatColor.YELLOW + "."
                                    );
                                    
                                } else {
                                    String message = buildMessage(args);
                                    MessagingManager.sendMessage(currentPlayer, playerName, message);
                                }
                            }
                        });
                    }
                }
            });
            
            return true;
        }
        
        return false;
    }
    
    private String buildMessage(String[] args)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            String word = args[i];
            stringBuilder.append(word);
            
            if (i != args.length - 1) {
                stringBuilder.append(' ');
            }
        }
        
        return stringBuilder.toString();
    }
}
