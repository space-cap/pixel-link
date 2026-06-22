package com.pixellink.dto;

import lombok.Data;
import java.util.List;

@Data
public class LinkAnalyticsResponse {
    private String linkId;
    private List<DailyClickStat> dailyClicks;
    private List<DeviceStat> devices;
    private List<OsStat> operatingSystems;
    private List<ReferrerStat> referrers;

    @Data
    public static class DailyClickStat {
        private String date;
        private int clicks;
        private int adClicks;
        private int conversions;
    }

    @Data
    public static class DeviceStat {
        private String name;
        private int value;
    }

    @Data
    public static class OsStat {
        private String name;
        private int value;
    }

    @Data
    public static class ReferrerStat {
        private String name;
        private int value;
    }
}
