package com.yxshop.Utils;

public class SnowflakeIdWorker {
    // 工作机器ID
    private long workerId;
    // 数据中心ID
    private long datacenterId;
    // 序列号，用于同一毫秒内的不同ID生成
    private long sequence = 0L;
    // 时间戳偏移量，通常设置为某个固定的纪元时间
    private long twepoch = 1288834974657L;
    // 工作机器ID位数
    private long workerIdBits = 5L;
    // 数据中心ID位数
    private long datacenterIdBits = 5L;
    // 最大工作机器ID，由workerIdBits决定
    private long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 最大数据中心ID，由datacenterIdBits决定
    private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    // 序列号位数
    private long sequenceBits = 12L;
    // 工作机器ID左移位数
    private long workerIdShift = sequenceBits;
    // 数据中心ID左移位数
    private long datacenterIdShift = sequenceBits + workerIdBits;
    // 时间戳左移位数
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    // 序列号掩码，用于循环计数
    private long sequenceMask = -1L ^ (-1L << sequenceBits);
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;

    // 构造函数，初始化工作机器ID和数据中心ID
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("工作机器ID不能大于%d或小于0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("数据中心ID不能大于%d或小于0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // 同步方法，生成下一个ID
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 检查时间戳是否回退
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("时钟向后移动。拒绝在%d毫秒内生成ID", lastTimestamp - timestamp));
        }

        // 如果时间戳相同，则增加序列号
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 如果序列号溢出，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 如果时间戳改变，则重置序列号
            sequence = 0L;
        }

        // 更新上次生成ID的时间戳
        lastTimestamp = timestamp;

        // 拼接各个部分生成最终的ID
        return ((timestamp - twepoch) << timestampLeftShift) |
               (datacenterId << datacenterIdShift) |
               (workerId << workerIdShift) | sequence;
    }

    // 获取下一毫秒的时间戳
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        // 等待直到时间戳大于上一次的时间戳
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    // 生成当前时间的时间戳
    protected long timeGen() {
        return System.currentTimeMillis();
    }
}