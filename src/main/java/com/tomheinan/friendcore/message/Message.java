package com.tomheinan.friendcore.message;

public class Message
{
    protected transient String messageType;
    
    public Message()
    {
        this.messageType = "message";
    }
    
    public String getMessageType()
    {
        return this.messageType;
    }
}
