package com.xiangkai.community.service;

import com.xiangkai.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DataService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    // 将指定的IP计入UV
    public void recordUV(String ip) {
        String redisKey = RedisUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    // 统计指定日期范围内的UV
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 整理该日期范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE, 1);
        }

        // 合并这些数据
        String redisKey = RedisUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey, Arrays.toString(keyList.toArray()));

        // 返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    // 将指定用户计入DAU
    public void recordDAU(int userId) {
        String redisKey = RedisUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    // 统计指定日期范围内的DAU
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }

        // 整理该日期范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }

        // 进行OR运算
        return (long) redisTemplate.execute((RedisCallback) connection -> {
            String redisKey = RedisUtil.getDAUKey(df.format(start), df.format(end));
            connection.bitOp(RedisStringCommands.BitOperation.OR,
                    redisKey.getBytes(), keyList.toArray(new byte[0][0]));
            return connection.bitCount(redisKey.getBytes());
        });
    }
}
