package com.medroute.service;

import com.medroute.dao.FacilityDAO;
import com.medroute.model.Facility;
import com.medroute.model.Role;
import com.medroute.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FacilityService {

    @Autowired
    private FacilityDAO facilityDAO;

    public void createFacility(Facility facility, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Only ADMIN can create facilities.");
        }
        facilityDAO.create(facility);
    }

    public void updateFacility(Facility facility, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new SecurityException("Only ADMIN can update facilities.");
        }
        facilityDAO.update(facility);
    }

    public List<Facility> getAllFacilities(int offset, int limit) {
        return facilityDAO.findAll(offset, limit);
    }

    public int countAllFacilities() {
        return facilityDAO.countAll();
    }

    public Optional<Facility> getFacilityById(Long id) {
        return facilityDAO.findById(id);
    }
}
