package middleware;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.io.*;
import java.nio.channels.*;
public class MyMiddleware{

	
	private int port;
	
	public MyMiddleware(int port){
		this.port = port;
	}
	
	public void startWorkers(ExecutorService executor){
		
		
	}
	
	public void connectToClients(Queue<SocketChannel> requestQueue, Selector selector){
		try {
				//create a selector and a socket channel. Register channel on the selector
				ServerSocketChannel middlewareSocket = ServerSocketChannel.open();
				middlewareSocket.socket().bind(new InetSocketAddress("localhost", this.port));
				
				
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
	                + port + " or listening for a connection");
	            System.out.println(e.getMessage());
	        }	
		
	}
	public static void main(String[] args){
		//Open a socket and listen
		int port = Integer.parseInt(args[0]);
		System.out.println("port number is: " + port);
		final Queue<SocketChannel> requestQueue = new LinkedBlockingQueue<SocketChannel>();
		MyMiddleware middleware = new MyMiddleware(port);
		//initialize the queue
		Selector selector;
		try {
			selector = Selector.open();
			ExecutorService executorService = Executors.newFixedThreadPool(10);			
			//start workers 
			executorService.execute(new WorkerThread(requestQueue, selector));			
			//middleware.startWorkers(executorService);
			middleware.connectToClients(requestQueue, selector);
			executorService.shutdown();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
