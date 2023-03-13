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

    private static String IP = "127.0.0.1";
    private static int PORT = 2137;
    private static CommunicationHandler communicationHandler;
    private static final LinkedList<Socket> sockets = new LinkedList<>();
    private static final HashMap<String, Socket> node_sockets = new HashMap<>();
    private static final HashMap<String, String> local_packet_history = new HashMap<>();
    private static int[] data = new int[2];
    private static boolean terminate = false;

    public static void main(String[] args) {
        communicationHandler = new CommunicationHandler();
        setUp(args);
        awaitConnections();
        tearDown();
    }

    private static void awaitConnections() {

        // Create new non-blocking server socket channel
        ServerSocketChannel serverSocketChannel = null;
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.configureBlocking(false);
            log("Node socket channel hearing on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        while (true) {
            try {

                // add new connection if there is one
                SocketChannel socketChannel = serverSocketChannel != null ? serverSocketChannel.accept() : null;
                if (socketChannel != null) {
                    log("Socket connected, adding");
                    sockets.add(socketChannel.socket());
                }

                //remove closed sockets
                sockets.removeIf(Socket::isClosed);

                // handle incoming messages
                for (Socket socket : sockets) {
                    if (socket.isClosed())
                        continue;
                    communicationHandler.pullMessage(socket);
                    String incomingMessage = communicationHandler.getIncomingMessage();
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

    private static void setUp(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-tcpport":
                    PORT = Integer.parseInt(args[++i]);
                    break;
                case "-record":
                    String[] node_data = args[++i].split(":");
                    data[0] = Integer.parseInt(node_data[0]);
                    data[1] = Integer.parseInt(node_data[1]);
                    break;
                case "-connect":
                    String[] connect_data = args[++i].split(":");
                    String address = connect_data[0];
                    int port = Integer.parseInt(connect_data[1]);
                    try {
                        Socket socket = new Socket(address, port);
                        sockets.add(socket);
                        IP = socket.getLocalAddress().getHostAddress();
                        communicationHandler.setOutgoingMessage("handshake" + " " + IP + ":" + PORT);
                        communicationHandler.pushMessage(socket);
                        log("Connected to node " + address + ":" + port);
                        node_sockets.put(socket.getInetAddress().getHostAddress() + ":" + port, socket);
                        log("Added node " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " known as " + socket.getInetAddress().getHostAddress() + ":" + port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private static void tearDown() {
        try {
            for (Socket socket : sockets) {
                communicationHandler.setOutgoingMessage("OK");
                communicationHandler.pushMessage(socket);
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Node terminated, all sockets closed");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    private static void log(String message) {
        System.out.println(message);
    }

    public static HashMap<String, String> getLocal_packet_history() {
        return local_packet_history;
    }

    public static void addToPacketHistory(String id, String origin) {
        DatabaseNode.local_packet_history.put(id, origin);
    }

    public static int[] getData() {
        return data;
    }

    public static void setData(int[] data) {
        DatabaseNode.data = data;
    }

    public static LinkedList<Socket> getSockets() {
        return sockets;
    }

    public static HashMap<String, Socket> getNode_sockets() {
        return node_sockets;
    }

    public static String getIP() {
        return IP;
    }

    public static int getPORT() {
        return PORT;
    }

    public static CommunicationHandler getCommunicationHandler() {
        return communicationHandler;
    }
}
