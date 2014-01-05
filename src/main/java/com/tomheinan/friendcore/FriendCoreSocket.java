package com.tomheinan.friendcore;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ClientEndpoint
@ServerEndpoint(value="/")
public class FriendCoreSocket
{
    @OnOpen
    public void onWebSocketConnect(Session session)
    {
        FriendCore.log("Socket Connected: " + session);
    }
    
    @OnMessage
    public void onWebSocketText(String message)
    {
        FriendCore.log("Received TEXT message: " + message);
    }
    
    @OnClose
    public void onWebSocketClose(CloseReason reason)
    {
        FriendCore.log("Socket Closed: " + reason);
    }
    
    @OnError
    public void onWebSocketError(Throwable cause)
    {
        FriendCore.error("Socket error", cause);
    }
}
