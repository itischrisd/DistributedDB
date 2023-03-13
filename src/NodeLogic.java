import java.io.IOException;
import java.net.Socket;

public class NodeLogic {

    public static void doLogic(NodePacket packet, Socket socket) {

        //handle message for the first time
        boolean first_time = isFirstTime(packet);
        if (first_time) {
            addMyselfToPacketTrace(packet);
            addPacketToLocalHistory(packet, socket);
            executeCommand(packet);
            if (packet.getCommand().equals("new-record")) {
                returnToClient(packet);
                return;
            }
        }

        //pass message to other nodes for consecutive times
        boolean nieghborhood_done = isNeighborhoodDone(packet);
        if (!nieghborhood_done) {
            passToNextNode(packet);
            return;
        }

        //return message up the graph
        boolean is_first_node = isFirstNode(packet);
        if (!is_first_node) {
            returnToNode(packet);
        } else {
            returnToClient(packet);
        }
    }

    private static boolean isFirstTime(NodePacket packet) {
        return !DatabaseNode.getLocal_packet_history().containsKey(packet.getID());
    }

    private static boolean isNeighborhoodDone(NodePacket packet) {
        return packet.getNode_trace().containsAll(DatabaseNode.getNode_sockets().keySet());
    }

    private static boolean isFirstNode(NodePacket packet) {
        return packet.getNode_trace().getFirst().equals(DatabaseNode.getIP() + ":" + DatabaseNode.getPORT());
    }

    private static void addMyselfToPacketTrace(NodePacket packet) {
        packet.getNode_trace().add(DatabaseNode.getIP() + ":" + DatabaseNode.getPORT());
    }

    private static void addPacketToLocalHistory(NodePacket packet, Socket socket) {
        DatabaseNode.addToPacketHistory(packet.getID(), socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
    }

    private static void passToNextNode(NodePacket packet) {
        for (String socket_address : DatabaseNode.getNode_sockets().keySet()) {
            if (!packet.getNode_trace().contains(socket_address)) {
                Socket neighbor_socket = DatabaseNode.getNode_sockets().get(socket_address);
                DatabaseNode.getCommunicationHandler().setOutgoingMessage(packet.toString());
                DatabaseNode.getCommunicationHandler().pushMessage(neighbor_socket);
                return;
            }
        }
    }

    private static void returnToNode(NodePacket packet) {
        String ID = packet.getID();
        String origin = DatabaseNode.getLocal_packet_history().get(ID);
        Socket node_socket;
        for (Socket s : DatabaseNode.getNode_sockets().values()) {
            if ((s.getInetAddress().getHostAddress() + ":" + s.getPort()).equals(origin)) {
                node_socket = s;
                DatabaseNode.getCommunicationHandler().setOutgoingMessage(packet.toString());
                DatabaseNode.getCommunicationHandler().pushMessage(node_socket);
                return;
            }
        }
    }

    private static void returnToClient(NodePacket packet) {
        String client_socket_address = DatabaseNode.getLocal_packet_history().get(packet.getID());
        Socket client_socket;
        for (Socket s : DatabaseNode.getSockets()) {
            if ((s.getInetAddress().getHostAddress() + ":" + s.getPort()).equals(client_socket_address)) {
                client_socket = s;
                DatabaseNode.getCommunicationHandler().setOutgoingMessage(packet.getResponse());
                DatabaseNode.getCommunicationHandler().pushMessage(client_socket);
                try {
                    client_socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private static void executeCommand(NodePacket packet) {
        switch (packet.getCommand()) {
            case "set-value":
                set_value(packet);
                break;
            case "get-value":
                get_value(packet);
                break;
            case "find-key":
                find_key(packet);
                break;
            case "get-max":
                get_max(packet);
                break;
            case "get-min":
                get_min(packet);
                break;
            case "new-record":
                new_record(packet);
                break;
        }
    }

    private static void set_value(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest().split(":")[0]);
        int value = Integer.parseInt(packet.getRequest().split(":")[1]);
        if (key == DatabaseNode.getData()[0]) {
            DatabaseNode.getData()[1] = value;
            packet.setResponse("OK");
        }
    }

    private static void get_value(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest());
        if (key == DatabaseNode.getData()[0]) {
            packet.setResponse(String.valueOf(DatabaseNode.getData()[0]) + ':' + DatabaseNode.getData()[1]);
        }
    }

    private static void find_key(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest());
        if (key == DatabaseNode.getData()[0]) {
            packet.setResponse(DatabaseNode.getIP() + ":" + DatabaseNode.getPORT());
        }
    }

    private static void get_max(NodePacket packet) {
        int value = Integer.parseInt(packet.getResponse().split(":")[1]);
        if (value < DatabaseNode.getData()[1]) {
            packet.setResponse(String.valueOf(DatabaseNode.getData()[0]) + ':' + DatabaseNode.getData()[1]);
        }
    }

    private static void get_min(NodePacket packet) {
        if (packet.getResponse().equals("NUL")) {
            packet.setResponse(String.valueOf(DatabaseNode.getData()[0]) + ':' + DatabaseNode.getData()[1]);
        } else {
            int value = Integer.parseInt(packet.getResponse().split(":")[1]);
            if (value > DatabaseNode.getData()[1]) {
                packet.setResponse(String.valueOf(DatabaseNode.getData()[0]) + ':' + DatabaseNode.getData()[1]);
            }
        }
    }

    private static void new_record(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest().split(":")[0]);
        int value = Integer.parseInt(packet.getRequest().split(":")[1]);
        int[] new_data = {key, value};
        DatabaseNode.setData(new_data);
    }
}
