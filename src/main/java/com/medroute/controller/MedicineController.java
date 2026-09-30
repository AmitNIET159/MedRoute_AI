package com.medroute.controller;

import com.medroute.model.Medicine;
import com.medroute.service.MedicineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/medicines")
public class MedicineController {

    @Autowired
    private MedicineService medicineService;

    @GetMapping
    public String listMedicines(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false) Long categoryId,
                                Model model) {
        int limit = 20;
        int offset = (page - 1) * limit;
        List<Medicine> medicines = medicineService.getAllMedicines(offset, limit, search, categoryId);
        int total = medicineService.countAllMedicines(search, categoryId);
        
        model.addAttribute("medicines", medicines);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int) Math.ceil((double) total / limit));
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("activePage", "medicines");
        model.addAttribute("contentPage", "/WEB-INF/views/medicine/list.jsp");
        
        return "layouts/base";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("medicine", new Medicine());
        model.addAttribute("activePage", "medicines");
        model.addAttribute("contentPage", "/WEB-INF/views/medicine/form.jsp");
        return "layouts/base";
    }

    @PostMapping("/new")
    public String createMedicine(@ModelAttribute Medicine medicine) {
        medicineService.createMedicine(medicine);
        return "redirect:/medicines";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Medicine> medicine = medicineService.getMedicineById(id);
        if (medicine.isPresent()) {
            model.addAttribute("medicine", medicine.get());
            model.addAttribute("activePage", "medicines");
            model.addAttribute("contentPage", "/WEB-INF/views/medicine/form.jsp");
            return "layouts/base";
        }
        return "redirect:/medicines";
    }

    @PostMapping("/edit/{id}")
    public String updateMedicine(@PathVariable Long id, @ModelAttribute Medicine medicine) {
        medicine.setId(id);
        medicineService.updateMedicine(medicine);
        return "redirect:/medicines";
    }

    @GetMapping("/{id}")
    public String viewMedicine(@PathVariable Long id, Model model) {
        Optional<Medicine> medicine = medicineService.getMedicineById(id);
        if (medicine.isPresent()) {
            model.addAttribute("medicine", medicine.get());
            model.addAttribute("activePage", "medicines");
            model.addAttribute("contentPage", "/WEB-INF/views/medicine/detail.jsp");
            return "layouts/base";
        }
        return "redirect:/medicines";
    }
}
