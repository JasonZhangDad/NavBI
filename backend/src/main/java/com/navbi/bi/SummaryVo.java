package com.navbi.bi;

public record SummaryVo(long todayPv, long todayUv, long yesterdayPv,
                        long totalPv, long totalUv, long todayIpCount,
                        long ipCount, long todayRegisterCount) {
}
