package com.navbi.nav;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NavCategoryService {

    private final NavCategoryMapper categoryMapper;
    private final NavItemMapper navItemMapper;

    public NavCategoryService(NavCategoryMapper categoryMapper, NavItemMapper navItemMapper) {
        this.categoryMapper = categoryMapper;
        this.navItemMapper = navItemMapper;
    }

    public List<NavCategory> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<NavCategory>()
                .orderByAsc(NavCategory::getSort)
                .orderByAsc(NavCategory::getId));
    }

    public NavCategory add(NavCategory category) {
        String name = normalizeRequiredName(category.getName());
        ensureNameAvailable(name, null);
        category.setId(null);
        category.setName(name);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category);
        return category;
    }

    public void update(NavCategory category) {
        if (category.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        NavCategory old = categoryMapper.selectById(category.getId());
        if (old == null) {
            throw new IllegalArgumentException("分类不存在: " + category.getId());
        }
        String name = normalizeRequiredName(category.getName());
        ensureNameAvailable(name, category.getId());
        category.setName(name);
        if (category.getSort() == null) {
            category.setSort(0);
        }
        category.setCreatedAt(null);
        category.setUpdatedAt(LocalDateTime.now());
        categoryMapper.updateById(category);
        if (!old.getName().equals(name)) {
            navItemMapper.update(null, new LambdaUpdateWrapper<NavItem>()
                    .eq(NavItem::getCategory, old.getName())
                    .set(NavItem::getCategory, name)
                    .set(NavItem::getUpdatedAt, LocalDateTime.now()));
        }
    }

    public void delete(Long id) {
        NavCategory category = categoryMapper.selectById(id);
        if (category == null) {
            return;
        }
        Long used = navItemMapper.selectCount(new LambdaQueryWrapper<NavItem>()
                .eq(NavItem::getCategory, category.getName()));
        if (used != null && used > 0) {
            throw new IllegalArgumentException("分类仍有导航项，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    public String requireExistingForNavItem(String category) {
        String name = category == null || category.isBlank() ? "默认" : normalizeRequiredName(category);
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<NavCategory>()
                .eq(NavCategory::getName, name));
        if (count == null || count == 0) {
            throw new IllegalArgumentException("分类不存在: " + name);
        }
        return name;
    }

    private void ensureNameAvailable(String name, Long currentId) {
        LambdaQueryWrapper<NavCategory> wrapper = new LambdaQueryWrapper<NavCategory>()
                .eq(NavCategory::getName, name);
        if (currentId != null) {
            wrapper.ne(NavCategory::getId, currentId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("分类已存在: " + name);
        }
    }

    private String normalizeRequiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        String normalized = name.trim();
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("分类名称不能超过 64 个字符");
        }
        return normalized;
    }
}
