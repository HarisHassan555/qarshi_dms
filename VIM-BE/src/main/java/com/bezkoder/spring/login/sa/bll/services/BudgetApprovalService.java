package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.dal.entities.BudgetApproval;
import java.util.List;

public interface BudgetApprovalService {
    BudgetApproval save(BudgetApproval budgetApproval);
    BudgetApproval getLatest();
    List<BudgetApproval> getAll();
}
