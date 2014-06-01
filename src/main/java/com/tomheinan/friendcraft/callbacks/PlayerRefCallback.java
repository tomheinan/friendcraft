package com.tomheinan.friendcraft.callbacks;

import java.util.UUID;

import com.firebase.client.Firebase;

public abstract class PlayerRefCallback
{
    public abstract void onFound(Firebase playerRef, UUID playerId, String playerName);
    public abstract void onNotFound();
}
