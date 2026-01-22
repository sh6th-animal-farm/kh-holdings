package com.kanghwang.khholdings.domain.my.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxHistsSearchDTO {
    private Long walletId;
    private String category;
    private String period;
    private Integer page = 1;

    public int getOffset() {
        return (this.page == null || this.page < 1) ? 0 : (this.page - 1) * 10;
    }
}
