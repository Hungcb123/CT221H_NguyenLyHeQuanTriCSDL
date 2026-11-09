package com.ctu.freshgrocery.repository;

import com.ctu.freshgrocery.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
    Optional<Recipe> findByTenMonAn(String tenMonAn);
    List<Recipe> findByTenMonAnContainingIgnoreCase(String keyword);
}
