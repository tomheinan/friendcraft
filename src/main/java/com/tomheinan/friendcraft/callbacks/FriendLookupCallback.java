package com.tomheinan.friendcraft.callbacks;

import com.tomheinan.friendcraft.models.Friend;

public abstract class FriendLookupCallback
{
    public abstract void onFound(Friend friend);
    public abstract void onNotFound();
}
