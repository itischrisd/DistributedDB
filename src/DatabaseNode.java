import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class DatabaseNode {

    private static DatabaseNode instance;
    private final List<String> attachedNodes = new LinkedList<>();
    private final HashMap<String, Connection> clientConnections = new HashMap<>();
    private final HashMap<String, String> packetIdToOriginHistory = new HashMap<>();
    private ServerSocket serverSocket;
    private String IP = "127.0.0.1";
    private int PORT;
    private int[] record = new int[2];
    private boolean running = true;

    public DatabaseNode(String[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            switch (parameters[i]) {
                case "-tcpport":
                    setPortFromParameter(parameters[++i]);
                    break;
                case "-record":
                    setRecordFromParameter(parameters[++i]);
                    break;
                case "-connect":
                    connectToNodeFromParameter(parameters[++i]);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        instance = new DatabaseNode(args);
        instance.createIncomingConnectionChannel();
        instance.awaitConnections();
        instance.tearDown();
    }

    public static DatabaseNode getInstance() {
        return instance;
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private void setPortFromParameter(String port) {
        PORT = Integer.parseInt(port);
        log("[SETUP] Set up port: " + PORT);
    }

    private void setRecordFromParameter(String record) {
        String[] recordData = record.split(":");
        this.record[0] = Integer.parseInt(recordData[0]);
        this.record[1] = Integer.parseInt(recordData[1]);
        log("[SETUP] Set up record: " + this.record[0] + ":" + this.record[1]);
    }

    private void connectToNodeFromParameter(String address) {
        Connection connection = new Connection(address);
        connection.pushMessage(IP + ":" + PORT + " handshake");
        IP = connection.getLocalIp();
        address = connection.getDeclaredAddress();
        connection.close();
        attachedNodes.add(address);
        log("[SETUP] Handshake with node: " + address);
    }

    public void createIncomingConnectionChannel() {
        try {
            serverSocket = new ServerSocket(PORT);
            log("[SETUP] Incoming connection channel hearing on port: " + PORT);
        } catch (IOException e) {
            log("[ERROR] While creating incoming socket channel.");
            e.printStackTrace();
        }
    }

    public void awaitConnections() {
        while (running) {
            try {
                acceptIncomingMessages();
            } catch (IOException e) {
                log("[ERROR] While waiting for next message.");
                e.printStackTrace();
            }
        }
    }

    private void acceptIncomingMessages() throws IOException {
        Socket socket = serverSocket.accept();
        Connection connection = new Connection(socket);
        String incomingMessage = connection.pullMessage();

        if (NodePacket.isInternalRequest(incomingMessage)) {
            processInternalRequest(incomingMessage);
        } else {
            clientConnections.put(connection.getDeclaredAddress(), connection);
            processClientRequest(incomingMessage, connection.getDeclaredAddress());
        }
    }

    private void processInternalRequest(String request) {
        String command = request.substring(request.lastIndexOf(' ') + 1);
        switch (command) {
            case "goodbye":
                receiveDetachNodeSignal(request);
                break;
            case "handshake":
                receiveHandshake(request);
                break;
            default:
                PacketProcessor.process(NodePacket.makePacket(request));
                break;
        }
    }

    private void processClientRequest(String request, String sender) {
        if (request.equals("terminate")) {
            receiveTerminationSignal();
        } else {
            NodePacket packet = NodePacket.makePacket(request);
            packet.setLastSender(sender);
            PacketProcessor.process(packet);
        }
    }

    private void receiveHandshake(String handshake) {
        String node = handshake.split(" ")[0];
        attachedNodes.add(node);
        log("[RUNNING] Attached node: " + node);
    }

    private void receiveDetachNodeSignal(String request) {
        String node = request.split(" ")[0];
        attachedNodes.remove(node);
        log("[RUNNING] Detached node: " + node);
    }

    private void receiveTerminationSignal() {
        running = false;
        clientConnections.forEach((k, v) -> {
            v.pushMessage("OK");
            v.close();
        });
        log("[SHUTDOWN] Termination signal received.");
    }

    public void sendToNextNode(NodePacket packet) {
        List<String> unvisited = new LinkedList<>(attachedNodes);
        unvisited.removeAll(packet.getVisitedNodeHistory());
        Connection connection = new Connection(unvisited.get(0));
        connection.pushMessage(packet.toString());
        connection.close();
    }

    public void returnToNode(NodePacket packet) {
        String ID = packet.getID();
        String origin = packetIdToOriginHistory.get(ID);
        Connection connection = new Connection(origin);
        connection.pushMessage(packet.toString());
        connection.close();
    }

    public void returnToClient(NodePacket packet) {
        String clientAddress = packetIdToOriginHistory.get(packet.getID());
        Connection connection = clientConnections.get(clientAddress);
        connection.pushMessage(packet.getResponse());
        connection.close();
        clientConnections.remove(clientAddress);
    }

    private void tearDown() {
        for (String node : attachedNodes) {
            Connection connection = new Connection(node);
            connection.pushMessage(IP + ":" + PORT + " goodbye");
            connection.close();
        }
        try {
            serverSocket.close();
            log("[SHUTDOWN] Node terminated, all connections closed. Press ENTER to end process...");
        } catch (IOException e) {
            log("[ERROR] While closing incoming socket channel.");
            e.printStackTrace();
        }
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public void addToPacketHistory(String id, String origin) {
        packetIdToOriginHistory.put(id, origin);
    }

    public HashMap<String, String> getPacketIdToOriginHistory() {
        return packetIdToOriginHistory;
    }

    public int[] getRecord() {
        return record;
    }

    public void setRecord(int[] record) {
        this.record = record;
    }

    public String getDeclaredAddress() {
        return IP + ":" + PORT;
    }

    public List<String> getAttachedNodes() {
        return attachedNodes;
    }
}