package middleware;

import java.util.List;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Request {
	private RequestType type;
	public String content;
	private int requestLength;
	
	public Request(StringBuffer request){
		this.content = request.toString();
		this.type = getRequestType(request.substring(0, 4));
	}
	public Request(String request){
		this.content = request.toString();
		this.type = getRequestType(request.substring(0, 4));
	}
	
	public boolean isComplete(){
		if(!this.content.endsWith("\r\n")) return false;
		if((this.type == RequestType.SET) && this.content.split("\r\n").length!=2) return false;
		return true;
	}
	
	public RequestType getRequestType(String command){
		if (command.equals("get "))
			return RequestType.GET;
		if (command.equals("set "))
			return RequestType.SET;
		if (command.equals("gets"))
			return RequestType.MULTI_GET;
		return null;
	}
	
	public RequestType getType(){
		return this.type;
	}
	public String getKey(){
		if(this.type == RequestType.MULTI_GET){
			return this.content.substring(this.content.indexOf(' '),this.requestLength-2);
		}
		return this.content.toString().split(" ")[1];
	}

	
	
}
