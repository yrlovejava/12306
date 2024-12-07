-- 定义 Redis 键
local hashKey = 'snowflake_work_id_key'
local dataCenterIdKey = 'dataCenterId'
local workIdKey = 'workId'

-- 检查是否初始化，没有直接返回，并初始化
if (redis.call('exists', hashKey) == 0) then
    redis.call('hincrby', hashKey, dataCenterIdKey, 0)
    redis.call('hincrby', hashKey, workIdKey, 0)
    return { 0, 0 }
end

-- 获取当前的数据中心ID
local dataCenterId = tonumber(redis.call('hget', hashKey, dataCenterIdKey))
-- 获取工作ID
local workId = tonumber(redis.call('hget', hashKey, workIdKey))

-- 定义最大值和返回值
local max = 31
local resultWorkId = 0
local resultDataCenterId = 0

if (dataCenterId == max and workId == max) then
    redis.call('hset', hashKey, dataCenterIdKey, '0')
    redis.call('hset', hashKey, workIdKey, '0')
elseif (workId == max) then
    resultWorkId = redis.call('hincrby', hashKey, workIdKey, 1)
    resultDataCenterId = dataCenterId
    redis.call('hset', hashKey, workIdKey, '0')
end

return { resultWorkId, resultDataCenterId }
