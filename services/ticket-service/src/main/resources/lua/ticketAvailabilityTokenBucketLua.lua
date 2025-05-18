-- KEYS[1] 令牌桶key
-- KEYS[2] 出发站_到达站点
-- ARGV[1] 座位类型和数量的JSON数组
-- ARGV[2] 所有路线的JSON数组

local inputString = KEYS[2]
local actualKey = inputString
local colonIndex = string.find(actualKey, ":")
if colonIndex ~= nil then
    actualKey = string.sub(actualKey, colonIndex + 1)
end

-- 解析JSON数组，[{seatType:1,count:1}]
local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)

-- 检查令牌桶中是否有足够的令牌
for index,jsonObject in ipairs(jsonArray) do
   local seatType = tonumber(jsonObject.seatType)
   local count = tonumber(jsonObject.count)
   -- 出发站_到达站点_座位类型
   local actualInnerHashKey = actualKey .. "_" .. seatType
   -- 检查令牌桶中是否有足够的令牌
   local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
   if ticketSeatAvailabilityTokenValue < count then
       return 1
   end
end

local alongJsonArrayStr = ARGV[2]
local alongJsonArray = cjson.decode(alongJsonArrayStr)

-- 减少令牌桶中的令牌
for index,jsonObj in ipairs(jsonArray) do
   local seatType = tonumber(jsonObj.seatType)
   local count = tonumber(jsonObj.count)
   for indexTwo,alongJosnObj in ipairs(alongJsonArray) do
       local startStation = tostring(alongJosnObj.startStation)
       local endStation = tostring(alongJosnObj.endStation)
       -- 出发站_到达站点_座位类型
       local actualInnerHashKey = startStation .. "_" .. endStation .. "_" .. seatType
       redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), -count)
   end
end

return 0