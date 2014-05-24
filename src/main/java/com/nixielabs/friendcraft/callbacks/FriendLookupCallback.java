package com.nixielabs.friendcraft.callbacks;

import com.nixielabs.friendcraft.models.Friend;

public abstract class FriendLookupCallback
{
    public abstract void onFound(Friend friend);
    public abstract void onNotFound();
}
