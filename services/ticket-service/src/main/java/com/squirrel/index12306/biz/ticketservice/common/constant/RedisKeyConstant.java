package com.squirrel.index12306.biz.ticketservice.common.constant;

/**
 * Redis Key 定义常量类
 */
public final class RedisKeyConstant {

    /**
     * 列车基本信息: Key Prefix + 列车ID
     */
    public static final String TRAIN_INFO = "index12306-ticket-service:train_info:";

    /**
     * 站点查询，Key Prefix + 起始城市_终点城市_日期
     */
    public static final String REGION_TRAIN_STATION = "index12306-ticket-service:region_train_station:";

    /**
     * 地区以及车站查询 Key Prefix + (车站名称 or 查询方式)
     */
    public static final String REGION_STATION = "index12306-ticket-service:region-station:";

    /**
     * 站点余票查询，Key Prefix + 列车ID_起始站点_终点
     */
    public static final String TRAIN_STATION_REMAINING_TICKET = "index12306-ticket-service:train_station_remaining_ticket:";

    /**
     * 列车车厢查询，Key Prefix + 列车ID
     */
    public static final String TRAIN_CARRIAGE = "index12306-ticket-service:train_carriage:";

    /**
     * 车厢余票查询，Key Prefix + 列车ID_起始站点_终点
     */
    public static final String TRAIN_STATION_CARRIAGE_REMAINING_TICKET = "index12306-ticket-service:train_station_carriage_remaining_ticket:";

    /**
     * 站点详细信息查询，Key Prefix + 列车ID_起始站点_终点
     */
    public static final String TRAIN_STATION_DETAIL = "index12306-ticket-service:train_station_detail:";

    /**
     * 列车路线信息查询，Key Prefix + 列车ID
     */
    public static final String TRAIN_STATION_STOPOVER_DETAIL = "index12306-ticket-service:train_station_stopover_detail:";

    /**
     * 列车购买令牌桶，Key Prefix + 列车ID
     */
    public static final String TICKET_AVAILABILITY_TOKEN_BUCKET = "index12306-ticket-service:ticket_availability_token_bucket:";

    /**
     * 列车站点缓存
     */
    public static final String STATION_ALL = "index12306-ticket-service:all_station";

    /**
     * 列车车厢状态， Key Prefix + 列车 ID + 起始站点 + 目的站点 + 车厢编号
     */
    public static final String TRAIN_CARRIAGE_SEAT_STATUS = "index12306-ticket-service:train_carriage_seat_status:";

    /**
     * 用户购票分布式锁 Key: 后续替换为 LUA 脚本就不需要了
     */
    public static final String LOCK_PURCHASE_TICKETS = "index12306-ticket-service:lock:purchase_tickets_%s";

    /**
     * 获取全部地点集合 Key
     */
    public static final String QUERY_ALL_REGION_LIST = "index12306-ticket-service:query_all_region_list";

    /**
     * 获取全部地点集合分布式锁 Key
     */
    public static final String LOCK_QUERY_REGION_LIST = "index12306-ticket-service:lock:query_all_region_list";

    /**
     * 获取列车车厢数量集合分布式锁 Key
     */
    public static final String LOCK_QUERY_CARRIAGE_NUMBER_LIST = "index12306-ticket-service:lock:query_carriage_number_list_%s";

    /**
     * 获取地区以及站点集合分布式锁 Key
     */
    public static final String LOCK_QUERY_REGION_STATION_LIST = "index12306-ticket-service:lock:query_region_station_list_%s";

    /**
     * 获取相邻座位余票分布式锁 Key
     */
    public static final String LOCK_SAFE_LOAD_SEAT_MARGIN_GET = "index12306-ticket-service:lock:safe_load_seat_margin_%s";

    /**
     * 列车购买令牌桶加载数据 key
     */
    public static final String LOCK_TICKET_AVAILABILITY_TOKEN_BUCKET = "index12306-ticket-service:lock:ticket_availability_token_bucket_%s";
}
