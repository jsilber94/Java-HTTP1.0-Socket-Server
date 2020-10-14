public class Httpfs {

    private final String[] args;

    public Httpfs(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        try {
            new HttpServer().startServer(8080);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
