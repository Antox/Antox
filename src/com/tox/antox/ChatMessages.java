package com.tox.antox;

public class ChatMessages {
	public String message;
    public boolean ownMessage;
    public String friendName;
	
	public ChatMessages()
	{
		super();
	}
	
	public ChatMessages(String message, boolean ownMessage)
	{
		super();
		this.message = message;
		this.ownMessage = ownMessage;
	}
	
	public ChatMessages(String message, String friendName)
	{
		super();
		this.message = message;
		this.friendName = friendName;
	}
	
	public boolean IsMine()
	{
		return ownMessage;
	}
}
