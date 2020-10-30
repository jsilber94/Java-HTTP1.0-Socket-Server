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

    /**
     * https://github.com/google/guava
     *
     * @param fullName
     * @return String
     */
    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public void startServer(int port, String path, boolean verbose) {
        try {
            serverSocket = new ServerSocket(port);
            if (verbose)
                System.out.println("Server has been started.");
        } catch (IOException e) {
            if (verbose)
                System.out.println("Could not create server socket on port " + port + ". Quitting.");
            System.exit(-1);
        }
        Socket clientSocket = null;
        while (true) {
            try {
                if (!(port >= 1024 && port <= 65535))
                    throw new Exception("Port is invalid. It must be between 1024 and 65535.");
//                if(path.indexOf("../")!=-1)
//                    throw new Exception("\'../\' is not allowed in the path. ");

                clientSocket = serverSocket.accept();
                if (verbose) {
                    System.out.println("New client has connected.");
                    System.out.println("Assigning new thread for the client");
                }

                new Thread(new ClientHandler(clientSocket, path, verbose)).start();

            } catch (Exception e) {
                System.out.println("Exception encountered on accept. " + e.getMessage());
                System.exit(-1);
            }
        }
    }

    class ClientHandler implements Runnable {

        private final Socket clientSocket;
        private final String defaultDirectory;
        boolean verbose = false;
        private PrintWriter out;
        private BufferedReader in;
        private int statusCode;
        private String statusVerb;
        private String contentType = "text/plain";


        ClientHandler(Socket socket, String path, boolean verbose) {
            clientSocket = socket;
            this.verbose = verbose;
            if (path == null)
                defaultDirectory = System.getProperty("user.dir");
            else
                defaultDirectory = path.charAt(path.length() - 1) == '/' ? path.substring(0, path.length() - 1) : path;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream());
                String response = dealWithRequest();

                statusCode = 200;
                statusVerb = "OK";
                sendResponse(response);

                if (verbose)
                    System.out.println("Server has been closed.");

            } catch (Exception e) {
                if (verbose)
                    System.out.println("Server has been closed.");
                statusCode = statusCode == 200 || statusCode == 0 ? 404 : statusCode;
                statusVerb = statusCode == 200 || statusCode == 0 ? "OK": "Not Found";
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
                    statusVerb = "Bad Request";
                    throw new Exception("Invalid HTTP 1.0 verb provided.");
                }
            }
        }

        private String dealWithGET(String path) throws Exception {
            if (determineIfFile(path)) {
                // the file exists
                File file = new File(defaultDirectory + path);
                if (file.renameTo(file)) {
                    String fileExtension = getFileExtension(defaultDirectory + path);
                    if (fileExtension.equals("json"))
                        contentType = "application/json";
                    if (fileExtension.equals("html"))
                        contentType = "text/html";
                    byte[] encoded = Files.readAllBytes(Paths.get(defaultDirectory + path));
                    return new String(encoded, StandardCharsets.US_ASCII);
                } else {
                    statusCode = 406;
                    statusVerb = "Not Acceptable";
                    throw new Exception("File is unable to be accessed Try again later.");
                }

            } else {
                //its a dir
                StringBuilder fileNames = new StringBuilder();
                String fullPath = defaultDirectory + path;
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
                statusCode = 403;
                statusVerb = "Forbidden";
                throw new Exception("Invalid path provided");
            }
            String fullPath = defaultDirectory + "/";
            path = path.substring(1);
            String[] paths = path.split("/");

            for (int i = 0; i < paths.length; i++) {
                fullPath = fullPath + paths[i] + "/";

                File currentPath = new File(fullPath);
                if (!currentPath.exists() && i < paths.length - 1) {
                    new File(fullPath).mkdirs();
                }
                if (i == paths.length - 1) {
                    if (!currentPath.exists()) {
                        new File(fullPath).createNewFile();
                        currentPath = new File(fullPath);
                    }

//                    File file = new File(fullPath);
                    if (currentPath.renameTo(currentPath)) {
                        FileWriter fw = new FileWriter(fullPath);
                        fw.write(payload);
                        fw.close();
                    } else {
                        statusCode = 406;
                        statusVerb = "Not Acceptable";
                        throw new Exception("File is unable to be accessed Try again later.");
                    }

                }
            }
            return "POST was successful";
        }

        private boolean determineIfFile(String path) throws Exception {
            if (path.indexOf("../") != -1) {
                statusCode = 403;
                statusVerb = "Forbidden";
                throw new Exception("Invalid path provided");
            }

            File file = new File(defaultDirectory + path);
            if (!file.exists()) {
                statusCode = 403;
                statusVerb = "Forbidden";
                throw new Exception("Invalid path or file provided");
            }

            if (file.isDirectory())
                return false;
            return file.isFile();
        }

        private void sendResponse(String response) {
            // Start sending our reply, using the HTTP 1.0 protocol
            out.println("HTTP/1.0 " + statusCode + " " + statusVerb + "\r\n"); // Version & status code
            out.println("Content-Type: " + contentType); // The type of data
            if (!response.isEmpty()) {
                out.println("Content-Disposition: " + "inline");
                out.println("Content-Length: " + response.length());
            }
            out.println("\r\n");
            out.println(response);
            out.println("Connection: close");
            out.flush();
        }
    }
}

