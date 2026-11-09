package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    @Query("""
            SELECT t FROM WalletTransaction t
            WHERE t.maKH = :customerId AND (t.maDH IS NULL OR EXISTS (
                SELECT o.maDH FROM Order o WHERE o.maDH = t.maDH AND o.nguonDon = 'SALE'
            ))
            ORDER BY t.ngayGiaoDich DESC, t.maGiaoDich DESC
            """)
    List<WalletTransaction> findCustomerHistory(@Param("customerId") Integer customerId);
}
