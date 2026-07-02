package com.navbi.nav;

import java.util.List;

public record NavGroup(String category, List<NavItem> items) {
}
