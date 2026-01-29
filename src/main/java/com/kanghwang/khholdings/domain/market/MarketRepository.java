package com.kanghwang.khholdings.domain.market;

import java.math.BigDecimal;
import java.util.List;

import com.kanghwang.khholdings.domain.order.dto.CandleDTO;
import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.market.dto.TokenListDTO;
import com.kanghwang.khholdings.domain.market.dto.PendingDTO;

@Mapper
public interface MarketRepository {

	// 종목 전체 조회
	List<TokenListDTO> selectAll();

	// 종목 검색어 조회
	List<TokenListDTO> selectBySearch(String content);

	// 미체결 내역 조회
	List<PendingDTO> selectPending(Long tokenId, Long walletId);

	// 특정 토큰 현재가 조회
	BigDecimal selectLatestTokenPrice(Long tokenId);

	// 차트 조회
	List<CandleDTO> selectCandles(Long tokenId, int unit, int limit);
}
