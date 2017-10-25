package test;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.io.*;

public class EchoClient {
    public static SocketChannel client;
    public static ByteBuffer buffer;
    public static EchoClient instance;
 
    public static EchoClient start() {
        if (instance == null)
            instance = new EchoClient();
 
        return instance;
    }
 
    public static void stop() throws IOException {
        client.close();
        buffer = null;
    }
 
    public EchoClient() {
        try {
            client = SocketChannel.open(new InetSocketAddress("localhost", 12345));
            buffer = ByteBuffer.allocate(1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    public String sendMessage(String msg) {
        buffer.put(msg.getBytes());
        StringBuffer response = new StringBuffer("");

        try {
	    buffer.flip();
            client.write(buffer);
            buffer.clear();
            int bytesRead = client.read(buffer);
            System.out.println(bytesRead);
	    buffer.flip();
	    while(buffer.hasRemaining()) response.append((char)buffer.get());
            System.out.println("response=" + response.toString() +" end of response!");
            //
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
 
    }
}
