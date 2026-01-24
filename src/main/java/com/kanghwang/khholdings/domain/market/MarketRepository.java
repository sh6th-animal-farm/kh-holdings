package com.kanghwang.khholdings.domain.market;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;

@Mapper
public interface MarketRepository {

	List<MarketDTO> selectAll();

	List<MarketDTO> selectBySearch(String content);

	// 특정 토큰 현재가 조회
	BigDecimal selectLatestTokenPrice(Long tokenId);
}
