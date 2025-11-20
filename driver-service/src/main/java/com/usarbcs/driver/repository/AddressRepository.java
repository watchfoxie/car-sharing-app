package com.usarbcs.driver.repository;

import com.usarbcs.driver.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
}
