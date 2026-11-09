package com.ctu.freshgrocery.service;

import com.ctu.freshgrocery.dto.RecipeDetailDto;

import java.util.List;
import java.util.Map;

public interface RecipeService {
    RecipeDetailDto getRecipeDetail(Integer recipeId, Integer servings);
    RecipeDetailDto searchRecipeWithIngredients(String keyword, Integer servings);
    Map<String, Object> addRecipeToCart(Integer customerId, Integer recipeId, Integer servings);
    List<RecipeDetailDto> getAllRecipes();
}
