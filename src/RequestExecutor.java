public class RequestExecutor {

    private final NodePacket request;

    private RequestExecutor(NodePacket request) {
        this.request = request;
    }

    public static void execute(NodePacket request) {
        RequestExecutor executor = new RequestExecutor(request);
        executor.execute();
    }

    private void execute() {
        switch (request.getCommand()) {
            case "set-value":
                setValue();
                break;
            case "get-value":
                getValue();
                break;
            case "find-key":
                findKey();
                break;
            case "get-max":
                getMax();
                break;
            case "get-min":
                getMin();
                break;
            case "new-record":
                newRecord();
                break;
        }
    }

    private void setValue() {
        int key = Integer.parseInt(request.getRequest().split(":")[0]);
        int value = Integer.parseInt(request.getRequest().split(":")[1]);
        if (key == DatabaseNode.getInstance().getRecord()[0]) {
            DatabaseNode.getInstance().getRecord()[1] = value;
            request.setResponse("OK");
        }
    }

    private void getValue() {
        int key = Integer.parseInt(request.getRequest());
        if (key == DatabaseNode.getInstance().getRecord()[0]) {
            request.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
        }
    }

    private void findKey() {
        int key = Integer.parseInt(request.getRequest());
        if (key == DatabaseNode.getInstance().getRecord()[0]) {
            request.setResponse(DatabaseNode.getInstance().getDeclaredAddress());
        }
    }

    private void getMax() {
        int value = Integer.parseInt(request.getResponse().split(":")[1]);
        if (value < DatabaseNode.getInstance().getRecord()[1]) {
            request.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
        }
    }

    private void getMin() {
        if (request.getResponse().equals("NUL")) {
            request.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
        } else {
            int value = Integer.parseInt(request.getResponse().split(":")[1]);
            if (value > DatabaseNode.getInstance().getRecord()[1]) {
                request.setResponse(String.valueOf(DatabaseNode.getInstance().getRecord()[0]) + ':' + DatabaseNode.getInstance().getRecord()[1]);
            }
        }
    }

    private void newRecord() {
        int key = Integer.parseInt(request.getRequest().split(":")[0]);
        int value = Integer.parseInt(request.getRequest().split(":")[1]);
        int[] new_data = {key, value};
        DatabaseNode.getInstance().setRecord(new_data);
    }
}