package com.navbi.nav;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.navbi.counter.CounterService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NavService {

    private final NavItemMapper navItemMapper;
    private final CounterService counterService;
    private final IconService iconService;
    private final NavCategoryService categoryService;

    public NavService(NavItemMapper navItemMapper, CounterService counterService, IconService iconService,
                      NavCategoryService categoryService) {
        this.navItemMapper = navItemMapper;
        this.counterService = counterService;
        this.iconService = iconService;
        this.categoryService = categoryService;
    }

    /** 前台导航：仅启用项，按分类分组，组内按 sort 升序。 */
    public List<NavGroup> listGrouped(String keyword) {
        LambdaQueryWrapper<NavItem> wrapper = new LambdaQueryWrapper<NavItem>()
                .eq(NavItem::getEnabled, true)
                .orderByAsc(NavItem::getCategory)
                .orderByAsc(NavItem::getSort)
                .orderByAsc(NavItem::getId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            // 分类不参与搜索：分类筛选是前台独立的过滤器
            wrapper.and(w -> w.like(NavItem::getTitle, kw)
                    .or().like(NavItem::getUrl, kw));
        }
        Map<String, List<NavItem>> grouped = new LinkedHashMap<>();
        for (NavItem item : navItemMapper.selectList(wrapper)) {
            grouped.computeIfAbsent(item.getCategory(), k -> new java.util.ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream()
                .map(e -> new NavGroup(e.getKey(), e.getValue()))
                .toList();
    }

    /** 管理端：全部导航项。 */
    public List<NavItem> listAll() {
        return navItemMapper.selectList(new LambdaQueryWrapper<NavItem>()
                .orderByAsc(NavItem::getCategory)
                .orderByAsc(NavItem::getSort)
                .orderByAsc(NavItem::getId));
    }

    public NavItem add(NavItem item) {
        item.setId(null);
        item.setCategory(categoryService.requireExistingForNavItem(item.getCategory()));
        if (item.getSort() == null) {
            item.setSort(0);
        }
        if (item.getEnabled() == null) {
            item.setEnabled(true);
        }
        item.setClickCount(0L);
        item.setUvCount(0L);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        navItemMapper.insert(item);
        return item;
    }

    public void update(NavItem item) {
        if (item.getId() == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        // 点击统计字段不允许通过更新接口篡改
        item.setClickCount(null);
        item.setUvCount(null);
        item.setCreatedAt(null);
        item.setUpdatedAt(LocalDateTime.now());
        if (item.getCategory() != null) {
            item.setCategory(categoryService.requireExistingForNavItem(item.getCategory()));
        }
        if (navItemMapper.updateById(item) == 0) {
            throw new IllegalArgumentException("导航不存在: " + item.getId());
        }
        iconService.evict(item.getId());
    }

    public void delete(Long id) {
        navItemMapper.deleteById(id);
        iconService.evict(id);
    }

    /** 点击计数：PV 直接 +1；UV 借助 Redis SET 按 sessionId 去重。 */
    public void click(Long id, String sessionId) {
        boolean newVisitor = sessionId != null && !sessionId.isBlank()
                && counterService.isNewNavClicker(id, sessionId);
        LambdaUpdateWrapper<NavItem> wrapper = new LambdaUpdateWrapper<NavItem>()
                .eq(NavItem::getId, id)
                .setSql("click_count = click_count + 1")
                .setSql(newVisitor, "uv_count = uv_count + 1");
        navItemMapper.update(null, wrapper);
    }
}
