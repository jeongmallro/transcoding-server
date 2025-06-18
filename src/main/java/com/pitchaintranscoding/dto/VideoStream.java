package com.pitchaintranscoding.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record VideoStream(int width, int height, double fps) {

    public static final Map<String, String> BITRATE = Map.of(
            "2160", "20000k",  // 4K - 20Mbps
            "1440", "12000k",  // 2K - 12Mbps
            "1080", "6000k",   // FHD - 6Mbps
            "720", "2500k",    // HD - 2.5Mbps
            "480", "1500k",    // SD - 1.5Mbps
            "360", "800k",     // Low - 800Kbps
            "240", "400k",     // Very Low - 400Kbps
            "144", "200k"      // Lowest - 200Kbps
    );
    private static final Map<String, String> resolutions = Map.of(
            "2160", "3840:2160",
            "1440", "2560:1440",
            "1080", "1920:1080",
            "720", "1280:720",
            "480", "854:480",
            "360", "640:360",
            "240", "426:240",
            "144", "256:144"
    );

    public List<String> getAvailableQualities() {
        List<String> qualities = new ArrayList<>();
        if (height >= 2160) qualities.add("2160");      // 4K (3840x2160)
        if (height >= 1440) qualities.add("1440");      // 2K (2560x1440)
        if (height >= 1080) qualities.add("1080");      // Full HD (1920x1080)
        if (height >= 720) qualities.add("720");        // HD (1280x720)
        if (height >= 480) qualities.add("480");        // SD (854x480)
        qualities.add("360");                           // Low (640x360)
        return qualities;
    }

    public List<String> getScales() {
        List<String> scales = new ArrayList<>();
        List<String> qualities = getAvailableQualities();

        for (int i = 0; i < qualities.size(); i++) {
            int streamIndex = qualities.size() - i;

            String quality = qualities.get(i);
            String scale = resolutions.get(quality);
            scales.add(String.format("[v%d]scale=%s[v%sp]", streamIndex, scale, quality));
        }
        return scales;
    }

}
