//package com.kanghwang.khholdings.domain.market.type;
//
//import lombok.Getter;
//
//@Getter
//public enum UnitEnum {
//    MIN_1("1m", 1),
//    MIN_5("5m", 5),
//    MIN_15("15m", 15),
//    HOUR_1("1h", 60),
//    HOUR_4("4h", 240),
//    DAY_1("1d", 1440);
//
//    private final String code;      // API/Redis용 (1m, 5m)
//    private final int minutes;      // 계산용
//
//    UnitEnum(String code, int minutes) {
//        this.code = code;
//        this.minutes = minutes;
//    }
//
//    // Spring @RequestParam에서 "1m"을 Enum으로 변환할 때 사용
//    @com.fasterxml.jackson.annotation.JsonCreator
//    public static UnitEnum fromCode(String code) {
//        for (UnitEnum unit : values()) {
//            if (unit.code.equalsIgnoreCase(code)) return unit;
//        }
//        throw new IllegalArgumentException("지원하지 않는 단위입니다: " + code);
//    }
//
//    @com.fasterxml.jackson.annotation.JsonValue
//    public String getCode() { return code; }
//    public int getMinutes() { return minutes; }
//}
