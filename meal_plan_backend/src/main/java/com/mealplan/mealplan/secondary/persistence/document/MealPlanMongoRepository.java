package com.mealplan.mealplan.secondary.persistence.document;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB 리포지토리.
 */
public interface MealPlanMongoRepository extends MongoRepository<MealPlanDocument, String> {
}
