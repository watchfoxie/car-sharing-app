package com.usarbcs.driver.repository;

import com.usarbcs.driver.model.Driver;
import com.usarbcs.driver.model.NotificationDriver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface NotificationDriverRepository extends JpaRepository<NotificationDriver, String> {

    List<NotificationDriver> findAllByDriverId(String driverId);
    Page<NotificationDriver> findAllByDriver(Pageable pageable, Driver driver);
    NotificationDriver findByCustomerIdAndDriver(String customerId, Driver driver);
}
