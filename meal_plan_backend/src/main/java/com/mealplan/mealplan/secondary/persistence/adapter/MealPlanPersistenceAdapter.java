package com.mealplan.mealplan.secondary.persistence.adapter;

import com.mealplan.mealplan.application.output.MealPlanRepositoryPort;
import com.mealplan.mealplan.domain.entity.GenerationRules;
import com.mealplan.mealplan.domain.entity.MealPlan;
import com.mealplan.mealplan.domain.entity.MealPlanId;
import com.mealplan.mealplan.domain.entity.MenuCategory;
import com.mealplan.mealplan.domain.entity.MenuItem;
import com.mealplan.mealplan.secondary.persistence.document.MealPlanDocument;
import com.mealplan.mealplan.secondary.persistence.document.MealPlanMongoRepository;
import com.mealplan.mealplan.secondary.persistence.mapper.MealPlanMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Component;

/**
 * {@link MealPlanRepositoryPort} 의 MongoDB 구현 어댑터 (Outbound).
 */
@Component
public class MealPlanPersistenceAdapter implements MealPlanRepositoryPort {

    private final MealPlanMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MealPlanPersistenceAdapter(MealPlanMongoRepository repository,
                                      MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public MealPlan save(MealPlan mealPlan) {
        MealPlanDocument saved = repository.save(MealPlanMapper.toDocument(mealPlan));
        return MealPlanMapper.toDomain(saved);
    }

    @Override
    public Optional<MealPlan> findById(MealPlanId id) {
        return repository.findById(id.value()).map(MealPlanMapper::toDomain);
    }

    @Override
    public List<MealPlan> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "importedAt")).stream()
                .map(MealPlanMapper::toDomain)
                .toList();
    }

    @Override
    public long deleteByIds(List<MealPlanId> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        List<String> idValues = ids.stream().map(MealPlanId::value).toList();
        var query = org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").in(idValues));
        return mongoTemplate.remove(query, MealPlanDocument.class).getDeletedCount();
    }

    /**
     * 카테고리별 빈도를 DB(aggregation)로 집계한 뒤, "인기 상위 + 랜덤 샘플"로 후보를 구성한다.
     *
     * <p>집계(메뉴 펼치기→메뉴별 카운트→정렬)는 MongoDB가 수행하므로, 전체 문서를 앱 메모리로
     * 끌어오지 않는다. 집계 결과는 "고유 메뉴 수"로 축소되어 작다. 그 안에서 카테고리별
     * 상위 N(인기) + 나머지에서 랜덤 N(다양성)을 골라 반환한다.
     */
    @Override
    public java.util.Map<MenuCategory, List<MenuItem>> loadCandidatePool() {
        // ① DB 집계: items 펼치기 → (category, name)별 count → count 내림차순 → 평탄화 투영
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.unwind("items"),
                Aggregation.group("items.category", "items.name").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.project("count")
                        .and("_id.category").as("category")
                        .and("_id.name").as("name"));

        AggregationResults<MenuFrequency> results =
                mongoTemplate.aggregate(agg, "meal_plans", MenuFrequency.class);

        // 카테고리별로 빈도 내림차순 목록을 모은다 (집계가 이미 정렬됨)
        java.util.Map<MenuCategory, List<String>> byCategory = new EnumMap<>(MenuCategory.class);
        for (MenuFrequency mf : results) {
            MenuCategory category = parseCategory(mf.category());
            if (category == null || mf.name() == null || mf.name().isBlank()) {
                continue;
            }
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(mf.name());
        }

        // ② 카테고리별 "인기 상위 + 랜덤 샘플" 구성
        java.util.Map<MenuCategory, List<MenuItem>> pool = new EnumMap<>(MenuCategory.class);
        byCategory.forEach((category, namesByFreqDesc) -> {
            List<String> picked = pickPopularAndRandom(category, namesByFreqDesc);
            List<MenuItem> items = picked.stream().map(n -> MenuItem.of(n, category)).toList();
            if (!items.isEmpty()) {
                pool.put(category, items);
            }
        });
        return pool;
    }

    /** 빈도 내림차순 목록에서 인기 상위 + 나머지 랜덤 샘플을 합쳐 반환. */
    private List<String> pickPopularAndRandom(MenuCategory category, List<String> namesByFreqDesc) {
        int total = GenerationRules.CANDIDATE_POOL_SIZE.getOrDefault(category, 0);
        if (total <= 0 || namesByFreqDesc.size() <= total) {
            // 정책 개수가 없거나, 고유 메뉴가 정책보다 적으면 있는 것 전부
            return new ArrayList<>(namesByFreqDesc);
        }

        int popular = Math.min(GenerationRules.popularCount(category), namesByFreqDesc.size());
        List<String> result = new ArrayList<>(namesByFreqDesc.subList(0, popular));

        // 나머지(인기 제외)에서 랜덤 샘플
        List<String> remainder = new ArrayList<>(
                namesByFreqDesc.subList(popular, namesByFreqDesc.size()));
        int randomCount = Math.min(GenerationRules.randomCount(category), remainder.size());
        Collections.shuffle(remainder);
        result.addAll(remainder.subList(0, randomCount));
        return result;
    }

    private MenuCategory parseCategory(String value) {
        try {
            return MenuCategory.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** 집계 결과 매핑용 (평탄화된 형태). */
    public record MenuFrequency(String category, String name, long count) {
    }
}
