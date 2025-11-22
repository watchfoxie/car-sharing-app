package com.usarbcs.driver.service.driver;


import com.usarbcs.core.details.BankAccount;
import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.util.JSONUtil;
import com.usarbcs.driver.command.DriverCommand;
import com.usarbcs.driver.command.RatingCommand;
import com.usarbcs.driver.criteria.DriverCriteria;
import com.usarbcs.driver.model.Driver;
import com.usarbcs.driver.payload.DriverDetails;
import com.usarbcs.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService{

    private static final String RATING_SERVICE_URI = "http://rating-service:8086/rating-service/v1/ratings";
    private static final String DRIVER_LOCATION_BASE_URI = "http://driver-location-service:8083/v1/driver-location";
    private static final String DRIVER_LOCATION_DETAILS_URI = DRIVER_LOCATION_BASE_URI + "/driver-location-details/%s";
    private static final String PAYMENT_ACCOUNT_DETAILS_URI = "http://payment-service:8084/v1/payment/account-details/%s";
    private static final String WALLET_DETAILS_BY_ACCOUNT_URI = "http://wallet-service:8085/v1/wallet/payment/%s";

    private final DriverRepository driverRepository;
    private final RestTemplate restTemplate;


    @Override
    public Driver create(DriverCommand driverCommand) {
        driverCommand.validate();
        log.info("Begin creating driver with payload {}", JSONUtil.toJSON(driverCommand));
        final Driver driver = Driver.create(driverCommand);
        log.info("Driver with id {} created successfully", driver.getId());
        driverRepository.save(driver);
        final String uri = DRIVER_LOCATION_BASE_URI + "/" + driver.getId();
        log.info("[+] URI => {}", uri);
        restTemplate.getForObject(
                uri,
                String.class,
                driver.getId());
        return driver;
    }
    private <T> ResponseEntity<T> getEntity(String url, Class<T> eClass){
        return restTemplate.getForEntity(url, eClass);
    }
    @Override
    public DriverDetails getDriverDetailsByDriverId(String driverId) {
        final Driver driver = findById(driverId);

        final ResponseEntity<DriverLocationDto> driverLocationDtoResponseEntity = getEntity(
                String.format(DRIVER_LOCATION_DETAILS_URI, driverId),
                DriverLocationDto.class
        );
        var driverResponse = driverLocationDtoResponseEntity.getBody();
        if (driverResponse == null) {
            throw new BusinessException(ExceptionPayloadFactory.DRIVER_LOCATION_NOT_FOUND.get());
        }
        final ResponseEntity<BankAccount> bankAccountResponseEntity = getEntity(
                String.format(PAYMENT_ACCOUNT_DETAILS_URI, driverId),
                BankAccount.class
        );
        var bankAccountResponse = bankAccountResponseEntity.getBody();
        if (bankAccountResponse == null) {
            throw new BusinessException(ExceptionPayloadFactory.BANK_ACCOUNT_NOT_FOUND.get());
        }
        final ResponseEntity<WalletDetails> walletDetailsResponseEntity = getEntity(
                String.format(WALLET_DETAILS_BY_ACCOUNT_URI, bankAccountResponse.getId()),
                WalletDetails.class
        );
        var walletDetailsResponse = walletDetailsResponseEntity.getBody();
        if (walletDetailsResponse == null) {
            throw new BusinessException(ExceptionPayloadFactory.WALLET_NOT_FOUND.get());
        }
        return new DriverDetails(
                driverId,
                driver.getFirstName(),
                driver.getLastName(),
                driverResponse,
                bankAccountResponse,
            walletDetailsResponse
        );
    }
    @Override
    public void deleteAccount(String driverId) {
        log.info("[+] Begin removing account with id {}", driverId);
        final Driver driver = findById(driverId);
        driverRepository.delete(driver);
        restTemplate.delete(DRIVER_LOCATION_BASE_URI + "/" + driverId, driverId);
    }
    @Override
    public Page<Driver> findAllByCriteria(Pageable pageable, DriverCriteria driverCriteria) {
        return driverRepository.findAllByCriteria(pageable, driverCriteria);
    }

    @Override
    public String sendRating(RatingCommand ratingCommand) {
        final Driver driver = findById(ratingCommand.getDriverId());
        final String customerId = driver.getLastNotification();
        if(ratingCommand.getCustomerId().equals(customerId)) {
                restTemplate.postForEntity(
                    RATING_SERVICE_URI,
                    ratingCommand,
                    RatingCommand.class
                );
            return "Message Sent";
        }
        else{
            return "Message Not Sent";
        }
    }

    @Override
    public void update(String driverId, DriverCommand driverCommand) {
        driverCommand.validate();
        log.info("[+] Begin updating driver with id {}", driverId);
        final Driver driver =findById(driverId);
        log.info("[+] Begin updating driver with payload {}", JSONUtil.toJSON(driverCommand));
        driver.updateInfo(driverCommand);
        log.info("[+] Driver with id {} updated successfully", driver.getId());
        driverRepository.save(driver);
    }
    @Override
    public Set<Driver> getDriversAvailable() {
        return driverRepository.findByDriverStatusStatus("AVAILABLE");
    }
    @Override
    public Driver findById(String driverId){
        log.info("Begin fetching driver with id {}", driverId);
        final Driver driver = driverRepository.findById(driverId).orElseThrow(
                () -> new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get()));
        log.info("Driver with id {} fetched successfully", driverId);
        return driver;
    }
    @Override
    public Page<Driver> getAll(Pageable pageable) {
        return driverRepository.findAllByDeletedFalse(pageable);
    }
}

