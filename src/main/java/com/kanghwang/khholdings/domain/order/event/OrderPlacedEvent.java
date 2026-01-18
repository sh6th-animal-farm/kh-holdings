package com.kanghwang.khholdings.domain.order.event;

import com.kanghwang.khholdings.domain.order.dto.OrderRequestDTO;

public record OrderPlacedEvent(OrderRequestDTO order) {
}
