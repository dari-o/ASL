package middleware;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.io.*;


//Class for the worker 
public class WorkerThread implements Runnable{
	Queue<SocketChannel> requestQueue;
	Selector selector;
	
	public WorkerThread(Queue<SocketChannel> requestQueue, Selector selector){
		this.requestQueue = requestQueue;
		this.selector = selector;
	}
	
	
	// method that simply sends the request to the server and accepts the response
	public void sendRequestToServer(int portNumber, String hostName, String request, int requestType){
		try(
				Socket echoSocket = new Socket(hostName, portNumber);
				PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
				){
			
			/*sendCommand
			 * command types: 
			 * 0 - set
			 * 1 - get
			 * 2 - multi get
			 */
			if(requestType==0){
				boolean isSet = set(request, in, out);
			}else if(requestType==1){
				String value = get(request, in, out);
				if (value != null)
					returnToClient();
			}	
			
		} catch (UnknownHostException e){
			System.err.println("Don't know about host " + hostName);
			System.exit(1);
		}catch (IOException e){
			System.err.println("Couldn't get I/O for the connection to " + hostName);
			System.exit(1);
		}
	}
	private void returnToClient() {
		// TODO Auto-generated method stub
		
	}
	// method to process the request
	public void sendCommand(String request, int requestType){
		
	}

	public String get(String key, BufferedReader in, PrintWriter out){
		out.println(key);
		String data = null;
		// if value for key is not there server response it's "END"
		//
		String serverResponse;
		try {
			serverResponse = in.readLine();		
			if (!serverResponse.equals("END")){
				// serverResponse - "VALUE key flag numBytes
				//read DATA
				data = in.readLine();
				// read "END"
				in.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	public boolean set(String request, BufferedReader in, PrintWriter out){
		String[] key_val = request.split(" ");
		// get number of Bytes
		int numBytes = key_val[1].getBytes().length; 
		String command = "set ";
		//TODO 
		return false;
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
		ByteBuffer buffer = ByteBuffer.allocate(1536);
		try {
			//System.out.println("Ok we are trying to process some message");
			int readByte =client.read(buffer);
			
			while(readByte!=0){
				StringBuffer request = new StringBuffer("");
				buffer.flip();
				
				while(buffer.hasRemaining()) request.append((char) buffer.get());
				CommandType type = getCommandType(request.charAt(0));
				System.out.println(request.toString());
				//System.out.println(request.toString().split("\n")[0]);
				System.out.println(type.toString());
				buffer.clear();

				readByte = client.read(buffer);
				System.out.println(readByte);

				/*if( type== CommandType.SET){
					System.out.println("This should be the value" + new String(buffer.array()));
				}*/
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String hashedKey(){
		// TODO 
		return null;
	}
	
	public CommandType getCommandType(char firstChar){
		if (firstChar == 'g')
			return CommandType.GET;
		if (firstChar == 's')
			return CommandType.SET;
		if (firstChar == 'm')
			return CommandType.MULTI_GET;
		return null;
	}
}
