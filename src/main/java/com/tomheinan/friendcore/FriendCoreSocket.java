package com.tomheinan.friendcore;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

public class FriendCoreSocket extends WebSocketAdapter
{
    @Override
    public void onWebSocketConnect(Session session)
    {
        super.onWebSocketConnect(session);
        FriendCore.log("Socket Connected: " + session);
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        FriendCore.log("Received TEXT message: " + message);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        FriendCore.log("Socket Closed: [" + statusCode + "] " + reason);
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        FriendCore.error("Socket error:", cause);
    }
}
