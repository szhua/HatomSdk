package com.fencer.hatomsdk;

import java.text.DecimalFormat;

public class TrafficUtils {

    private static final int DATA_UNIT = 1024;

    public TrafficUtils() {
    }

    public static String getCurrentTraffic(long diffValue) {
        double result = (double) diffValue / 1024.0D;
        String unit = "KB/s";
        String currentFlow = "0.00";
        if (result < 0.0D) {
            currentFlow = "0.00";
        } else {
            DecimalFormat format;
            if (result < 10.0D) {
                format = new DecimalFormat("0.00");
                currentFlow = format.format(result);
            } else if (result < 100.0D) {
                format = new DecimalFormat("#0.0");
                currentFlow = format.format(result);
            } else if (result < 1000.0D) {
                format = new DecimalFormat("##0");
                currentFlow = format.format(result);
            } else {
                unit = "MB/s";
                result /= 1024.0D;
                format = new DecimalFormat("#0.00");
                currentFlow = format.format(result);
            }
        }

        return currentFlow + unit;
    }

    public static String getTotalTraffic(long currentDataLength) {
        double result = (double) currentDataLength / 1048576.0D;
        String unit = "MB";
        if (result >= 1000.0D) {
            unit = "GB";
            result /= 1024.0D;
        }

        DecimalFormat format = new DecimalFormat("###0.00");
        String totalFlow = format.format(result);
        return totalFlow + unit;
    }

}
