package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByMaKH(Integer maKH);
}
