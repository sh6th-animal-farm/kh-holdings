package com.kanghwang.khholdings.domain.market;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.kanghwang.khholdings.domain.market.dto.MarketDTO;

@Mapper
public interface MarketRepository {

	public abstract List<MarketDTO> selectAll();

	public abstract List<MarketDTO> selectBySearch(String content);
}
