package com.tomheinan.friendcraft.commandexecutors;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ocpsoft.prettytime.PrettyTime;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
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
                        
                    } else if (item.equalsIgnoreCase("info")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Shows detailed information about a given player.",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc info <player name>"
                        });
                        return true;
                        
                    } else if (item.equalsIgnoreCase("sidebar")) {
                        currentPlayer.sendMessage(new String[]{
                            ChatColor.YELLOW + "Turns the sidebar on or off.",
                            ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/fc sidebar <on/off>"
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
                    
                } else if (action.equalsIgnoreCase("info") && args.length > 1) {
                    final String playerName = args[1];
                    
                    FriendCraft.getPlayerRef(playerName, new PlayerRefCallback() {
                        
                        public void onFound(Firebase playerRef, UUID playerId, final String playerName) {
                            playerRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                public void onDataChange(DataSnapshot snapshot) {
                                    Boolean privateMode = (Boolean) snapshot.child("settings/private-mode").getValue();
                                    if (privateMode == null) {
                                        privateMode = Boolean.FALSE;
                                    }
                                    
                                    Boolean inFriendsList = Boolean.FALSE;
                                    UUID currentPlayerId = PlayerManager.sharedInstance.getUUID(currentPlayer);
                                    if (privateMode.booleanValue()) {
                                        Iterator<DataSnapshot> it = snapshot.child("friends").getChildren().iterator();
                                        while (it.hasNext()) {
                                            DataSnapshot friendEntry = it.next();
                                            UUID uuid = UUID.fromString(friendEntry.getName());
                                            if (currentPlayerId.equals(uuid)) {
                                                inFriendsList = Boolean.TRUE;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (!privateMode.booleanValue() || inFriendsList.booleanValue()) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> presence = (Map<String, Object>) snapshot.child("presence").getValue();
                                        String pluginId = (String) FriendCraft.sharedInstance.getConfig().getConfigurationSection("authentication").getValues(false).get("id");
                                        
                                        if (presence != null && presence.get("plugin") != null) {
                                            if (pluginId.equalsIgnoreCase((String) presence.get("plugin"))) {
                                                currentPlayer.sendMessage(
                                                    playerName + ChatColor.YELLOW + " is online and playing on this server."
                                                );
                                                
                                            } else {
                                                Firebase pluginNameRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + ((String) presence.get("plugin")) + "/status/name");
                                                pluginNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                                    public void onDataChange(DataSnapshot snapshot) {
                                                        String pluginName = (String) snapshot.getValue();
                                                        currentPlayer.sendMessage(
                                                            playerName + ChatColor.YELLOW + " is online and playing on the " + ChatColor.WHITE +
                                                            pluginName + ChatColor.YELLOW + " server."
                                                        );
                                                    }
                                                });
                                            }
                                        } else if (presence != null && presence.get("app") != null) {
                                            currentPlayer.sendMessage(
                                                playerName + ChatColor.YELLOW + " is online via the FriendCraft app."
                                            );
                                            
                                        } else {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> lastSeen = (Map<String, Object>) snapshot.child("last-seen").getValue();
                                            
                                            if (lastSeen != null) {
                                                String lastPluginId = (String) lastSeen.get("plugin");
                                                Long lastTimestamp = (Long) lastSeen.get("timestamp");
                                                final Date lastSeenAt = new Date(lastTimestamp.longValue());
                                                
                                                Firebase pluginNameRef = new Firebase(FriendCraft.firebaseRoot + "/plugins/" + lastPluginId + "/status/name");
                                                pluginNameRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                                    public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                                    public void onDataChange(DataSnapshot snapshot) {
                                                        String pluginName = (String) snapshot.getValue();
                                                        PrettyTime pt = new PrettyTime();
                                                        currentPlayer.sendMessage(
                                                            playerName + ChatColor.YELLOW + " is offline; they were last seen " + ChatColor.WHITE +
                                                            pt.format(lastSeenAt) + ChatColor.YELLOW + " on " + ChatColor.WHITE +
                                                            pluginName + ChatColor.YELLOW + "."
                                                        );
                                                    }
                                                });
                                            } else {
                                                currentPlayer.sendMessage(
                                                    playerName + ChatColor.YELLOW + " is offline."
                                                );
                                            }
                                        }
                                        
                                    } else {
                                        currentPlayer.sendMessage(
                                            playerName + ChatColor.YELLOW + " has chosen not to share their status."
                                        );
                                    }
                                }
                            });
                        }
                        
                        public void onNotFound() {
                            currentPlayer.sendMessage(ChatColor.YELLOW + "FriendCraft can't find a player named " + ChatColor.WHITE + playerName + ChatColor.YELLOW + ". Sorry!");
                        }
                    });
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("sidebar")) {
                    Firebase sidebarSettingRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(currentPlayer) + "/settings/sidebar");
                    boolean enableSidebar = FriendCraft.sharedInstance.getConfig().getBoolean("enable-sidebar", true);
                    
                    if (enableSidebar) {
                        if (args.length > 1) {
                            String onOff = args[1];
                            if (onOff.equalsIgnoreCase("on")) {
                                sidebarSettingRef.setValue(Boolean.TRUE);
                            } else if (onOff.equalsIgnoreCase("off")) {
                                sidebarSettingRef.setValue(Boolean.FALSE);
                            } else {
                                return false;
                            }
                            
                        } else {
                            sidebarSettingRef.addListenerForSingleValueEvent(new ValueEventListener() {

                                public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                                public void onDataChange(DataSnapshot snapshot) {
                                    Boolean showSidebar = (Boolean) snapshot.getValue();
                                    if (showSidebar == null) {
                                        showSidebar = Boolean.TRUE;
                                    }
                                    
                                    snapshot.getRef().setValue(Boolean.valueOf(!showSidebar.booleanValue()));
                                }
                            });
                        }
                        
                    } else {
                        currentPlayer.sendMessage(ChatColor.YELLOW + "Sorry, the FriendCraft sidebar is not currently enabled on this server.");
                    }
                    
                    return true;
                    
                } else if (action.equalsIgnoreCase("private")) {
                    Firebase privateModeSettingRef = new Firebase(FriendCraft.firebaseRoot + "/players/" + PlayerManager.sharedInstance.getUUID(currentPlayer) + "/settings/private-mode");
                    
                    if (args.length > 1) {
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
                        
                        privateModeSettingRef.setValue(Boolean.valueOf(privateMode), new Firebase.CompletionListener() {

                            public void onComplete(FirebaseError error, Firebase ref) {
                                currentPlayer.sendMessage(successMessage);
                            }
                        });
                        
                    } else {
                        privateModeSettingRef.addListenerForSingleValueEvent(new ValueEventListener() {

                            public void onCancelled(FirebaseError error) { FriendCraft.error(error.getMessage()); }

                            public void onDataChange(DataSnapshot snapshot) {
                                Boolean privateMode = (Boolean) snapshot.getValue();
                                if (privateMode == null) {
                                    privateMode = Boolean.FALSE;
                                }
                                
                                String onOff = "off";
                                if (privateMode.booleanValue()) {
                                    onOff = "on";
                                }
                                
                                currentPlayer.sendMessage(ChatColor.YELLOW + "Private mode is currently " + ChatColor.WHITE + onOff + ChatColor.YELLOW + ".");
                            }
                        });
                    }
                    
                    return true;
                }
            }
        }
        
        return false;
    }
}
