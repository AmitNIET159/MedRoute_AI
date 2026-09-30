import com.zaxxer.hikari.HikariDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.core.JdbcTemplate;
import com.medroute.dao.*;
import com.medroute.service.*;
import com.medroute.model.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.lang.reflect.Field;

public class TestFullFlow {
    
    static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }
    
    public static void main(String[] args) {
        System.out.println("=== Starting full AI analyze flow simulation ===");
        
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/medroute_ai?useSSL=false&serverTimezone=UTC");
        ds.setUsername("root");
        ds.setPassword("root");
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        try {
            // Build DAOs using reflection for @Autowired fields
            FacilityDAO facilityDAO = new FacilityDAO();
            setField(facilityDAO, "jdbcTemplate", jdbc);
            
            MedicineDAO medicineDAO = new MedicineDAO();
            setField(medicineDAO, "jdbcTemplate", jdbc);
            
            DemandDAO demandDAO = new DemandDAO(jdbc);
            AIDAO aiDAO = new AIDAO(jdbc, mapper);
            
            // Build Services
            DemandService demandService = new DemandService(demandDAO, facilityDAO, medicineDAO);
            HuggingFaceClient hfClient = new HuggingFaceClient(dotenv, mapper);
            AIService aiService = new AIService(aiDAO, hfClient, mapper, dotenv);
            
            // Step 1: Resolve facilityId (simulate ADMIN)
            Long facilityId;
            java.util.List<Facility> facilities = facilityDAO.findActiveBounded(1);
            if (facilities != null && !facilities.isEmpty()) {
                facilityId = facilities.get(0).getId();
            } else {
                facilityId = 1L;
            }
            System.out.println("Step 1 - FacilityId resolved: " + facilityId);
            
            Long userId = 1L;
            Long medicineId = 999L;
            
            // Step 2: DemandService.analyzeMedicine
            System.out.println("Step 2 - Calling demandService.analyzeMedicine(" + facilityId + ", " + medicineId + ")");
            DemandAnalysis analysis = demandService.analyzeMedicine(facilityId, medicineId);
            System.out.println("Step 2 - SUCCESS. Risk: " + analysis.getRiskScore().getRiskLevel() + ", Score: " + analysis.getRiskScore().getFinalScore());
            
            // Step 3: AIService.getInsight
            System.out.println("Step 3 - Calling aiService.getInsight");
            AIInsight insight = aiService.getInsight(facilityId, medicineId, analysis, userId);
            System.out.println("Step 3 - SUCCESS. Title: " + insight.getTitle());
            
            // Step 4: Jackson serialization
            System.out.println("Step 4 - Serializing to JSON");
            String json = mapper.writeValueAsString(insight);
            System.out.println("Step 4 - SUCCESS. JSON length: " + json.length());
            System.out.println("Step 4 - First 200 chars: " + json.substring(0, Math.min(200, json.length())));
            
            System.out.println("\n=== ALL STEPS PASSED ===");
            
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.out);
        }
        
        ds.close();
    }
}
