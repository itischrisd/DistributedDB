import java.util.Arrays;
import java.util.LinkedList;

public class NodePacket {

    private final String ID;
    private final String command;
    private final String request;
    private final LinkedList<String> visitedNodeHistory;
    private String response;
    private String lastSender;

    private NodePacket(String[] packet) {
        this.ID = String.valueOf(System.currentTimeMillis());
        this.command = packet[0];
        if (packet.length > 1)
            this.request = packet[1];
        else
            this.request = "NUL";
        this.visitedNodeHistory = new LinkedList<>();
        switch (command) {
            case "set-value":
            case "get-value":
            case "find-key":
                this.response = "ERROR";
                break;
            case "get-max":
                this.response = "0:0";
                break;
            case "get-min":
                this.response = "NUL";
                break;
            case "new-record":
                this.response = "OK";
                break;
        }
    }

    private NodePacket(String input_data) {
        String[] data = input_data.split(" ");
        this.ID = data[0];
        this.command = data[1];
        this.request = data[2];
        this.response = data[3];
        String[] history = data[4].split(",");
        this.visitedNodeHistory = new LinkedList<>();
        this.visitedNodeHistory.addAll(Arrays.asList(history));
        this.lastSender = data[5];
    }

    public static NodePacket makePacket(String data) {
        if (Character.isDigit(data.charAt(0)))
            return new NodePacket(data);
        else
            return new NodePacket(data.split(" "));
    }

    public static boolean isInternalRequest(String message) {
        return Character.isDigit(message.charAt(0));
    }

    public String getID() {
        return ID;
    }

    public String getCommand() {
        return command;
    }

    public String getRequest() {
        return request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public LinkedList<String> getVisitedNodeHistory() {
        return visitedNodeHistory;
    }

    public String getLastSender() {
        return lastSender;
    }

    public void setLastSender(String lastSender) {
        this.lastSender = lastSender;
    }

    private String visitedNodeHistoryToString() {
        StringBuilder node_trace_string = new StringBuilder();
        for (String node : visitedNodeHistory) {
            node_trace_string.append(node).append(",");
        }
        return node_trace_string.toString();
    }

    @Override
    public String toString() {
        return ID + ' ' + command + ' ' + request + ' ' + response + ' ' + visitedNodeHistoryToString() + ' ' + lastSender;
    }
}