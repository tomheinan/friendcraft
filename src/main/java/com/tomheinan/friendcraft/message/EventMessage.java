package com.tomheinan.friendcraft.message;

public class EventMessage extends Message
{
    protected String type;
    
    public EventMessage()
    {
        super();
        this.messageType = "event";
    }
}
