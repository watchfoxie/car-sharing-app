package com.usarbcs.wallet.service.payload;

import com.usarbcs.wallet.service.dto.WalletDto;
import com.usarbcs.wallet.service.dto.WalletPaymentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletSnapshotPayload {
    private WalletDto wallet;
    private List<WalletPaymentDto> recentPayments;
}
