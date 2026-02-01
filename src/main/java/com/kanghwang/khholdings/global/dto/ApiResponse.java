package com.kanghwang.khholdings.global.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {
	private String message;
	private T payload;
}
