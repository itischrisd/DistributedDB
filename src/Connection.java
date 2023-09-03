import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;

public class Connection {

    private final DataOutputStream outToClient;
    private final BufferedReader inFromClient;
    private final Socket socket;
    private String declaredAddress;

    public Connection(String host, int port) throws IOException {
        socket = new Socket(host, port);
        declaredAddress = host + ":" + port;
        inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outToClient = new DataOutputStream(socket.getOutputStream());
    }

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        declaredAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outToClient = new DataOutputStream(socket.getOutputStream());
    }

    public void pushMessage(String outgoingMessage) throws IOException {
        outToClient.writeBytes(outgoingMessage + '\n');
        outToClient.flush();
        System.out.println("Sent packet: " + outgoingMessage + " --TO-- " + getRemoteAddress());
    }

    public String pullMessage() throws IOException {
        String incomingMessage = "";
//        if (inFromClient.ready()) {
            incomingMessage = inFromClient.readLine();
            System.out.println("Received packet: " + incomingMessage + " --FROM-- " + getRemoteAddress());
//        }
        return incomingMessage;
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    public String getRemoteHost() {
        return socket.getInetAddress().getHostAddress();
    }

    public String getDeclaredAddress() {
        return declaredAddress;
    }

    public void setDeclaredAddress(String declaredAddress) {
        this.declaredAddress = declaredAddress;
    }

    @Override
    public String toString() {
        return getDeclaredAddress();
    }

    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void close() throws IOException {
        socket.close();
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public int getPort() {
        return socket.getPort();
    }
}
