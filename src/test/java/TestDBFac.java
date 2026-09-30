import com.zaxxer.hikari.HikariDataSource; import org.springframework.jdbc.core.JdbcTemplate; import java.util.List; import java.util.Map;
public class TestDBFac {
    public static void main(String[] args) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/medroute_ai?useSSL=false&serverTimezone=UTC");
        ds.setUsername("root"); ds.setPassword("root");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, name FROM facilities");
        for (Map<String, Object> row : rows) {
            System.out.println(row);
        }
        ds.close();
    }
}