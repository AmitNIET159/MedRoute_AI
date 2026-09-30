import com.fasterxml.jackson.databind.ObjectMapper; import java.util.Map;
public class TestJson {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Long> map = mapper.readValue("{\"medicineId\": \"999\"}", Map.class);
        System.out.println(map.get("medicineId").getClass());
    }
}