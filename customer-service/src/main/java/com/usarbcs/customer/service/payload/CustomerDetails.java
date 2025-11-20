package com.usarbcs.customer.service.payload;


import com.usarbcs.core.details.BankAccount;
import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.customer.service.dto.CustomerDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDetails {
    private CustomerDto customer;
    private DriverLocationDto driverLocationDto;
    private BankAccount bankAccount;
    private WalletDetails walletDetails;

    public CustomerDetails(CustomerDto customer,
                           DriverLocationDto driverLocationDto,
                           BankAccount bankAccount,
                           WalletDetails walletDetails) {
        this.customer = customer;
        this.driverLocationDto = driverLocationDto;
        this.bankAccount = bankAccount;
        this.walletDetails = walletDetails;
    }
}
