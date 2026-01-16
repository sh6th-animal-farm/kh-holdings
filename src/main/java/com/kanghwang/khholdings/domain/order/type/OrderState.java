package com.kanghwang.khholdings.domain.order.type;

public enum OrderState {
	NEW,		//신규
	PARTIAL,	//부분체결
	COMPLETED, 	//체결완료
	CANCELLED,	// 주문취소
	EJECTED		//주문실패
}
