package com.usarbcs.customer.service.service.customer;


import com.usarbcs.core.details.BankAccount;
import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.util.JSONUtil;
import com.usarbcs.customer.service.command.CustomerCommand;
import com.usarbcs.customer.service.command.CustomerInfoUpdateCmd;
import com.usarbcs.customer.service.command.CustomerRequestDriver;
import com.usarbcs.customer.service.command.RatingCommand;
import com.usarbcs.customer.service.criteria.CustomerCriteria;
import com.usarbcs.customer.service.mapper.CustomerMapper;
import com.usarbcs.customer.service.model.Customer;
import com.usarbcs.customer.service.model.Driver;
import com.usarbcs.customer.service.payload.CustomerDetails;
import com.usarbcs.customer.service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService{

    private static final String RATING_SERVICE_URI = "http://rating-service:8086/rating-service/v1/ratings";
    private static final String DRIVER_SERVICE_AVAILABLE_URI = "http://driver-service:8087/v1/drivers/available";
    private static final String DRIVER_LOCATION_DETAILS_URI = "http://driver-location-service:8083/v1/driver-location/driver-location-details/%s";
    private static final String PAYMENT_ACCOUNT_DETAILS_URI = "http://payment-service:8084/v1/payment/account-details/%s";
    private static final String WALLET_DETAILS_BY_ACCOUNT_URI = "http://wallet-service:8085/v1/wallet/payment/%s";

    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate;
    private final CustomerMapper customerMapper;

    @Override
    public Customer create(CustomerCommand customerCommand) {
        customerCommand.validate();
        log.info("[+] Begin creating customer with payload {}", JSONUtil.toJSON(customerCommand));
        final Customer customer = customerRepository.save(Customer.create(customerCommand));
        log.info("[+] Customer with id {} created successfully", JSONUtil.toJSON(customer.getId()));
        return customer;
    }
    @Override
    public Page<Customer> findAllByDeletedFalse(Pageable pageable) {
        return customerRepository.findCustomersByDeletedFalse(pageable);
    }
    @Override
    public String sendRating(RatingCommand ratingCommand) {
        findById(ratingCommand.getCustomerId());
        restTemplate.postForEntity(
            RATING_SERVICE_URI,
            ratingCommand,
            RatingCommand.class
        );
        return "[+] Message Sent successfully !!";
    }
    @Override
    public Customer findById(String customerId) {
        log.info("[+] Begin fetching customer by id {}", customerId);
        final Customer customer = customerRepository.findById(parseCustomerId(customerId)).orElseThrow(
                () -> new BusinessException(ExceptionPayloadFactory.CUSTOMER_NOT_FOUND.get())
        );
        log.info("[+] Customer with id {} fetched successfully", customer.getId());
        return customer;
    }

    @Override
    public Set<Driver> getDriversAvailable() {
        final ResponseEntity<Set<Driver>> objects = restTemplate.exchange(
                DRIVER_SERVICE_AVAILABLE_URI, HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });
        log.info("[+] Drivers with payload {}", JSONUtil.toJSON(objects.getBody()));
        return objects.getBody();
    }
    @Override
    public void sendRequestDriver(CustomerRequestDriver requestDriver){
        getDriversAvailable().stream().filter(
                        dv -> dv.getId().equals(requestDriver.getDriverId()))
                .findAny().orElseThrow(
                        () -> new BusinessException(ExceptionPayloadFactory.DRIVER_LOCATION_NOT_FOUND.get())
                );
        log.info("[+] Begin sending message with payload {}", JSONUtil.toJSON(requestDriver));
        log.info("[+] Message with payload {} send Good :)", JSONUtil.toJSON(requestDriver));
    }
    @Override
    public CustomerDetails findCustomerDetailsById(String customerId) {
        final Customer customer = findById(customerId);
        final String driverId = requireIdentifier(customer.getDriverId(), ExceptionPayloadFactory.DRIVER_NOT_FOUND);
        final DriverLocationDto driverResponse = Objects.requireNonNull(getResource(
                String.format(DRIVER_LOCATION_DETAILS_URI, driverId),
                DriverLocationDto.class,
                ExceptionPayloadFactory.DRIVER_LOCATION_NOT_FOUND
        ), "Driver location payload must not be null");
        final BankAccount bankAccountResponse = Objects.requireNonNull(getResource(
                String.format(PAYMENT_ACCOUNT_DETAILS_URI, customer.getId()),
                BankAccount.class,
                ExceptionPayloadFactory.BANK_ACCOUNT_NOT_FOUND
        ), "Bank account payload must not be null");
        final WalletDetails walletDetailsResponse = Objects.requireNonNull(getResource(
                String.format(WALLET_DETAILS_BY_ACCOUNT_URI, bankAccountResponse.getId()),
                WalletDetails.class,
                ExceptionPayloadFactory.WALLET_NOT_FOUND
        ), "Wallet payload must not be null");
        return new CustomerDetails(
                customerMapper.toDto(customer),
                driverResponse,
                bankAccountResponse,
                walletDetailsResponse);
    }
    private  <T> T getResource(String url, Class<T> eClass, ExceptionPayloadFactory payloadFactory){
        try {
            return restTemplate.getForObject(url, eClass);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new BusinessException(payloadFactory.get());
            }
            throw exception;
        }
    }

    private String requireIdentifier(String value, ExceptionPayloadFactory payloadFactory) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(payloadFactory.get());
        }
        return value;
    }

    private UUID parseCustomerId(String rawId) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ExceptionPayloadFactory.CUSTOMER_NOT_FOUND.get());
        }
    }

    @Override
    public Page<Customer> getAllByCriteria(Pageable pageable, CustomerCriteria customerCriteria) {
        return customerRepository.findAllByCriteria(pageable, customerCriteria);
    }

    @Override
    public void updateInfo(CustomerInfoUpdateCmd customerCommand, String customerId) {
        customerCommand.validate();
        final Customer customer = findById(customerId);
        customer.updateInfo(customerCommand);
        customerRepository.save(customer);
    }
    public List<Customer> findAll(){
        return customerRepository.findAll();
    }
}
