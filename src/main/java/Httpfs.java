public class Httpfs {

    private final String[] args;

    public Httpfs(String[] args) {
        this.args = args;
    }

    public static void main(String[] args) {
        try {
            int port = 8080;
            boolean verbose = false;
            String path = null;

            if (args.length != 0)
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("-p")) {
                        port = Integer.parseInt(args[i + 1]);
                    }
                    if (args[i].equalsIgnoreCase("-v")) {
                        verbose = true;
                    }
                    if (args[i].equalsIgnoreCase("-d")) {
                        path = args[i + 1];
                    }
                }

            new HttpServer().startServer(port,path,verbose);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
