public class Httpfs {

    private final String[] args;

    public Httpfs(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        try {
            int port = 8080;
            HttpServer.startServer(port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
