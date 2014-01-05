package com.tomheinan.friendcore;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class FriendCoreServlet extends WebSocketServlet
{
    private static final long serialVersionUID = 5548790152861979109L;

    @Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.register(FriendCoreSocket.class);
    }
}
