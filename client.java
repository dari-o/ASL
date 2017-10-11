import java.io.*;
import java.net.*;

public class client{
    public static int valueSize = 64;
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
	    
	    while((userInput = stdIn.readLine()) != null){
		
		sendCommand(userInput, in, out, stdIn);
		System.out.println("Enter a command");
	    }
	} catch (UnknownHostException e){
	    System.err.println("Don't know about host " + hostName);
	    System.exit(1);
	}catch (IOException e){
	    System.err.println("Couldn't get I/O for the connection to " + hostName);
	    System.exit(1);
	}
    }
    public static void sendCommand(String userInput,BufferedReader in, PrintWriter out, BufferedReader stdIn){
	    
	if(userInput.startsWith("set")){
	    boolean isSet = set(userInput, in, out, stdIn);
	}else if(userInput.startsWith("get")){
	    String data;
	    System.out.println("get data");
	    data = get(userInput, in, out, stdIn);
	}

    }

    public static String get(String userInput, BufferedReader in, PrintWriter out, BufferedReader stdIn){
	out.println(userInput);
	String value;
	String data = "";
	try{
	    value = in.readLine();
	    boolean inDb = true;
	    
	    if (value.equals("END")){
		inDb = false;
		System.out.println(value);
	    }else{
		data = in.readLine();
		//END message
		in.readLine();
		System.out.println("Server: " + value);
		System.out.println("Server: " + data);
	    }
	}catch(IOException e){
	    System.err.println("Couldn't read the input ");
	    System.exit(1);
	}
	return data;
    }	
    public static boolean set(String userInput, BufferedReader in, PrintWriter out, BufferedReader stdIn){
	boolean stored = false;
	boolean error = false;
	try{   
	    
	    String key = userInput.split(" ")[1];
	    System.out.printf("Please enter the value with key %s\n", key);
	    String value = stdIn.readLine();
	    int hashedKey = key.hashCode();
	    out.println(userInput);
	    out.println(value + "\r");
	    String result;
	    result = in.readLine();
	    System.out.println("Server: " + result);
	    if(result.equals("STORED")) stored = true;
	    else error = true;
	    //out.println("\n");
	    //END message
	    //in.readLine();
	//TO DO:
	}catch(IOException e){
	    System.err.println("Couldn't read the input ");
	    System.exit(1);
	}
	return stored; 
    }
    public static boolean isError(String serverResponse){
	return false;
    }
}
