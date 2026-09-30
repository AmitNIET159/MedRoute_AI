package com.medroute.dao;

import com.medroute.model.Medicine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MedicineDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final RowMapper<Medicine> MEDICINE_ROW_MAPPER = (rs, rowNum) -> {
        Medicine medicine = new Medicine();
        medicine.setId(rs.getLong("id"));
        medicine.setName(rs.getString("name"));
        medicine.setGenericName(rs.getString("generic_name"));
        medicine.setCategoryId(rs.getLong("category_id"));
        
        try {
            medicine.setCategoryName(rs.getString("category_name"));
        } catch (Exception e) {
            // category_name might not be in the result set
        }

        medicine.setUnit(rs.getString("unit"));
        medicine.setDescription(rs.getString("description"));
        medicine.setRequiresColdChain(rs.getBoolean("requires_cold_chain"));
        medicine.setControlled(rs.getBoolean("is_controlled"));
        
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            medicine.setCreatedAt(createdAt.toLocalDateTime());
        }
        return medicine;
    };

    public void create(Medicine medicine) {
        String sql = "INSERT INTO medicine_catalog (name, generic_name, category_id, unit, description, requires_cold_chain, is_controlled) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, medicine.getName(), medicine.getGenericName(), medicine.getCategoryId(), medicine.getUnit(), medicine.getDescription(), medicine.isRequiresColdChain(), medicine.isControlled());
    }

    public void update(Medicine medicine) {
        String sql = "UPDATE medicine_catalog SET name = ?, generic_name = ?, category_id = ?, unit = ?, description = ?, requires_cold_chain = ?, is_controlled = ? WHERE id = ?";
        jdbcTemplate.update(sql, medicine.getName(), medicine.getGenericName(), medicine.getCategoryId(), medicine.getUnit(), medicine.getDescription(), medicine.isRequiresColdChain(), medicine.isControlled(), medicine.getId());
    }

    public Optional<Medicine> findById(Long id) {
        String sql = "SELECT m.*, c.name as category_name FROM medicine_catalog m LEFT JOIN medicine_categories c ON m.category_id = c.id WHERE m.id = ?";
        List<Medicine> medicines = jdbcTemplate.query(sql, MEDICINE_ROW_MAPPER, id);
        return medicines.isEmpty() ? Optional.empty() : Optional.of(medicines.get(0));
    }

    public List<Medicine> findAll(int offset, int limit, String search, Long categoryId) {
        StringBuilder sql = new StringBuilder("SELECT m.*, c.name as category_name FROM medicine_catalog m LEFT JOIN medicine_categories c ON m.category_id = c.id WHERE 1=1");
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (m.name LIKE ? OR m.generic_name LIKE ?)");
        }
        if (categoryId != null) {
            sql.append(" AND m.category_id = ?");
        }
        
        sql.append(" ORDER BY m.name ASC LIMIT ? OFFSET ?");

        if (search != null && !search.isEmpty() && categoryId != null) {
            String searchPattern = "%" + search + "%";
            return jdbcTemplate.query(sql.toString(), MEDICINE_ROW_MAPPER, searchPattern, searchPattern, categoryId, limit, offset);
        } else if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search + "%";
            return jdbcTemplate.query(sql.toString(), MEDICINE_ROW_MAPPER, searchPattern, searchPattern, limit, offset);
        } else if (categoryId != null) {
            return jdbcTemplate.query(sql.toString(), MEDICINE_ROW_MAPPER, categoryId, limit, offset);
        } else {
            return jdbcTemplate.query(sql.toString(), MEDICINE_ROW_MAPPER, limit, offset);
        }
    }

    public int countAll(String search, Long categoryId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM medicine_catalog m WHERE 1=1");
        
        if (search != null && !search.isEmpty()) {
            sql.append(" AND (m.name LIKE ? OR m.generic_name LIKE ?)");
        }
        if (categoryId != null) {
            sql.append(" AND m.category_id = ?");
        }

        if (search != null && !search.isEmpty() && categoryId != null) {
            String searchPattern = "%" + search + "%";
            return jdbcTemplate.queryForObject(sql.toString(), Integer.class, searchPattern, searchPattern, categoryId);
        } else if (search != null && !search.isEmpty()) {
            String searchPattern = "%" + search + "%";
            return jdbcTemplate.queryForObject(sql.toString(), Integer.class, searchPattern, searchPattern);
        } else if (categoryId != null) {
            return jdbcTemplate.queryForObject(sql.toString(), Integer.class, categoryId);
        } else {
            return jdbcTemplate.queryForObject(sql.toString(), Integer.class);
        }
    }

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM medicine_catalog WHERE name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name);
        return count != null && count > 0;
    }
}
