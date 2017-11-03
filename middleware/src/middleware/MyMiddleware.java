package middleware;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.util.List;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
public class MyMiddleware{
	
	String myIp;
	int myPort = 0;
	List<String> mcAddresses = null;
	int numThreadsPTP = -1;
	boolean readSharded = false;
	double numServers;
	List<Double> serverPoints;
	public static Queue<Request> requestQueue = new LinkedBlockingQueue<Request>();
	
	public MyMiddleware(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded){
		this.myPort = myPort;
		this.myIp = myIp;
		this.mcAddresses = mcAddresses;
		this.numThreadsPTP = numThreadsPTP;
		this.readSharded = readSharded;
		this.numServers = (double) mcAddresses.size();
	}
	
	
	public void connectToClients(Selector selector){
		try {
				//create a selector and a socket channel. Register channel on the selector
				ServerSocketChannel middlewareSocket = ServerSocketChannel.open();
				middlewareSocket.socket().bind(new InetSocketAddress("localhost", this.myPort));
				
				
				//configure non blocking mode. Otherwise, can't use selector
				middlewareSocket.configureBlocking(false);					
				middlewareSocket.register(selector, SelectionKey.OP_ACCEPT);				
				while (true) {
					// select a channel 
		            selector.select();
		            Set<SelectionKey> selectedKeys = selector.selectedKeys();
		            Iterator<SelectionKey> iter = selectedKeys.iterator();
		            while (iter.hasNext()) {
		 
		                SelectionKey key = iter.next();
		                // connect to a client when ready. Register the client for a read operation
		                if (key.isAcceptable()) {
		                    SocketChannel client = middlewareSocket.accept();
		                    client.configureBlocking(false);
		                    client.register(selector, SelectionKey.OP_READ);
		                    System.out.println("Connected to a client");
		                }
		                // read events are for client channels. Add "request" to the queue
		                if (key.isReadable()) {
		                	readFromClient(key);
		                    System.out.println("Number of requests: " + MyMiddleware.requestQueue.size());
		                }
		                iter.remove();
		            }
		        }	         
	          
	        } catch (IOException e) {
	            System.out.println("Exception caught when trying to listen on port "
	                + this.myPort + " or listening for a connection");
	            System.out.println(e.getMessage());
	        }	
		
	}
	
	
	public void run(){
		Selector selector;
		try {
			selector = Selector.open();
			ExecutorService executorService = Executors.newFixedThreadPool(2);			
			//start workers 
			executorService.execute(new WorkerThread(/*requestQueue,*/ this.mcAddresses, readSharded));			
			//middleware.startWorkers(executorService);
			this.connectToClients(selector);
			//while(!executorService.isTerminated()) System.out.println("TRUEEEEEEEEEEEEEEEEEEEEE");

			executorService.shutdown();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void readFromClient(SelectionKey key) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel client = (SocketChannel) key.channel();
        
        StringBuffer readMessage = new StringBuffer("");
		int readByte = client.read(buffer);
		while(readByte > 0){				
			//String request;
			buffer.flip();
			//find a way to turn str buffer to string directly
			while(buffer.hasRemaining()) readMessage.append((char) buffer.get());				
			buffer.clear();
			readByte = client.read(buffer);
		}
		
		if(key.attachment()==null) {
			System.out.println("new message from client!");
			Request request = new Request(readMessage, client);
			if(request.isComplete()) MyMiddleware.requestQueue.add(request);
			else key.attach(request);			
		}else {
			Request request = (Request) key.attachment();
			if(request.append(readMessage)) {
				key.attach(null);
				MyMiddleware.requestQueue.add(request);
			}
				
		}
		
	}
}
