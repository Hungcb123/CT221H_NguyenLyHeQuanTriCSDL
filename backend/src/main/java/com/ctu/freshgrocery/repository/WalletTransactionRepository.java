package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByMaKHOrderByNgayGiaoDichDesc(Integer maKH);
}
