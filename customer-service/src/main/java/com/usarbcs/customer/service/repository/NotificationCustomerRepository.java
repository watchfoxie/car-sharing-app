package com.usarbcs.customer.service.repository;

import com.usarbcs.customer.service.model.NotificationCustomer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface NotificationCustomerRepository extends JpaRepository<NotificationCustomer, UUID > {

    Page<NotificationCustomer> findNotificationCustomersByCustomer_Id(UUID customerId, Pageable pageable);
}
