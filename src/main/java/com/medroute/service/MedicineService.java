package com.medroute.service;

import com.medroute.dao.MedicineDAO;
import com.medroute.model.Medicine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicineService {

    @Autowired
    private MedicineDAO medicineDAO;

    public void createMedicine(Medicine medicine) {
        if (medicineDAO.existsByName(medicine.getName())) {
            throw new IllegalArgumentException("Medicine with name " + medicine.getName() + " already exists.");
        }
        medicineDAO.create(medicine);
    }

    public void updateMedicine(Medicine medicine) {
        Optional<Medicine> existing = medicineDAO.findById(medicine.getId());
        if (!existing.isPresent()) {
            throw new IllegalArgumentException("Medicine not found.");
        }
        if (!existing.get().getName().equals(medicine.getName()) && medicineDAO.existsByName(medicine.getName())) {
            throw new IllegalArgumentException("Medicine with name " + medicine.getName() + " already exists.");
        }
        medicineDAO.update(medicine);
    }

    public Optional<Medicine> getMedicineById(Long id) {
        return medicineDAO.findById(id);
    }

    public List<Medicine> getAllMedicines(int offset, int limit, String search, Long categoryId) {
        return medicineDAO.findAll(offset, limit, search, categoryId);
    }

    public int countAllMedicines(String search, Long categoryId) {
        return medicineDAO.countAll(search, categoryId);
    }
}
