import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TestServer {
    private static final int port = 8089;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on prt " + port);

            //thread to handle input
            new Thread(this::handleCommandInput).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
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
            System.out.print("Enter command: ");
            String command = scanner.nextLine();
            broadcastCommand(command);
        }
    }

    public static void broadcastCommand(String command){
        System.out.println("Broadcasting: " + command);
        clients.forEach(client -> client.sendCommand(command));
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String message;
                while ((message = in.readLine()) != null){
                    System.out.println("Recieved from " + socket.getInetAddress() + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                cleanup();
            }
        }

        public void sendCommand(String command) {
            if (out != null){
                out.println(command);
            }
        }

        private void cleanup(){
            clients.remove(this);
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
