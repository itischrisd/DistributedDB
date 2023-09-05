import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Connection {

    private final String declaredAddress;
    private DataOutputStream outToClient = null;
    private BufferedReader inFromClient = null;
    private Socket socket;

    public Connection(String address) {
        String[] connectionData = address.split(":");
        String host = connectionData[0];
        int port = Integer.parseInt(connectionData[1]);
        try {
            socket = new Socket(host, port);
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log("[ERROR] While creating streams with " + this);
            e.printStackTrace();
        }
        declaredAddress = socket.getInetAddress().getHostAddress() + ":" + port;
    }

    public Connection(Socket socket) {
        this.socket = socket;
        declaredAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        try {
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToClient = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log("[ERROR] While creating streams with " + this);
            e.printStackTrace();
        }
    }

    private static void log(String message) {
        System.out.println(message);
    }

    public void pushMessage(String outgoingMessage) {
        try {
            outToClient.writeBytes(outgoingMessage + '\n');
            outToClient.flush();
        } catch (IOException e) {
            log("[ERROR] While pushing message to " + this);
            e.printStackTrace();
        }
        log("[SENT] " + outgoingMessage + " --TO-- " + declaredAddress);
    }

    public String pullMessage() {
        String incomingMessage = null;
        try {
            incomingMessage = inFromClient.readLine();
        } catch (IOException e) {
            log("[ERROR] While pulling message from " + this);
            e.printStackTrace();
        }
        log("[RECEIVED] " + incomingMessage + " --FROM-- " + declaredAddress);
        return incomingMessage;
    }

    public String getLocalIp() {
        return socket.getLocalAddress().getHostAddress();
    }

    public String getDeclaredAddress() {
        return declaredAddress;
    }

    public void close() {
        try {
            inFromClient.close();
            outToClient.close();
            socket.close();
        } catch (IOException e) {
            log("[ERROR] While closing connection with " + this);
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getDeclaredAddress();
    }
}