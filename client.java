import java.io.*;
import java.net.*;
public static int valueSize = 64;
public class EchoClient{
	public static void main(String[] args) throws IOException{
		if(args.length != 2){
			System.err.println("blah blah");
			System.exit(1);
		}
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		try(
				Socket echoSocket = new Socket(hostName, portNumber);
				PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
				){
			String userInput;
			sendCommand(userInput);
			getServerResponse(in);
			while((userInput = stdIn.readLine()) != null){
				sendCommand(userInput);
			}
		} catch (UnknownHostException e){
			System.err.println("Don't know about host " + hostName);
			System.exit(1);
		}catch (IOException e){
			System.err.println("Couldn't get I/O for the connection to " + hostName);
			System.exit(1);
		}
	}
	public void sendCommand(String userInput){
		if(userInput.startsWith('set')){
			boolean isSet = set(userInput);
		}else if(userInput.startsWith('get'))
			String data = get(userInput);

	}

	public String get(String key){
		out.println(userInput);
		String value = in.readline();
		boolean inDb = true;
		if (value.equals('END')){
			inBd = false;
		}else{
			String data = in.readLine();
			in.readLine();
			System.out.println("Server: " + value);
			System.out.println("Server: " + data);
		}
		
	}	
	public boolean set(String userInput, String value){
		String value = stdIn.readLine();
		String key = userInput.split(' ')[0];
		int hashedKey = key.hashCode();
	}
}
