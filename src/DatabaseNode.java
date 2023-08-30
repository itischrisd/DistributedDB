import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;

public class DatabaseNode {

    private final HashMap<Connection, String> connections = new HashMap<>();
    private final HashMap<String, String> localPacketHistory = new HashMap<>();
    private String IP = "127.0.0.1";
    private int PORT = 2137;
    private int[] record = new int[2];
    private boolean terminate = false;

    public DatabaseNode(String[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            switch (parameters[i]) {
                case "-tcpport":
                    setPortFromParameter(parameters[i++]);
                    break;
                case "-record":
                    setRecordFromParameter(parameters[i++]);
                    break;
                case "-connect":
                    connectToNodeFromParameter(parameters[i++]);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        DatabaseNode databaseNode = new DatabaseNode(args);
        databaseNode.awaitConnections();
        tearDown();
    }

    private static void tearDown() {
        try {
            for (Socket socket : connections) {
                connection.setOutgoingMessage("OK");
                connection.pushMessage(socket);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Node terminated, all sockets closed");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public static void addToPacketHistory(String id, String origin) {
        DatabaseNode.localPacketHistory.put(id, origin);
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

    private void connectToNodeFromParameter(String nodeAddress) {
        String[] connectionData = nodeAddress.split(":");
        String host = connectionData[0];
        int port = Integer.parseInt(connectionData[1]);

        try {
            Connection connection = new Connection(host, port);
            connections.put(connection, connection.getInetAddress().getHostAddress() + ":" + port);
            connection.pushMessage("handshake" + " " + IP + ":" + PORT);
            log("Connected to node: " + host + ":" + port);
            log("Added node " + connection.getRemoteAddress() + " known as " + connection.getRemoteHost() + ":" + port);
            IP = connection.getLocalAddress().getHostAddress(); ///TODO nieeleganckie IP
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void awaitConnections() {

        // Create new non-blocking server socket channel
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.configureBlocking(false);
            log("Node socket channel hearing on port: " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (true) {
            try {

                // add new connection if there is one
                SocketChannel socketChannel = serverSocketChannel != null ? serverSocketChannel.accept() : null;
                if (socketChannel != null) {
                    log("Socket connected, adding");
                    connections.add(socketChannel.socket());
                }

                //remove closed sockets
                connections.removeIf(Socket::isClosed);

                // handle incoming messages
                for (Connection connection : connections) {
                    if (connection.isClosed()) continue;
                    String incomingMessage = connection.pullMessage();
                    switch (incomingMessage) {
                        case "OK":
                            for (Map.Entry<String, Socket> entry : node_sockets.entrySet()) {
                                if (entry.getValue().equals(socket)) {
                                    node_sockets.remove(entry.getKey());
                                    break;
                                }
                            }
                            log("Disconnected node " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                            break;
                    }
                    if (incomingMessage.isEmpty()) {
                        //noinspection UnnecessaryContinue
                        continue;
                    } else if (incomingMessage.startsWith("OK")) {
                        for (Map.Entry<String, Socket> entry : node_sockets.entrySet()) {
                            if (entry.getValue().equals(socket)) {
                                node_sockets.remove(entry.getKey());
                                break;
                            }
                        }
                        log("Disconnected node " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                    } else if (incomingMessage.equals("terminate")) {
                        log("Termination signal received");
                        terminate = true;
                        break;
                    } else if (incomingMessage.startsWith("handshake")) {
                        String IP = socket.getInetAddress().getHostAddress();
                        String PORT = incomingMessage.split(":")[1];
                        node_sockets.put(IP + ":" + PORT, socket);
                        log("Added node " + IP + ":" + socket.getPort() + " known as " + IP + ":" + PORT + ", my port is " + socket.getLocalPort());
                    } else if (!Character.isDigit(incomingMessage.charAt(0))) {
                        // handle command from client
                        NodePacket recv_packet = new NodePacket(incomingMessage.split(" "));
                        NodeLogic.doLogic(recv_packet, socket);
                    } else {
                        // handle data from other node
                        NodePacket recv_packet = new NodePacket(incomingMessage);
                        NodeLogic.doLogic(recv_packet, socket);
                    }
                }

                //exit if terminate requested
                if (terminate) {
                    log("Terminating");
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void log(String message) {
        System.out.println(message);
    }

    public HashMap<String, String> getLocal_packet_history() {
        return localPacketHistory;
    }

    public int[] getRecord() {
        return record;
    }

    public void setRecord(int[] record) {
        DatabaseNode.record = record;
    }

    public LinkedList<Socket> getConnections() {
        return connections;
    }

    public HashMap<String, Socket> getNode_sockets() {
        return node_sockets;
    }

    public String getIP() {
        return IP;
    }

    public int getPORT() {
        return PORT;
    }

    public Connection getCommunicationHandler() {
        return connection;
    }
}
