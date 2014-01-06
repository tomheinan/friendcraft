package com.tomheinan.friendcore;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

import com.google.gson.Gson;
import com.tomheinan.friendcore.message.Message;

public class MessageDispatcher
{
    private static final AsyncHandler sendHandler = new AsyncHandler();
    private static final Gson gson = new Gson();
    
    public static void broadcast(Message message)
    {
        Iterator<Session> i = FriendCore.sessions.iterator();
        while (i.hasNext()) {
            Session session = i.next();
            session.getAsyncRemote().sendText(toJson(message), sendHandler);
        }
    }
    
    private static class AsyncHandler implements SendHandler
    {
        @Override
        public void onResult(SendResult result) {
            // TODO Auto-generated method stub
        }
    }
    
    public static String toJson(Message message)
    {
        Map<String, Message> wrapper = new TreeMap<String, Message>();
        wrapper.put(message.getMessageType(), message);
        return gson.toJson(wrapper);
    }
}
