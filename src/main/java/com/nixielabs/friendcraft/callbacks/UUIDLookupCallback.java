package com.nixielabs.friendcraft.callbacks;

import java.util.Map;
import java.util.UUID;

public abstract class UUIDLookupCallback
{
    public abstract void onResult(Map<String, UUID> result);
}
