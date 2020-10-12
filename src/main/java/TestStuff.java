import java.util.Map;
import static java.util.Map.entry;

public class TestStuff {

    public static void main(String[] args){
        HttpClient.httpPort = 8080;
        Map<String, String> headers = Map.ofEntries(entry("Accept ", "text/html"));
        String res = HttpClient.get("http://localhost:8080/.idea/compiler.xml", headers, true);
//        String res = HttpClient.get("http://localhost:8080/.git/test", headers, true);
        System.out.println(res);
    }
}
