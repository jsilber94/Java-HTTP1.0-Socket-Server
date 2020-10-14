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

    ServerSocket serverSocket;

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server has been started.");
        } catch (IOException e) {
            System.out.println("Could not create server socket on port " + port + ". Quitting.");
            System.exit(-1);
        }
        Socket clientSocket = null;
        while (true) {
            try {
                if (!(port >= 1024 && port <= 65535))
                    throw new Exception("Port is invalid. It must be between 1024 and 65535.");

                clientSocket = serverSocket.accept();

                System.out.println("New client has connected.");
                System.out.println("Assigning new thread for the client");

                new Thread(new ClientHandler(clientSocket)).start();

            } catch (Exception e) {
                System.out.println("Exception encountered on accept.");
                System.exit(-1);
            }
        }
    }

    class ClientHandler implements Runnable {

        Socket clientSocket;
        PrintWriter out;
        BufferedReader in;
        private int statusCode;

        ClientHandler(Socket socket) {
            clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream());
                String response = dealWithRequest();

                statusCode = 200;
                sendResponse(response);


                System.out.println("Server has been closed.");

            } catch (Exception e) {
                System.out.println("Server has been closed.");
                statusCode = 404;
                sendResponse(e.getMessage());
            } finally {
                out.close();
                try {
                    clientSocket.close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        private String dealWithRequest() throws Exception {
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
            //code to read the post payload data
            StringBuilder payload = new StringBuilder();
            while (in.ready()) {
                payload.append((char) in.read());
            }

            switch (method) {
                case "GET":
                    return dealWithGET(path);
                case "POST":
                    return dealWithPOST(path, payload.toString());
                default: {
                    statusCode = 400;
                    throw new Exception("Invalid HTTP 1.0 verb provided.");
                }
            }
        }

        private String dealWithGET(String path) throws Exception {
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

        private String dealWithPOST(String path, String payload) throws Exception {
            if (path.indexOf("../") != -1) {
                statusCode = 400;
                throw new Exception("Invalid path provided");
            }
            String fullPath = System.getProperty("user.dir") + "/";
            path = path.substring(1);
            String[] paths = path.split("/");

            for (int i = 0; i < paths.length; i++) {
                fullPath = fullPath + paths[i] + "/";

                File currentPath = new File(fullPath);
                if (!currentPath.exists() && i < paths.length - 1) {
                    new File(fullPath).mkdirs();
                }
                if (i == paths.length - 1) {
                    if (!currentPath.exists())
                        new File(fullPath).createNewFile();
                    FileWriter fw = new FileWriter(fullPath);
                    fw.write(payload);
                    fw.close();
                }
            }

            return "POST was successful";
        }

        private boolean determineIfFile(String path) throws Exception {
            if (path.indexOf("../") != -1) {
                statusCode = 400;
                throw new Exception("Invalid path provided");
            }

            File file = path.charAt(0) == '/' ? new File(path.substring(1)) : new File(path);
            if (!file.exists()) {
                statusCode = 400;
                throw new Exception("Invalid path or file provided");
            }

            if (file.isDirectory())
                return false;
            return file.isFile();
        }

        private void sendResponse(String response) {
            // Start sending our reply, using the HTTP 1.0 protocol
            out.println("HTTP/1.0 " + statusCode); // Version & status code
            out.println("Content-Type: text/plain"); // The type of data
            if (!response.isEmpty())
                out.println("Content-length: " + response.length());
            out.println();
            out.println(response);
            out.println("Connection: close"); // Will close stream
            out.flush();
        }
    }
}

