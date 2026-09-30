import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

public class TestEndToEnd {
    public static void main(String[] args) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/medroute_ai?useSSL=false&serverTimezone=UTC");
        ds.setUsername("root");
        ds.setPassword("root");
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        System.out.println("=== All tables ===");
        List<Map<String, Object>> tables = jdbc.queryForList("SHOW TABLES");
        for (Map<String, Object> r : tables) System.out.println(r);

        ds.close();
    }
}
