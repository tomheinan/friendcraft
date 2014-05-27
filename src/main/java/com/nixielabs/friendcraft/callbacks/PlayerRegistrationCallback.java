package com.nixielabs.friendcraft.callbacks;

import java.util.UUID;

import org.bukkit.entity.Player;

public abstract class PlayerRegistrationCallback
{
    public abstract void onRegistration(Player player, UUID uuid);
}
