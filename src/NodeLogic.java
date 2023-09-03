import java.io.IOException;

public class NodeLogic {

    public static void doLogic(NodePacket packet, Connection connection) {

        if (isFirstTime(packet)) {
            addMyselfToPacketTrace(packet);
            addPacketToLocalHistory(packet, connection);
            executeCommand(packet);
            if (packet.getCommand().equals("new-record")) {
                returnToClient(packet);
                return;
            }
        }

        boolean nieghborhood_done = isNeighborhoodDone(packet);
        if (!nieghborhood_done) {
            passToNextNode(packet);
            return;
        }

        boolean is_first_node = isFirstNode(packet);
        if (!is_first_node) {
            returnToNode(packet);
        } else {
            returnToClient(packet);
        }
    }

    private static boolean isFirstTime(NodePacket packet) {
        return !DatabaseNode.getInstance().getLocalPacketHistory().containsKey(packet.getID());
    }

    private static boolean isNeighborhoodDone(NodePacket packet) {
        return packet.getVisitedNodeHistory().containsAll(DatabaseNode.getInstance().getNeighbours());
    }

    private static boolean isFirstNode(NodePacket packet) {
        return packet.getVisitedNodeHistory().getFirst().equals(DatabaseNode.getInstance().getDeclaredAddress());
    }

    private static void addMyselfToPacketTrace(NodePacket packet) {
        packet.getVisitedNodeHistory().add(DatabaseNode.getInstance().getDeclaredAddress());
    }

    private static void addPacketToLocalHistory(NodePacket packet, Connection connection) {
        DatabaseNode.getInstance().addToPacketHistory(packet.getID(), connection.getDeclaredAddress());
    }

    private static void passToNextNode(NodePacket packet) {
        for (String nodeAddress : DatabaseNode.getInstance().getNeighbours()) {
            if (!packet.getVisitedNodeHistory().contains(nodeAddress)) {
                Connection neighborConnection = DatabaseNode.getInstance().getNeighbour(nodeAddress);
                try {
                    neighborConnection.pushMessage(packet.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private static void returnToNode(NodePacket packet) {
        String ID = packet.getID();
        String origin = DatabaseNode.getInstance().getLocalPacketHistory().get(ID);
        for (Connection connection : DatabaseNode.getInstance().getConnections()) {
            if (connection.getDeclaredAddress().equals(origin)) {
                try {
                    connection.pushMessage(packet.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    private static void returnToClient(NodePacket packet) {
        String clientSocketAddress = DatabaseNode.getInstance().getLocalPacketHistory().get(packet.getID());
        for (Connection connection : DatabaseNode.getInstance().getConnections()) {
            if (connection.getDeclaredAddress().equals(clientSocketAddress)) {
                try {
                    connection.pushMessage(packet.getResponse());
                    connection.close();
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
        if (key == DatabaseNode.getInstance().getRecord()[0]) {
            DatabaseNode.getInstance().getRecord()[1] = value;
            packet.setResponse("OK");
        }
    }

    private static void get_value(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest());
        if (key == DatabaseNode.getInstance().getRecord()[0]) {
            packet.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
        }
    }

    private static void find_key(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest());
        if (key == DatabaseNode.getInstance().getRecord()[0]) {
            packet.setResponse(DatabaseNode.getInstance().getDeclaredAddress());
        }
    }

    private static void get_max(NodePacket packet) {
        int value = Integer.parseInt(packet.getResponse().split(":")[1]);
        if (value < DatabaseNode.getInstance().getRecord()[1]) {
            packet.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
        }
    }

    private static void get_min(NodePacket packet) {
        if (packet.getResponse().equals("NUL")) {
            packet.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
        } else {
            int value = Integer.parseInt(packet.getResponse().split(":")[1]);
            if (value > DatabaseNode.getInstance().getRecord()[1]) {
                packet.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
            }
        }
    }

    private static void new_record(NodePacket packet) {
        int key = Integer.parseInt(packet.getRequest().split(":")[0]);
        int value = Integer.parseInt(packet.getRequest().split(":")[1]);
        int[] new_data = {key, value};
        DatabaseNode.getInstance().setRecord(new_data);
    }
}
