import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medroute.model.AIInsight;
import com.medroute.model.InsightType;
import com.medroute.model.Severity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TestJackson {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        AIInsight insight = new AIInsight();
        insight.setId(1L);
        insight.setFacilityId(999L);
        insight.setMedicineId(999L);
        insight.setInsightType(InsightType.SHORTAGE_RISK);
        insight.setTitle("Test Title");
        insight.setContent("Test Content");
        insight.setSeverity(Severity.WARNING);
        insight.setRiskScore(new BigDecimal("45.50"));
        insight.setMetadata("{}");
        insight.setRequestHash("abc123");
        insight.setRead(false);
        insight.setCreatedAt(LocalDateTime.now());
        insight.setExpiresAt(LocalDateTime.now().plusHours(6));

        try {
            String json = mapper.writeValueAsString(insight);
            System.out.println("SUCCESS: " + json);
        } catch (Exception e) {
            System.out.println("SERIALIZATION FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
