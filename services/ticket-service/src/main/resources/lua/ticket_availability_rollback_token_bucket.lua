-- KEYS[1] 令牌桶key
-- KEYS[2] hashKey
-- ARGV[1] 座位类型对应的数量
-- ARGV[2] 所有扣减了数量的路线

local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)
local alongJsonArrayStr = ARGV[2]
local alongJsonArray = cjson.decode(alongJsonArrayStr)

-- 遍历座位类型数组增加对应数量
for index, jsonObj in ipairs(jsonArray) do
    local seatType = tonumber(jsonObj.seatType)
    local count = tonumber(jsonObj.count)
    -- 每个扣减了数量的路线都需要加回去
    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do
        local startStation = tostring(alongJsonObj.startStation)
        local endStation = tostring(alongJsonObj.endStation)
        local alongKey = startStation .. "_" .. endStation .. "_" .. seatType
        -- 获取令牌桶中剩下的
        local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(alongKey)))
        if ticketSeatAvailabilityTokenValue >= 0 then
            redis.call('hincrby', KEYS[1], tostring(alongKey), count)
        end
    end
end
return 0
