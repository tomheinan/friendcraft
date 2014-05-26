package com.nixielabs.friendcraft.callbacks;

import java.util.UUID;

public abstract class UUIDCallback
{
    public abstract void onFound(UUID uuid);
    public abstract void onNotFound();
}
