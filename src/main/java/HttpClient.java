import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * Http Client made with sockets
 *
 * @author Jesse Silber - 26993702
 */

public class HttpClient {

    static int httpPort = 80;

    private static Socket socket = null;
    private static URI uri = null;
    private static String verb = null;
    private static StringBuilder responseString = null;

    private static OutputStream output = null;
    private static PrintWriter writer = null;
    private static BufferedReader reader = null;

    private static String payload = null;
    private static File filePayload = null;

    /**
     * GET endpoint for http client
     *
     * @param url     url for request
     * @param headers request headers
     * @param verbose descriptive flag
     * @return String response
     */
    public static String get(String url, Map<String, String> headers, boolean verbose) {
        verb = "GET";
        return request(url, headers, verbose);
    }

    public static String delete(String url, Map<String, String> headers, boolean verbose) {
        verb = "DELETE";
        return request(url, headers, verbose);
    }

    /**
     * POST endpoint for http client
     *
     * @param url     url for request
     * @param headers request headers
     * @param verbose descriptive flag
     * @return String response
     */
    public static String post(String url, Map<String, String> headers, boolean verbose) {
        verb = "POST";
        return request(url, headers, verbose);
    }

    /**
     * POST endpoint for http client with body
     *
     * @param url     url for request
     * @param headers request headers
     * @param body    payload for post request
     * @param verbose descriptive flag
     * @return String response
     */
    public static String post(String url, Map<String, String> headers, String body, boolean verbose) {
        payload = body;
        return post(url, headers, verbose);
    }

    /**
     * POST endpoint for http client with file payload
     *
     * @param url     url for request
     * @param headers request headers
     * @param file    file payload for post request
     * @param verbose descriptive flag
     * @return String response
     */
    public static String post(String url, Map<String, String> headers, File file, boolean verbose) {
        filePayload = file;
        return post(url, headers, verbose);
    }

    private static String request(String url, Map<String, String> headers, boolean verbose) {
        try {
            if (url == null || url.isEmpty())
                throw new MalformedURLException("Bad url");
            url = url.replaceAll("'", "");
            url = url.replaceAll("\"", "");
            url = url.replaceAll(" ", "%20");
            uri = new URI(url);

            responseString = new StringBuilder();

            // connect to server
            socket = new Socket(uri.getHost(), HttpClient.httpPort);
            HttpClient.sendRequest(headers);

            String response = HttpClient.receiveResponse();

            // close streams
            writer.close();
            output.close();
            reader.close();
            // close socket
            socket.close();

            if ((response.toLowerCase().contains("302 found") || response.toLowerCase().contains("301 moved permanently")) && response.contains("Location"))
                response = handleRedirect(response, headers, verbose);


            // static class so always make sure to reset after each request
            payload = null;
            filePayload = null;
            return verbose ? response : parseResponse(response);

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String handleRedirect(String response, Map<String, String> headers, boolean verbose) {
        String newURL = response.substring(response.indexOf("Location:") + 10);
        newURL = newURL.substring(0, newURL.indexOf("\n"));

        if (verb.equals("POST")) {
            if (payload != null)
                return post(newURL, headers, payload, verbose);
            else if (filePayload != null)
                return post(newURL, headers, filePayload, verbose);
            else return post(newURL, headers, verbose);
        } else return get(newURL, headers, verbose);
    }

    private static void sendRequest(Map<String, String> headers) throws IOException {
        // send data to server: use OutputStream
        output = socket.getOutputStream();

        // wrap OutputSteam in PrinterWriter so we send data in text format + autoFlush
        writer = new PrintWriter(output, true);

        String path = determinePath(uri);
        if (verb.equals("POST"))
            writer.println("POST " + path + " HTTP/1.0");
        else if (verb.equals("GET"))
            writer.println("GET " + path + " HTTP/1.0");
        else
            writer.println("DELETE " + path + " HTTP/1.0");


        // default headers
        writer.println("Host: " + uri.getHost());
        writer.println("User-Agent: Concordia-HTTP/1.0");

        boolean isContentTypePresent = false;
        boolean isContentLengthPresent = false;
        boolean isAcceptTypePresent= false;
        // add given headers
        if (headers != null) {
            for (String key : headers.keySet()) {
                writer.println(key + ": " + headers.get(key));

                if(key.toLowerCase().equals("accept ")){
                    isAcceptTypePresent = true;
                }
                if (verb.equals("POST")) {
                    if (key.trim().equalsIgnoreCase("content-type")) {
                        isContentTypePresent = true;
                    }
                    if (key.trim().equalsIgnoreCase("content-length")) {
                        isContentLengthPresent = true;
                    }
                }
            }
        }

        // assume txt/plain if not otherwise given
        if (verb.equals("POST") && !isContentTypePresent) {
            writer.println("Content-Type: text/plain");
        }
        // determine content length if not otherwise given
        if (verb.equals("POST") && !isContentLengthPresent) {
            if (payload != null)
                writer.println("Content-Length:" + payload.length());
            if (filePayload != null)
                writer.println("Content-Length:" + filePayload.length());
        }
        if(!isAcceptTypePresent)
            writer.println("Accept:" + "*/*");

        writer.println("Connection: close");
        writer.println();

        if (verb.equals("POST"))
            addPayload();
    }

    private static String determinePath(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getQuery() != null) {
            path += "?" + uri.getQuery();
        }
        return path;
    }

    private static void addPayload() throws IOException {
        // add string
        if (verb.equals("POST") && payload != null)
            writer.println(payload);
        // add file
        if (verb.equals("POST") && filePayload != null) {
            byte[] buffer = new byte[(int) filePayload.length()];
            FileInputStream fis = new FileInputStream(filePayload);
            BufferedInputStream in = new BufferedInputStream(fis);
            in.read(buffer, 0, buffer.length);
            System.out.println("Sending files");
            output.write(buffer, 0, buffer.length);

            // close streams
            fis.close();
            in.close();
        }
    }

    private static String receiveResponse() throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            responseString.append(line).append("\n");
        }
        return responseString.toString();
    }

    private static String parseResponse(String response) {
        return response.substring(response.indexOf("\"args\"") - 4);
    }


}