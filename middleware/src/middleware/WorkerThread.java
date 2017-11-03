package middleware;

import org.apache.log4j.Logger;

import org.apache.log4j.BasicConfigurator;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.*;


//Class for the worker 
public class WorkerThread implements Runnable{
	//static Logger log = Logger.getLogger("bacon");
	//Queue<SocketChannel> requestQueue;
	Selector selector;
	double numServers;
	boolean readSharded = false;
	List<SocketChannel> serverConnections;
	Logger log = Logger.getLogger("Worker Thread");
	
	public WorkerThread(/*Queue<SocketChannel> requestQueue,*/ List<String> mcAddresses,
			boolean readSharded) throws IOException{
		//this.requestQueue = requestQueue;
		this.selector = Selector.open();
		this.numServers = (double) mcAddresses.size();
		this.readSharded = readSharded;
		this.serverConnections = openServerConnections(mcAddresses);
	}

	
	private List<SocketChannel> openServerConnections(List<String> mcAddresses) {
		List<SocketChannel> serverConnections = new ArrayList<SocketChannel>();
		for(int i=0; i<mcAddresses.size();i++){
			try {
				String[] info = mcAddresses.get(i).split(":");
				SocketChannel server = SocketChannel.open(new InetSocketAddress(info[0], Integer.parseInt(info[1])));
				server.configureBlocking(false);
				server.register(this.selector, SelectionKey.OP_READ); 
				serverConnections.add(server);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return serverConnections;
	}


	public void run(){
		
		//check if there is Something in the queue
		while(true) {			
			Request request = null;
			//System.out.println(Thread.currentThread().isAlive());
			///synchronized(MyMiddleware.requestQueue){
				//System.out.println("Checking the queue");
			while(MyMiddleware.requestQueue.isEmpty());
			request= MyMiddleware.requestQueue.poll();
			processClientMessage(request);
			//}			
		}
	}
	private void processClientMessage(Request request) {
		if (request == null) return;
		// allocate buffer to receive message
		// TODO deal with incomplete requests and multigets
		try {
			ByteBuffer buffer = ByteBuffer.allocate(1024);
			SocketChannel client = request.getClient();
			/* read the message from client
			System.out.println("Reading a message from client");
			try {
				StringBuffer fromClient = readMessage(client, buffer);
			
				if(fromClient.length()==0) {
					System.out.println("Got an empty message will break out and try again");
					return;
					//fromClient = readMessage(client, buffer);
					/*buffer.put("ERROR\r\n".getBytes());
				sendMessage(client, buffer);
				buffer.clear();
				}
				Request request = new Request(fromClient);
				if(!request.isComplete()) fromClient.append(readMessage(client, buffer));*/
				StringBuffer response = null;
				switch(request.getType()){
				case SET:
					response = this.set(request, buffer);
				case GET:
					response = this.get(request.content.toString(), request.getKey(), buffer);
				case MULTI_GET:
					response = this.multi_get(request, buffer);
				}
			
				System.out.println("Sending response back to client");
				buffer.put(response.toString().getBytes());
				sendMessage(client, buffer);
				buffer.clear();
			}catch(IOException e) {
				System.out.println("Client connection exception");
				return;
			}
			// process message and send to servers

			
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

		
	public StringBuffer set(Request request, ByteBuffer buffer) throws IOException{
		//ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(request.content.toString().getBytes());
		for (int i=0; i<this.numServers; i++){
			SocketChannel server = this.serverConnections.get(i);
			System.out.println("sending requesto to server");
			this.sendMessage(server, buffer);
		}
		outer: while (true){
			int stored=0;
			String response = null;
			this.selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			Iterator<SelectionKey> iter = selectedKeys.iterator();
			while (iter.hasNext()) { 
				SelectionKey key = iter.next();
				// read events are for client channels. Add "request" to the queue
				if (key.isReadable()) {
					SocketChannel server = (SocketChannel) key.channel();
					System.out.println("reading response from server");
					response = this.readMessage(server, buffer).toString();
					//keep track of how many servers have successfully stored the request
					if(response.equals("STORED\r\n"))
						stored++;
					else
						this.sendMessage(server, buffer);
				}         
				iter.remove();
				// when we have all of the servers positive responses - break
				if (stored == this.numServers) break outer;
			}
		}
		return response;
	}
	
	public StringBuffer get(String requestContent, String requestKey, ByteBuffer buffer) throws IOException{
		
		//ByteBuffer buffer = ByteBuffer.allocate(1024);
		int serverId = getServerIndex(requestKey, this.numServers);
		SocketChannel server = this.serverConnections.get(serverId);
		buffer.put(requestContent.getBytes());
		sendMessage(server, buffer);
		StringBuffer response = this.readMessage(server, buffer);		
		return response;
	}
	
	public StringBuffer multi_get(Request request, ByteBuffer buffer) throws IOException{
		StringBuffer response = new StringBuffer("");
		if (!readSharded){
			response = get(request.content.toString(), request.getKey(), buffer);
		}else{
			String[] keys = request.getKey().split(" ");
			for(int i=0;i<keys.length; i++){
				String smallReq = "get " + keys[i] + "\r\n";
				response.append(get(smallReq,  keys[i], buffer)+"\r\n");
			}
		}
		return response;
	}
	
	public StringBuffer readMessage(SocketChannel socketChannel,ByteBuffer buffer) throws IOException{
		//System.out.println("reading a message");
		
		
		StringBuffer readMessage = new StringBuffer("");
		int readByte =socketChannel.read(buffer);		
		while(readByte!=0){				
			//String request;
			buffer.flip();
			//find a way to turn str buffer to string directly
			while(buffer.hasRemaining()) readMessage.append((char) buffer.get());				
			buffer.clear();
			readByte = socketChannel.read(buffer);
		}
		return readMessage;


	}
	
	public void sendMessage(SocketChannel server, ByteBuffer buffer){
		try {
			//System.out.println("sending a message");
		    buffer.flip();
	        server.write(buffer);
	        buffer.clear();
		} catch (IOException e) {
		}
	}
	
}
