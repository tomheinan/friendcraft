package com.tomheinan.friendcore.message;

public class EventMessage extends Message
{
    protected String type;
    
    public EventMessage()
    {
        super();
        this.messageType = "event";
    }
}
