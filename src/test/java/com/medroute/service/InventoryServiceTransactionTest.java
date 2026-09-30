package com.medroute.service;

import com.medroute.config.AppConfig;
import com.medroute.config.TestDatabaseConfig;
import com.medroute.config.DatabaseConfig;
import com.medroute.dao.InventoryBatchDAO;
import com.medroute.dao.InventoryTransactionDAO;
import com.medroute.dao.StockConsumptionDAO;
import com.medroute.model.InventoryBatch;
import com.medroute.model.InventoryTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class, TestDatabaseConfig.class})
@org.springframework.test.context.ActiveProfiles("test")
@WebAppConfiguration
public class InventoryServiceTransactionTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryBatchDAO inventoryBatchDAO;

    @Autowired
    private InventoryTransactionDAO inventoryTransactionDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        // Clean up test data if necessary, or just rely on isolated data
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("DELETE FROM stock_consumption");
        jdbcTemplate.execute("DELETE FROM inventory_transactions");
        jdbcTemplate.execute("DELETE FROM inventory_batches");
        jdbcTemplate.execute("DELETE FROM facilities");
        jdbcTemplate.execute("DELETE FROM medicine_catalog");
        jdbcTemplate.execute("DELETE FROM medicine_categories");
        
        // Insert dummy facility and medicine to satisfy foreign keys
        jdbcTemplate.execute("INSERT INTO facilities (id, name, facility_type, address, city, state, pincode, phone, email) VALUES (999, 'Test Facility', 'HOSPITAL', '123 Test St', 'Test City', 'TS', '123456', '1234567890', 'test@test.com')");
        jdbcTemplate.execute("INSERT INTO medicine_categories (id, name) VALUES (999, 'Test Category')");
        jdbcTemplate.execute("INSERT INTO medicine_catalog (id, name, category_id, unit) VALUES (999, 'Test Medicine', 999, 'Box')");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    public void testStockInRollbackOnException() {
        // Prepare a valid batch
        InventoryBatch batch = new InventoryBatch();
        batch.setFacilityId(999L);
        batch.setMedicineId(999L);
        batch.setBatchNumber("TEST-BATCH-001");
        batch.setQuantity(100);
        batch.setExpiryDate(LocalDate.now().plusDays(100));

        // We will pass an invalid performedBy user ID (999999L) to intentionally cause a 
        // DataIntegrityViolationException during inventory_transactions insertion
        
        Exception ex = assertThrows(Exception.class, () -> {
            inventoryService.stockIn(batch, 999999L, null); // invalid user causes foreign key constraint violation
        });

        // Verify the exception occurred
        assertNotNull(ex);

        // Verify that the batch was NOT inserted (rollback successful)
        List<InventoryBatch> batches = inventoryBatchDAO.findByFacilityId(999L, 0, 10, null, null, null, null);
        assertTrue(batches.isEmpty(), "Batch should have been rolled back");

        // Verify that the transaction was NOT inserted
        int txCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inventory_transactions", Integer.class);
        assertEquals(0, txCount, "Transaction should have been rolled back");
    }
}
