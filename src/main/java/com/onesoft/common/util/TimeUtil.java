package com.onesoft.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class TimeUtil {

    private static final Logger logger = LoggerFactory.getLogger(TimeUtil.class);

    /**
     * 작업 소요 시간 출력
     */
    public static void showWorkTime(long startTime, long endTime, String workName) {
        logger.info("#######################################################");
        long elapsedTime = endTime - startTime;
        long seconds = elapsedTime / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, secs);

        logger.info("## {} 작업 시작 시간: {}", workName, new Date(startTime));
        logger.info("## {} 작업 종료 시간: {}", workName, new Date(endTime));
        logger.info("## {} 작업 소요 시간: {} ms ({} hh:mm:ss)", workName, elapsedTime, formattedTime);
        logger.info("#######################################################");
    }
}