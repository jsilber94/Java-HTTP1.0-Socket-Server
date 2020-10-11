public class TestStuff {

    public static void main(String[] args){
        HttpClient.httpPort = 8080;
        String res = HttpClient.get("http://localhost:8080", null, true);
        System.out.println(res);
    }
}
