package com.usarbcs.driver.service.notification;


import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.driver.command.AcceptRequestCustomer;
import com.usarbcs.core.util.Assert;
import com.usarbcs.driver.command.CustomerRequestDriver;
import com.usarbcs.driver.model.Driver;
import com.usarbcs.driver.model.NotificationDriver;
import com.usarbcs.driver.repository.DriverRepository;
import com.usarbcs.driver.repository.NotificationDriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService{


    private final NotificationDriverRepository notificationDriverRepository;
    private final DriverRepository driverRepository;


    @Override
    public List<NotificationDriver> getNotificationsByDriverId(String driverId) {
        log.info("[+] Begin fetching driver with id {}", driverId);
        final Driver driver = fetchDriver(driverId);
        return notificationDriverRepository.findAllByDriverId(driver.getId());
    }

    @Override
    public Driver acceptRequest(final AcceptRequestCustomer acceptRequestCustomer) {
        validatePayload(acceptRequestCustomer);
        final Driver driver = fetchDriver(acceptRequestCustomer.getDriverId());
        var existingNotification = notificationDriverRepository.findByCustomerIdAndDriver(acceptRequestCustomer.getCustomerId(), driver);
        if(existingNotification == null){
            final CustomerRequestDriver requestDriver = CustomerRequestDriver.create(
                    acceptRequestCustomer.getCustomerId(),
                    acceptRequestCustomer.getDriverId()
            );
            existingNotification = NotificationDriver.create(requestDriver);
            existingNotification.linkToDriver(driver);
            notificationDriverRepository.save(existingNotification);
            driver.addToDriver(existingNotification);
        }
        driverRepository.save(driver);
        log.info("[+] Driver with id {} accepted request for customer {}", acceptRequestCustomer.getDriverId(), acceptRequestCustomer.getCustomerId());
        return driver;
    }
    @Override
    public Driver cancelRequest(final AcceptRequestCustomer acceptRequestCustomer){
        validatePayload(acceptRequestCustomer);
        final Driver driver = fetchDriver(acceptRequestCustomer.getDriverId());
        log.info("[+] Driver with id {} fetched successfully", acceptRequestCustomer.getDriverId());
        final NotificationDriver notificationDriver = notificationDriverRepository.findByCustomerIdAndDriver(acceptRequestCustomer.getCustomerId(), driver);
        if(notificationDriver == null){
            throw new BusinessException(ExceptionPayloadFactory.CUSTOMER_NOT_FOUND.get());
        }
        driver.cancelRequestNotification(notificationDriver);
        notificationDriverRepository.delete(notificationDriver);
        driverRepository.save(driver);
        return driver;
    }
    @Override
    public Page<NotificationDriver> findAllByDriverId(Pageable pageable, String driverId) {
        final Driver driver = fetchDriver(driverId);
        log.info("Driver with id {} fetched successfully", driverId);
        return notificationDriverRepository.findAllByDriver(pageable, driver);
    }
    @Override
    public Page<NotificationDriver> getAll(Pageable pageable) {
        return notificationDriverRepository.findAll(pageable);
    }

    private Driver fetchDriver(String driverId) {
        return driverRepository.findById(parseDriverId(driverId)).orElseThrow(
                () -> new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get()));
    }

    private UUID parseDriverId(String driverId) {
        if (driverId == null) {
            throw new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get());
        }
        try {
            return UUID.fromString(driverId);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ExceptionPayloadFactory.DRIVER_NOT_FOUND.get());
        }
    }

    private void validatePayload(AcceptRequestCustomer acceptRequestCustomer) {
        Assert.assertNotBlank(acceptRequestCustomer.getDriverId());
        Assert.assertNotBlank(acceptRequestCustomer.getCustomerId());
    }
}
