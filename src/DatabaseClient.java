import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class DatabaseClient {

    private Socket socket;
    private DataOutputStream outToServer = null;
    private BufferedReader inFromServer = null;
    private String command;
    private String gateway;
    private int port;

    public DatabaseClient(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-gateway":
                    setUpGateway(args[++i]);
                    break;
                case "-operation":
                    setUpCommand(args[++i]);
                    break;
                default:
                    setUpArgument(args[i]);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        DatabaseClient databaseClient = new DatabaseClient(args);
        databaseClient.connect();
        databaseClient.makeRequest();
        databaseClient.exit();
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private static void logErr(String message) {
        System.err.println(message);
    }

    private void exit() {
        try {
            outToServer.close();
            inFromServer.close();
            socket.close();
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void handleError(Exception e) {
        e.printStackTrace();
        if (e instanceof UnknownHostException) {
            logErr("Unknown host: " + gateway + ".");
        } else if (e instanceof IOException) {
            logErr("No connection with " + gateway + ".");
        } else if (e instanceof InterruptedException) {
            logErr("Problem ocurred while waiting after connection established.");
        } else {
            logErr("Unknown error ocurred.");
        }
        log("Press ENTER to end process...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        System.exit(1);
    }

    private void makeRequest() {
        try {
            log("Sending: " + command);
            outToServer.writeBytes(command + '\n');
            outToServer.flush();
            String response = inFromServer.readLine();
            log(response);
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void connect() {
        try {
            log("Connecting with: " + gateway + " at port " + port);
            Thread.sleep(1000);
            socket = new Socket(gateway, port);
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToServer = new DataOutputStream(socket.getOutputStream());
            log("Connected");
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void setUpArgument(String parameter) {
        command += " " + parameter;
    }

    private void setUpCommand(String parameter) {
        command = parameter;
    }

    private void setUpGateway(String parameter) {
        String[] gatewayArray = parameter.split(":");
        gateway = gatewayArray[0];
        port = Integer.parseInt(gatewayArray[1]);
    }
}