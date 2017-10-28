package middleware;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.io.*;


//Class for the worker 
public class WorkerThread implements Runnable{
	Queue<SocketChannel> requestQueue;
	Selector selector;
	double numServers;
	boolean readSharded = false;
	List<SocketChannel> serverConnections;


	public WorkerThread(Queue<SocketChannel> requestQueue, Selector selector, List<String> mcAddresses,
			boolean readSharded){
		this.requestQueue = requestQueue;
		this.selector = selector;
		this.numServers = (double) mcAddresses.size();
		this.readSharded = readSharded;
		//this.serverConnections = openServerConnections(mcAddresses);
	}

	
	private List<SocketChannel> openServerConnections(List<String> mcAddresses) {
		List<SocketChannel> serverConnections = new ArrayList<SocketChannel>();
		for(int i=0; i<mcAddresses.size();i++){
			try {
				SocketChannel server = SocketChannel.open(new InetSocketAddress(mcAddresses.get(i), 11211));
				server.register(this.selector, SelectionKey.OP_READ); 
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return serverConnections;
	}


	public void run(){
		//check if there is Something in the queue
		while(true){
			SocketChannel client = null;
			synchronized(this.requestQueue){
				if(!this.requestQueue.isEmpty())
					//make sure that the threads
					client= this.requestQueue.poll();
			}
			processClientMessage(client);
		}
	}
	private void processClientMessage(SocketChannel client) {
		if (client == null) return;
		// allocate buffer to receive message
		// TODO deal with incomplete requests and multigets
		ByteBuffer buffer = ByteBuffer.allocate(6);
		// read the message from client
		StringBuffer fromClient = readMessage(client, buffer);
		Request request = new Request(fromClient);
		if(!request.isComplete()) fromClient.append(readMessage(client, buffer));
		StringBuffer response = new StringBuffer("STORED");
		// process message and send to servers
		switch(request.getType()){
		case SET:
			this.set(request);
		case GET:
			response = this.get(request);
		case MULTI_GET:
			//response = 
		}
		buffer.put(response.toString().getBytes());
		sendMessage(client, buffer);
		buffer.clear();
		//readByte = client.read(buffer);

		
	}

	//get the hashing of the key
	
	//get server id from consistent hashing server
	public static int getServerIndex(String key, double numServers){
		double hashedKey = getHashedKey(key, numServers);
		return (int) Math.ceil(hashedKey/(1.0/numServers));
	}

	public static double getHashedKey(String key, double numServers){
		double hashedKey = ((double) key.hashCode()%numServers)/numServers;
		return hashedKey;
	}

	/*public boolean isSetSuccess(String response){
		return response.equals("STORED");
	}*/
	
	public void set(Request request){
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(request.content.getBytes());
		for (int i=0; i<this.numServers; i++){
			SocketChannel server = this.serverConnections.get(i);
			this.sendMessage(server, buffer);
		}
		while (true){
			int stored=0;
			try {
				selector.select();
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
	            Iterator<SelectionKey> iter = selectedKeys.iterator();
	            while (iter.hasNext()) {
	 
	                SelectionKey key = iter.next();
	                // read events are for client channels. Add "request" to the queue
	                if (key.isReadable()) {
	                    SocketChannel server = (SocketChannel) key.channel();
	                    if(this.readMessage(server, buffer).equals("STORED"))
	                    	stored++;
	                    else
	                    	this.sendMessage(server, buffer);
	                }
	          
	                iter.remove();	                
	            }
	            // when we have all of the servers positive responses - break
	            if (stored == this.numServers) break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//keep track of how many servers have successfully stored the request
			
		}
	}
	
	public StringBuffer get(Request request){
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int serverId = getServerIndex(request.getKey(), this.numServers);
		SocketChannel server = this.serverConnections.get(serverId);
		buffer.put(request.content.getBytes());
		sendMessage(server, buffer);
		StringBuffer response = this.readMessage(server, buffer);		
		return response;
	}
	
	public String multi_get(Request request){
		StringBuffer response = new StringBuffer("");
		if (!readSharded){
			response = get(request);
		}else{
			String[] keys = request.getKey().split(" ");
			for(int i=0;i<keys.length; i++){
				Request smallReq = new Request("get " + keys[i]);
				response.append(get(smallReq)+"\r\n");
			}
		}
		return response.toString();
	}
	
	public StringBuffer readMessage(SocketChannel socketChannel,ByteBuffer buffer){
		StringBuffer readMessage = new StringBuffer("");
		try {
			int readByte =socketChannel.read(buffer);

			while(readByte!=0){				
				//String request;
				buffer.flip();
				//find a way to turn str buffer to string directly
				while(buffer.hasRemaining()) readMessage.append((char) buffer.get());
				buffer.clear();
				readByte = socketChannel.read(buffer);
			} 
		}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		return readMessage;
	}
	
	public void sendMessage(SocketChannel server, ByteBuffer buffer){
		try {
		    buffer.flip();
	        server.write(buffer);
	        buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
