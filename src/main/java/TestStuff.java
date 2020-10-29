import java.util.Map;
import static java.util.Map.entry;

public class TestStuff {

    public static void main(String[] args){
        HttpClient.httpPort = 8080;
        Map<String, String> headers = Map.ofEntries(entry("Accept ", "text/html"));
        String res2 = HttpClient.get("http://localhost:8080/hmm.json/", null, true);
//        System.out.println(res2);

//            System.out.println(HttpClient.get("http://localhost:8080/.git", headers, true));
//            System.out.println(HttpClient.post("http://localhost:8080/..hmm1.txt", headers,"this is what too write", true));

//        String res = HttpClient.post("http://localhost:8080/hmm.txt", headers,"this is what to write hfghfhhmm", true);
        System.out.println(res2);
//         String res1 = HttpClient.post("http://localhost:8080/hmm.txt", headers,"ESFES efEF fF", true);
//        System.out.println(res1);

    }
}

//