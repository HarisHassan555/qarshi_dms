package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.sa.bll.services.BudgetApprovalService;
import com.bezkoder.spring.login.sa.dal.entities.BudgetApproval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/budget-approval")
public class BudgetApprovalController {

    @Autowired
    private BudgetApprovalService budgetApprovalService;

    @PostMapping("/save")
    public ResponseEntity<BudgetApproval> save(@RequestBody BudgetApproval budgetApproval) {
        budgetApproval.setCreatedDate(new Date());
        return ResponseEntity.ok(budgetApprovalService.save(budgetApproval));
    }

    @GetMapping("/latest")
    public ResponseEntity<BudgetApproval> getLatest() {
        return ResponseEntity.ok(budgetApprovalService.getLatest());
    }

    @GetMapping("/all")
    public ResponseEntity<List<BudgetApproval>> getAll() {
        return ResponseEntity.ok(budgetApprovalService.getAll());
    }
}
