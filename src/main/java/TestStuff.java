import java.util.Map;
import static java.util.Map.entry;

public class TestStuff {

    public static void main(String[] args){
        HttpClient.httpPort = 8080;
        Map<String, String> headers = Map.ofEntries(entry("Accept ", "text/html"));
        String res2 = HttpClient.get("http://localhost:8080/.git", headers, true);

        for(int i =0; i <10000; i++){
            System.out.println(HttpClient.get("http://localhost:8080/.git", headers, true));
        }
//        String res = HttpClient.post("http://localhost:8080/hmm.txt", headers,"this is what too write", true);
//        System.out.println(res);
//         String res1 = HttpClient.post("http://localhost:8080/hmm.txt", headers,"ESFES efEF fF", true);
//        System.out.println(res1);

    }
}

//