package com.kanghwang.khholdings.global.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    // 기준 시간: 2026-01-01 00:00:00 KST
    private final long epoch = 1767193200000L;
    private final long sequenceBits = 12L;

    private long sequence = 0L;
    private long lastTimeStamp = -1L;

    public synchronized long nextId() {

        long timestamp = timeGen();

        if (timestamp < lastTimeStamp) {
            throw new RuntimeException("시간에 문제가 생겼습니다.");
        }

        if (lastTimeStamp == timestamp) {
            sequence  = (sequence + 1) & ((1L << sequenceBits) - 1);
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimeStamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimeStamp = timestamp;

        // 시간을 왼쪽으로 12칸 밀고, 그 자리에 순번을 넣습니다.
        return ((timestamp - epoch) << sequenceBits) | sequence;

    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
