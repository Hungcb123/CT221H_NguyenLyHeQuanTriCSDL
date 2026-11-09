package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByMaKHOrderByNgayTaoDesc(Integer maKH);
    @org.springframework.data.jpa.repository.Query("""
        SELECT o FROM Order o WHERE o.maKH=:customerId
          AND o.nguonDon = 'SALE'
        ORDER BY o.ngayTao DESC, o.maDH DESC
        """)
    List<Order> findCustomerOrders(@org.springframework.data.repository.query.Param("customerId") Integer customerId,
                                 org.springframework.data.domain.Pageable page);
}
