import java.io.*;
import java.net.Socket;

public class CommunicationHandler {

    private String incomingMessage;
    private String outgoingMessage;

    public CommunicationHandler() {
        incomingMessage = "";
        outgoingMessage = "";
    }

    public void pushMessage(Socket socket) {
        try {
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
            outToClient.writeBytes(outgoingMessage + '\n');
            outToClient.flush();
            System.out.println("Sent packet: " + outgoingMessage + " --TO-- " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            outgoingMessage = "";
        } catch (IOException e) {
            System.err.println("Sending failed - connection closed by client");
        }
    }

    public void pullMessage(Socket socket) {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (inFromClient.ready()) {
                incomingMessage = inFromClient.readLine();
                System.out.println("Received packet: " + incomingMessage + " --FROM-- " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
            }
        } catch (IOException e) {
            System.err.println("Receiving failed - connection closed by client");
        }
    }

    public String getIncomingMessage() {
        String temp = incomingMessage;
        incomingMessage = "";
        return temp;
    }

    public void setOutgoingMessage(String outgoingMessage) {
        this.outgoingMessage = outgoingMessage;
    }
}
