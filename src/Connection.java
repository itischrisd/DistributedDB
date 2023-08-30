import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Connection extends Socket {

    private final DataOutputStream outToClient;
    private final BufferedReader inFromClient;

    public Connection(String host, int port) throws IOException {
        super(host, port);
        inFromClient = new BufferedReader(new InputStreamReader(this.getInputStream()));
        outToClient = new DataOutputStream(this.getOutputStream());
    }

    public void pushMessage(String outgoingMessage) throws IOException {
        outToClient.writeBytes(outgoingMessage + '\n');
        outToClient.flush();
        System.out.println("Sent packet: " + outgoingMessage + " --TO-- " + getRemoteAddress());
    }

    public String pullMessage() throws IOException {
        String incomingMessage = "";
        if (inFromClient.ready()) {
            incomingMessage = inFromClient.readLine();
            System.out.println("Received packet: " + incomingMessage + " --FROM-- " + getRemoteAddress());
        }
        return incomingMessage;
    }

    public String getRemoteAddress() {
        return getInetAddress().getHostAddress() + ":" + getPort();
    }

    public String getRemoteHost() {
        return getInetAddress().getHostAddress();
    }
}
