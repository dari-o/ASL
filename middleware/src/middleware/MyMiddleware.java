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
import java.nio.channels.*;
public class MyMiddleware{
	
	String myIp;
	int myPort = 0;
	List<String> mcAddresses = null;
	int numThreadsPTP = -1;
	boolean readSharded = false;
	double numServers;
	List<Double> serverPoints;
	
	public MyMiddleware(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded){
		this.myPort = myPort;
		this.myIp = myIp;
		this.mcAddresses = mcAddresses;
		this.numThreadsPTP = numThreadsPTP;
		this.readSharded = readSharded;
		this.numServers = (double) mcAddresses.size();
	}
	
	
	public void connectToClients(Queue<SocketChannel> requestQueue, Selector selector){
		try {
				//create a selector and a socket channel. Register channel on the selector
				ServerSocketChannel middlewareSocket = ServerSocketChannel.open();
				middlewareSocket.socket().bind(new InetSocketAddress("localhost", this.myPort));
				
				
				//configure non blocking mode. Otherwise, can't use selector
				middlewareSocket.configureBlocking(false);				
				middlewareSocket.register(selector, SelectionKey.OP_ACCEPT);				
				System.out.println("will start waiting for a client");
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
		                    SocketChannel client = (SocketChannel) key.channel();
		                    requestQueue.add(client);
		        
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
		System.out.println("port number is: " + myPort);
		final Queue<SocketChannel> requestQueue = new LinkedBlockingQueue<SocketChannel>();
		//initialize the queue
		Selector selector;
		try {
			selector = Selector.open();
			ExecutorService executorService = Executors.newFixedThreadPool(10);			
			//start workers 
			executorService.execute(new WorkerThread(requestQueue, selector,this.mcAddresses, readSharded));			
			//middleware.startWorkers(executorService);
			this.connectToClients(requestQueue, selector);
			executorService.shutdown();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
}
