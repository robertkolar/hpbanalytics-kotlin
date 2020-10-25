package com.highpowerbear.hpbanalytics.config;

import com.highpowerbear.hpbanalytics.enums.Currency;
import com.highpowerbear.hpbanalytics.enums.StatisticsPlMethod;

/**
 * Created by robertk on 5/29/2017.
 */
public class HanSettings {

    private HanSettings() {
    }

    public static final int SCHEDULED_THREAD_POOL_SIZE = 10;

    public static final int IFI_START_YEAR = 2016;
    public static final int DECIMAL_SCALE = 5;

    public static final Currency PORTFOLIO_BASE = Currency.EUR;
    public static final StatisticsPlMethod STATISTICS_PL_METHOD = StatisticsPlMethod.PORTFOLIO_BASE_CLOSE_ONLY;

    public static final String EXCHANGE_RATE_DATE_FORMAT = "yyyy-MM-dd";
    public static final int EXCHANGE_RATE_RETRIEVAL_DELAY_SECONDS = 8;

    public static final String HAZELCAST_INSTANCE_NAME = "han-instance";
    public static final String HAZELCAST_EXECUTION_QUEUE_NAME = "executionQueue";
    public static final int HAZELCAST_EXECUTION_QUEUE_MAX_SZE = 1000;
    public static final int HAZELCAST_EXECUTION_QUEUE_BACKUP_COUNT = 1;
    public static final int HAZELCAST_CONSUMER_START_DELAY_SECONDS = 10;

    public static final String DB_DATABASE = "hpbanalytics";
    public static final String DB_SCHEMA = "hpbanalytics";

    public static final String WS_RELOAD_REQUEST_MESSAGE = "reloadRequest";
}
