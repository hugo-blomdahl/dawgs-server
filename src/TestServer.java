import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TestServer {
    private static final int port = 8089;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Local Host: " + InetAddress.getLocalHost());
            System.out.println("Server listening on prt " + port);
            

            //thread to handle input
            new Thread(this::handleCommandInput).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCommandInput() {
        //CHANGEME not good for our purposes
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter command (Format: 'ALL <msg>' or 'MAC <msg>'): ");
            String input = scanner.nextLine();

            String[] parts = input.split(" ", 2);
            if(parts.length < 2) continue;

            String target = parts[0];
            String message = parts[1];

            if(target.equalsIgnoreCase("ALL")){
                broadcastCommand(message);
            } else {
                sendCommand(target, message);
            }
            
        }
    }

    public static void broadcastCommand(String command){
        System.out.println("Broadcasting: " + command);
        clients.values().forEach(client -> client.sendMessage(command));
    }

    public static void sendCommand(String macAddress, String command){
        ClientHandler client = clients.get(macAddress);
        if (client != null){
            System.out.println("Sending to " + macAddress + ": " + command);
            client.sendMessage(command);
        } else {
            System.out.println("Client " + macAddress + " not registered.");
        }
    }

    public static void registerClient(String macAddress, ClientHandler handler){
        clients.put(macAddress, handler);
        System.out.println("Registered client: " + macAddress);
    }

    public static void removeClient(String macAddress){
        if (macAddress != null){
            clients.remove(macAddress);
            System.out.println("Removed client: " + macAddress);
        }
    }


    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String macAddress;

        public ClientHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String handshake = in.readLine();
                if (handshake==null || handshake.trim().isEmpty()){
                    System.out.println("Handshake not successful");
                    return;
                }

                this.macAddress = handshake.trim();
                TestServer.registerClient(handshake, this);

                String message;
                while ((message = in.readLine()) != null){
                    System.out.println("Recieved from " + macAddress + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Client error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        public void sendMessage(String message) {
            if (out != null){
                out.println(message);
            }
        }

        private void cleanup(){
            TestServer.removeClient(this.macAddress);
            try{
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
