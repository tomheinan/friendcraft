package com.tomheinan.friendcraft.callbacks;

import java.util.UUID;

public abstract class PlayerDeregistrationCallback
{
    public abstract void onDeregistration(UUID uuid);
}
