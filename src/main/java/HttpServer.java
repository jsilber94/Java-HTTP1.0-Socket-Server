import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class HttpServer {

    private static final BufferedOutputStream dataOut = null;
    private static ServerSocket serverSocket = null;
    private static BufferedReader in = null;
    private static PrintWriter out = null;


    public static void startServer(int port, String directory) throws Exception {
        if (!validatePort(port))
            throw new Exception("Port is invalid. It must be between 1024 and 65535.");

        listenForConnection(port);
    }

    private static void listenForConnection(int port) throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server has been started.");

            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    String response = dealWithRequest(socket);
                    sendResponse(socket, response);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            serverSocket.close();
            System.out.println("Server has been closed.");
        }
    }

    private static String dealWithRequest(Socket socket) throws IOException {
        // we read characters from the client via input stream on the socket
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String[] headers = in.readLine().split(" ");
        String method = headers[0].toUpperCase();
        String path = headers[1];
        String acceptType = "*/*";

        String input;
        while ((input = in.readLine()) != null) {
            String[] keyValue = input.split(":");
            if (keyValue[0].trim().equals("Accept"))
                acceptType = keyValue[1].trim();
            if (input.equals(""))
                break;
        }

        switch (method) {
            case "GET":
                return dealWithGET(acceptType, path);
            case "POST":
                return dealWithPOST();
            default:
                return "Error: Invalid HTTP 1.0 verb provided.";

        }
    }

    private static void sendResponse(Socket socket, String response) throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        // Start sending our reply, using the HTTP 1.0 protocol
        out.println("HTTP/1.0 200"); // Version & status code
        out.println("Content-Type: text/plain"); // The type of data
        out.println("Content-length: " + response.length());
        out.println();
        out.println(response);
        out.println("Connection: close"); // Will close stream
        out.flush();
        out.close();
    }

    private static String dealWithGET(String acceptType, String path) {
        if (determineIfFile(path)) {
            return null;

        } else {
            StringBuilder fileNames = new StringBuilder();
            String userDirectory = System.getProperty("user.dir");
            String fullPath = userDirectory + path;
            File folder = new File(fullPath);
            File[] listOfFiles = folder.listFiles();

            for (File file : listOfFiles) {
                fileNames.append(file.getName()).append("\n");
            }
            return fileNames.toString();

        }
    }

    private static String dealWithPOST() {
        return null;
    }

    private static boolean determineIfFile(String path) {
        return false;
    }

    private static boolean validatePort(int port) {
        return port >= 1024 && port <= 65535;
    }
}