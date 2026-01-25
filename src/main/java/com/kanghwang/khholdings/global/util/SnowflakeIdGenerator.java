package com.kanghwang.khholdings.global.util;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator {

    // 1. 기준 시간 (바꾸지 마세요. 유지해야 과거 ID와 정렬이 맞습니다)
    private final long epoch = 1767193200000L; // 2026-01-01

    // 2. 비트 할당 (표준 규격)
    private final long sequenceBits = 12L;   // 1ms당 4096개 생성 가능
    private final long workerIdBits = 5L;     // 서버 식별 (서버 1대라도 자리 비워둠)

    // 3. 밀기 연산 (이 숫자들이 시간 데이터를 결정합니다)
    private final long workerIdShift = sequenceBits; // 12칸
    private final long timestampLeftShift = sequenceBits + workerIdBits; // 17칸
    private final long sequenceMask = -1L ^ (-1L << sequenceBits); // 4095

    private long lastTimestamp = -1L;
    private long sequence = 0L;
    private final long workerId = 0L; // 현재 서버 1대이므로 0 고정

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards.");
        }

        if (lastTimestamp == timestamp) {
            // 같은 밀리초에 호출되면 sequence 증가
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 4096개를 다 쓰면 다음 밀리초까지 대기 (중복 방지 핵심)
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 시간이 흐르면 sequence는 다시 0부터
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // [중요] 비트 조립
        // 시간차를 17칸 왼쪽으로 밀어서 가장 높은 자리에 배치 (시간 순 정렬 보장)
        return ((timestamp - epoch) << timestampLeftShift) |
            (workerId << workerIdShift) |
            sequence;
    }

    protected Long tilNextMillis(Long lastTimestamp) {
        Long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected Long timeGen() {
        return System.currentTimeMillis();
    }
}
