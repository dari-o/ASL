package middleware;

import java.nio.channels.SocketChannel;

public class Request {
	private RequestType type;
	public StringBuffer content;
	private int requestLength;
	private SocketChannel client;
	private boolean complete = false;
	
	public Request(StringBuffer request, SocketChannel client){
		this.content = request;		
		this.type = getRequestType(this.content.substring(0, 4));
		this.client = client;
		setComplete();
	}
	/*public Request(String request){
		this.content = request.toString();		
		this.type = getRequestType(this.content.substring(0, 4));
	}*/
	
	public void setComplete(){
		String strContent = this.content.toString();
		if(!strContent.endsWith("\r\n")) {
			this.complete=false;
			return;
		}
		if((this.type == RequestType.SET) && strContent.split("\r\n").length==2) 
			this.complete = true;
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
			return this.content.substring(this.content.indexOf(" "),this.requestLength-2);
		}
		return this.content.toString().split(" ")[1];
	}

	public SocketChannel getClient() {
		return this.client;
	}
	public boolean isComplete() {
		return this.complete;
	}
	public boolean append(StringBuffer append) {
		this.content.append(append);
		setComplete();
		return this.complete;
	}
}
