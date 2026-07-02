package com.navbi.bi;

import com.navbi.track.VisitLog;

import java.util.List;

public record LogPage(long total, List<VisitLog> records) {
}
