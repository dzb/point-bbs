package com.jujin.point.service;

import com.jujin.point.domain.entity.Category;
import com.jujin.freeway.db.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Category service — tree structure management.
 */
public class CategoryService {
    private final Database db;

    public CategoryService(Database db) {
        this.db = db;
    }

    public List<Category> findAllActive() {
        return db.query("SELECT * FROM bbs_category WHERE status = 1 ORDER BY sort_no ASC")
            .list(Category.class);
    }

    public List<CategoryTree> getTree() {
        var all = findAllActive();
        var byParent = all.stream()
            .collect(Collectors.groupingBy(Category::getParentId));

        var roots = byParent.getOrDefault(0L, List.of());
        return roots.stream()
            .map(root -> buildTree(root, byParent))
            .collect(Collectors.toList());
    }

    private CategoryTree buildTree(Category cat, Map<Long, List<Category>> byParent) {
        var children = byParent.getOrDefault(cat.getId(), List.of()).stream()
            .map(child -> buildTree(child, byParent))
            .collect(Collectors.toList());
        return new CategoryTree(cat.getId(), cat.getName(), cat.getType(), cat.getDescription(), children);
    }

    public record CategoryTree(
        Long id, String name, String type, String description, List<CategoryTree> children
    ) {}
}
