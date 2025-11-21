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
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService{

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
        if(findById(ratingCommand.getDriverId()) == null)
            throw new BusinessException(ExceptionPayloadFactory.DRIVER_LOCATION_NOT_FOUND.get());
        restTemplate.postForEntity(
                "http://localhost:8086/rating-service/v1/ratings", ratingCommand,
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
                "https://carsharing-microservice-driver-service.azuremicroservices.io/v1/drivers", HttpMethod.GET,
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
        final ResponseEntity<DriverLocationDto> driverLocationDtoResponseEntity = getEntity(
                "http://DRIVER-LOCATION:8083/v1/driver-location/driver-location-details/"
                + customer.getId(),
                DriverLocationDto.class
        );
        var driverResponse = driverLocationDtoResponseEntity.getBody();
        final ResponseEntity<BankAccount> bankAccountResponseEntity = getEntity(
                "http://PAYMENT:8084/v1/payment/account-details/"
                    + customer.getId(),
                BankAccount.class
        );
        var bankAccountResponse = bankAccountResponseEntity.getBody();
        final ResponseEntity<WalletDetails> walletDetailsResponseEntity = getEntity(
                "http://WALLET:8085/v1/wallet/payment/" +
                        bankAccountResponse.getId(),
                WalletDetails.class
        );
        var walletDetailsResponse = walletDetailsResponseEntity.getBody();
        return new CustomerDetails(
                customerMapper.toDto(customer),
                driverResponse,
                bankAccountResponse,
                walletDetailsResponse);
    }
    private  <T> ResponseEntity<T> getEntity(String url, Class<T> eClass){
        return restTemplate.getForEntity(url, eClass);
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
