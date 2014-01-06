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
    public void onOpen(Session session)
    {
        FriendCore.sessions.add(session);
        FriendCore.log("session properties:" + session.getUserProperties().toString());
        FriendCore.log(Integer.toString(FriendCore.sessions.size()));
    }
    
    @OnMessage
    public void onMessage(Session session, String message)
    {
        FriendCore.log("Received TEXT message: " + message);
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason)
    {
        FriendCore.sessions.remove(session);
        FriendCore.log(Integer.toString(FriendCore.sessions.size()));
    }
    
    @OnError
    public void onError(Session session, Throwable cause)
    {
        FriendCore.error("Socket error", cause);
    }
}
