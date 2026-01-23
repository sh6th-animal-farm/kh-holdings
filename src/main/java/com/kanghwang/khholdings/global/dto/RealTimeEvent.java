package com.kanghwang.khholdings.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RealTimeEvent<T> {
	private String action; // "INSERT", "UPDATE", "DELETE"
	private T data;        // 실제 데이터
}
