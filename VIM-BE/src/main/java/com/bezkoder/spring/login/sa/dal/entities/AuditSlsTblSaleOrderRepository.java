package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditSlsTblSaleOrderRepository extends JpaRepository<AuditSlsTblSaleOrder, Integer> {
}