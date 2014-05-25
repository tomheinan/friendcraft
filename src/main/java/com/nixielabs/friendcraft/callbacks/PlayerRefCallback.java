package com.nixielabs.friendcraft.callbacks;

import com.firebase.client.Firebase;

public abstract class PlayerRefCallback
{
    public abstract void onFound(Firebase playerRef);
    public abstract void onNotFound();
}
