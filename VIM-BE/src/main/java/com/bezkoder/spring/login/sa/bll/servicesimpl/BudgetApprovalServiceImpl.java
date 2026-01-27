package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.services.BudgetApprovalService;
import com.bezkoder.spring.login.sa.dal.entities.BudgetApproval;
import com.bezkoder.spring.login.sa.dal.entities.BudgetApprovalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class BudgetApprovalServiceImpl implements BudgetApprovalService {

    @Autowired
    private BudgetApprovalRepository budgetApprovalRepository;

    @Override
    public BudgetApproval save(BudgetApproval budgetApproval) {
        return budgetApprovalRepository.save(budgetApproval);
    }

    @Override
    public BudgetApproval getLatest() {
        List<BudgetApproval> list = budgetApprovalRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public List<BudgetApproval> getAll() {
        return budgetApprovalRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));
    }
}
