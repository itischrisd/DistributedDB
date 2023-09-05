public class PacketProcessor {

    private final NodePacket packet;

    private PacketProcessor(NodePacket packet) {
        this.packet = packet;
    }

    public static void process(NodePacket packet) {
        PacketProcessor packetProcessor = new PacketProcessor(packet);
        packetProcessor.process();
    }

    private void process() {
        if (isFirstTime()) {
            addMyselfToPacketTrace();
            addPacketToLocalHistory();
            RequestExecutor.execute(packet);
        }

        if (isNewRecordRequest()) {
            returnToClient();
            return;
        }

        if (isNeighbourhoodUnfinished()) {
            passToNextNode();
            return;
        }

        if (isFirstNode()) {
            returnToClient();
        } else {
            returnToNode();
        }
    }

    private boolean isFirstTime() {
        return !DatabaseNode.getInstance().getPacketIdToOriginHistory().containsKey(packet.getID());
    }

    private boolean isNewRecordRequest() {
        return packet.getCommand().equals("New-record");
    }

    private boolean isNeighbourhoodUnfinished() {
        return !packet.getVisitedNodeHistory().containsAll(DatabaseNode.getInstance().getAttachedNodes());
    }

    private boolean isFirstNode() {
        return packet.getVisitedNodeHistory().getFirst().equals(DatabaseNode.getInstance().getDeclaredAddress());
    }

    private void addMyselfToPacketTrace() {
        packet.getVisitedNodeHistory().add(DatabaseNode.getInstance().getDeclaredAddress());
    }

    private void addPacketToLocalHistory() {
        DatabaseNode.getInstance().addToPacketHistory(packet.getID(), packet.getLastSender());
    }

    private void passToNextNode() {
        packet.setLastSender(DatabaseNode.getInstance().getDeclaredAddress());
        DatabaseNode.getInstance().sendToNextNode(packet);
    }

    private void returnToNode() {
        DatabaseNode.getInstance().returnToNode(packet);
    }

    private void returnToClient() {
        DatabaseNode.getInstance().returnToClient(packet);
    }
}