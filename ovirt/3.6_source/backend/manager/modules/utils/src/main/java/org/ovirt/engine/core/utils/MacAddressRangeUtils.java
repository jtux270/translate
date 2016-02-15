package org.ovirt.engine.core.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.LongRange;
import org.apache.commons.lang.math.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacAddressRangeUtils {

    private static final Logger log = LoggerFactory.getLogger(MacAddressRangeUtils.class);

    private static final int HEX_RADIX = 16;

    public static final long MAC_ADDRESS_MULTICAST_BIT = 0x010000000000L;

    private MacAddressRangeUtils() {
    }

    public static List<String> macAddressesToStrings(List<Long> macAddresses) {
        final List<String> result = new ArrayList<>(macAddresses.size());

        for (Long macAddress : macAddresses) {
            result.add(macToString(macAddress));
        }

        return result;
    }

    public static Collection<LongRange> parseRangeString(String ranges) {
        if (StringUtils.isEmpty(ranges)) {
            return Collections.emptyList();
        }

        String[] rangesArray = ranges.split("[,]", -1);
        DisjointRanges disjointRanges = new DisjointRanges();

        for (int i = 0; i < rangesArray.length; i++) {
            String[] startEndArray = rangesArray[i].split("[-]", -1);

            if (startEndArray.length == 2) {
                disjointRanges.addRange(macToLong(startEndArray[0]), macToLong(startEndArray[1]));
            } else {
                throw new IllegalArgumentException(
                        "Failed to initialize Mac Pool range. Please fix Mac Pool range: rangesArray[i]");
            }
        }

        return clipMultiCastsFromRanges(disjointRanges.getRanges());
    }

    public static Collection<LongRange> clipMultiCastsFromRanges(Collection<LongRange> ranges) {
        final Collection<LongRange> result = new ArrayList<>();
        for (LongRange range : ranges) {
            final LongRange clippedRange = clipRange(range);
            if (clippedRange != null) {
                result.add(clippedRange);
            }
        }
        return result;
    }

    public static LongRange clipRange(Range range) {
        long rangeEnd = range.getMaximumLong();
        long rangeStart = range.getMinimumLong();

        boolean trimmingOccurred = false;
        if (MacAddressRangeUtils.macIsMulticast(rangeStart)) {
            rangeStart = (rangeStart | 0x00FFFFFFFFFFL) + 1;
            trimmingOccurred = true;
        }

        final long trimmedRangeEnd = Math.min(rangeStart + Integer.MAX_VALUE - 1, rangeEnd);
        if (rangeEnd != trimmedRangeEnd) {
            rangeEnd = trimmedRangeEnd;
            trimmingOccurred = true;
        }

        if (MacAddressRangeUtils.macIsMulticast(rangeEnd)) {
            rangeEnd = (rangeEnd & 0xFF0000000000L) - 1;
            trimmingOccurred = true;
        }

        if (rangeStart > rangeEnd) {
            log.warn(
                    "User supplied range({}) contains only multicast addresses, so this range is not usable.", range);
            return null;
        }

        final LongRange result = new LongRange(rangeStart, rangeEnd);
        if (trimmingOccurred) {
            log.warn("User supplied range({}) need to be trimmed to {}.", range, result);
        }
        return result;
    }

    public static boolean macIsMulticast(long mac) {
        return (MAC_ADDRESS_MULTICAST_BIT & mac) != 0;
    }

    public static String macToString(long macAddress) {
        String value = String.format("%012x", macAddress);
        char[] chars = value.toCharArray();

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(chars[0]).append(chars[1]);
        for (int pos = 2; pos < value.length(); pos += 2) {
            stringBuilder.append(":")
                    .append(chars[pos])
                    .append(chars[pos + 1]);
        }

        return stringBuilder.toString();
    }

    public static long macToLong(String mac) {
        return Long.parseLong(StringUtils.remove(mac, ':'), HEX_RADIX);
    }

    public static boolean isRangeValid(String start, String end) {
        long startNum = macToLong(start);
        long endNum = macToLong(end);

        if (startNum > endNum) {
            return false;
        }

        Collection<LongRange> ranges = parseRangeString(start + "-" + end);

        for (LongRange range : ranges) {
            if (range.getMaximumLong() - range.getMinimumLong() < 0) {
                return false;
            }
        }

        return true;
    }
}
