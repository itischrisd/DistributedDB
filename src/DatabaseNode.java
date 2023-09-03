import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class DatabaseNode {

    private static DatabaseNode instance;
    private final List<Connection> connections = new LinkedList<>();
    private final HashMap<String, String> localPacketHistory = new HashMap<>();
    private ServerSocketChannel incomingConnectionChannel;
    private String IP = "127.0.0.1";
    private int PORT;
    private int[] record = new int[2];
    private boolean terminate = false;

    public static void main(String[] args) throws IOException {
        instance = new DatabaseNode(args);
        instance.establishLocalIp();
        instance.createIncomingConnectionChannel();
        instance.awaitConnections();
        instance.tearDown();
    }

    public DatabaseNode(String[] parameters) throws IOException {
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

    public static DatabaseNode getInstance() {
        return instance;
    }

    private void setPortFromParameter(String port) {
        PORT = Integer.parseInt(port);
        log("Set up port: " + PORT);
    }

    private void setRecordFromParameter(String record) {
        String[] recordData = record.split(":");
        this.record[0] = Integer.parseInt(recordData[0]);
        this.record[1] = Integer.parseInt(recordData[1]);
        log("Set up record: " + this.record[0] + ":" + this.record[1]);
    }

    private void connectToNodeFromParameter(String nodeAddress) throws IOException {
        String[] connectionData = nodeAddress.split(":");
        String host = connectionData[0];
        int port = Integer.parseInt(connectionData[1]);

        Connection connection = new Connection(host, port);
        connections.add(connection);
        connection.pushMessage("handshake" + " " + IP + ":" + PORT);
        log("Connected to node: " + host + ":" + port);
        log("Added node " + connection.getRemoteAddress() + " known as " + connection.getRemoteHost() + ":" + port);
    }

    public void establishLocalIp() {
        if (connections.isEmpty()) {
            IP = "127.0.0.1";
        } else {
            IP = connections.get(0).getLocalAddress().getHostAddress();
        }
        log("Established local IP address as: " + IP);
    }

    public void awaitConnections() throws IOException {
        while (true) {
            attemptAddingIncomingConnection();
            removeClosedConnections();
            pollIncomingMessages();
            if (terminate) {
                log("Terminating...");
                break;
            }
        }
    }

    public void createIncomingConnectionChannel() throws IOException {
        incomingConnectionChannel = ServerSocketChannel.open();
        incomingConnectionChannel.bind(new InetSocketAddress(PORT));
        incomingConnectionChannel.configureBlocking(false);
        log("Incoming connection channel hearing on port: " + incomingConnectionChannel.getLocalAddress());
    }

    private void attemptAddingIncomingConnection() throws IOException {
        SocketChannel socketChannel = incomingConnectionChannel.accept();
        if (socketChannel != null) {
            connections.add(new Connection(socketChannel.socket()));
            log("New connection received, added to connection pool.");
        }
    }

    private void removeClosedConnections() {
        connections.removeIf(Connection::isClosed);
    }

    private void pollIncomingMessages() throws IOException {
        for (Connection connection : connections) {
            if (connection.isClosed()) continue;
            String incomingMessage = connection.pullMessage();
            if (incomingMessage.isEmpty()) continue;

            int indexOfSpace = incomingMessage.indexOf(" ");
            String command;
            if (indexOfSpace != -1) command = incomingMessage.substring(0, incomingMessage.indexOf(" "));
            else command = incomingMessage;


            switch (command) {
                case "OK":
                    receiveConnectionCloseSignal(connection);
                    break;
                case "terminate":
                    receiveTerminationSignal();
                    break;
                case "handshake":
                    receiveHandshake(connection, incomingMessage);
                    break;
                default:
                    receiveDatabaseRequest(incomingMessage, connection);
                    break;
            }
        }
    }

    private void receiveConnectionCloseSignal(Connection connection) {
        try {
            connection.close();
        } catch (IOException e) {
            log("Unnecessary connection closing attempt!");
        }
        log("Closed connection to node: " + connection.getDeclaredAddress());
    }

    private void receiveTerminationSignal() {
        terminate = true;
        log("Termination signal received.");
    }

    private void receiveHandshake(Connection connection, String handshake) {
        String IP = connection.getInetAddress().getHostAddress();
        String PORT = handshake.split(":")[1];
        connection.setDeclaredAddress(IP + ":" + PORT);
        log("Connection from " + IP + ":" + connection.getPort() + " identified as " + IP + ":" + PORT);
    }

    private void receiveDatabaseRequest(String request, Connection connection) {
        if (!Character.isDigit(request.charAt(0))) {
            NodePacket recv_packet = new NodePacket(request.split(" "));
            NodeLogic.doLogic(recv_packet, connection);
        } else {
            NodePacket recv_packet = new NodePacket(request);
            NodeLogic.doLogic(recv_packet, connection);
        }
    }

    private void tearDown() throws IOException {
        for (Connection connection : connections) {
            connection.pushMessage("OK");
            connection.close();
        }
        log("Node terminated, all connections closed. Press ENTER to end process...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public void addToPacketHistory(String id, String origin) {
        localPacketHistory.put(id, origin);
    }

    private void log(String message) {
        System.out.println(message);
    }

    public HashMap<String, String> getLocalPacketHistory() {
        return localPacketHistory;
    }

    public int[] getRecord() {
        return record;
    }

    public void setRecord(int[] record) {
        this.record = record;
    }

    public List<String> getNeighbours() {
        List<String> neighbours = new LinkedList<>();
        for (Connection connection : connections)
            neighbours.add(connection.getDeclaredAddress());
        return neighbours;
    }

    public String getDeclaredAddress() {
        return IP + ":" + PORT;
    }

    public Connection getNeighbour(String declaredAddress) {
        return connections.stream().filter(e -> e.getDeclaredAddress().equals(declaredAddress)).collect(Collectors.toList()).get(0);
    }

    public List<Connection> getConnections() {
        return connections;
    }
}
