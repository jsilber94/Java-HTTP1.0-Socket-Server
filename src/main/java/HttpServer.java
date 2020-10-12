import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Jesse Silber
 */
public class HttpServer {

    private static ServerSocket serverSocket = null;
    private static BufferedReader in = null;
    private static PrintWriter out = null;
    private static Socket socket = null;

    public static void startServer(int port) throws Exception {
        while (true) {
            try {
                if (!validatePort(port))
                    throw new Exception("Port is invalid. It must be between 1024 and 65535.");
                serverSocket = new ServerSocket(port);
                System.out.println("Server has been started.");
                socket = serverSocket.accept();
                String response = dealWithRequest(socket);
                sendResponse(socket, response, 200);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(socket, e.getMessage(), 404);
            } finally {
                socket.close();
                serverSocket.close();
                in.close();
                out.close();

                System.out.println("Server has been closed.");
            }
        }
    }

    private static String dealWithRequest(Socket socket) throws Exception {
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
                return "Invalid HTTP 1.0 verb provided.";
        }
    }

    private static void sendResponse(Socket socket, String response, int statusCode) throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        // Start sending our reply, using the HTTP 1.0 protocol
        out.println("HTTP/1.0 " + statusCode); // Version & status code
        out.println("Content-Type: text/plain"); // The type of data
        out.println("Content-length: " + response.length());
        out.println();
        out.println(response);
        out.println("Connection: close"); // Will close stream
        out.flush();
        out.close();
    }

    private static String dealWithGET(String acceptType, String path) throws Exception {
        if (determineIfFile(path)) {
            byte[] encoded = Files.readAllBytes(Paths.get(path.substring(1)));
            return new String(encoded, StandardCharsets.US_ASCII);
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

//        POST …/<FILE> should create or overwrite the file specified by the method in the data directory with the content of the body of the request.
//        You can implement more options for the POST such as overwrite=true|false, but again this is optional.
//        This includes methods such as “POST /docs/foo” assuming docs is a directory and foo is a file.
//        If directory docs doesn’t exist, then the server should create it. Also includes base case of “POST /foo”.
        return null;
    }

    private static boolean determineIfFile(String path) throws Exception {
        if (path.substring(0,3).equals("../"))
            throw new Exception("Invalid path provided");

        File file = path.charAt(0) == '/' ? new File(path.substring(1)) : new File(path);
        if (!file.exists())
            throw new Exception("Invalid path provided");
        if (file.isDirectory())
            return false;
        return file.isFile();
    }

    private static boolean validatePort(int port) {
        return port >= 1024 && port <= 65535;
    }
}