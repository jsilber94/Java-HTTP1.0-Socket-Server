import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;


public class HttpServer {

    private static ServerSocket serverSocket = null;
    private static BufferedReader in = null;
    private static PrintWriter out = null;
    private static BufferedOutputStream dataOut = null;

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
                    dealWithRequest(socket);
                    dealWithResponse(socket);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            serverSocket.close();
            System.out.println("Server has been closed.");
        }
    }

    private static void dealWithRequest(Socket socket) throws IOException {
        // we read characters from the client via input stream on the socket
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String input = in.readLine();
        StringTokenizer parse = new StringTokenizer(input);
        String method = parse.nextToken().toUpperCase();


        switch (method) {
            case "GET":
                dealWithGET();
                break;
            case "POST":
                dealWithPOST();
                break;
            default:
                dealWithResponse(socket);

        }


    }

    private static void dealWithResponse(Socket socket) throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        // Start sending our reply, using the HTTP 1.0 protocol
        out.print("HTTP/1.0 200 \r\n"); // Version & status code
        out.print("Content-Type: text/plain\r\n"); // The type of data
        out.print("Connection: close\r\n"); // Will close stream
        out.print("\r\n"); // End of headers
    }


    private static void dealWithGET() {

    }

    private static void dealWithPOST() {
    }


    private static boolean validatePort(int port) {
        return port >= 1024 && port <= 65535;
    }
}
