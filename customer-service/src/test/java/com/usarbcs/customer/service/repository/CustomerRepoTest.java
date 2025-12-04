package com.usarbcs.customer.service.repository;


import com.usarbcs.customer.service.command.CustomerCommand;
import com.usarbcs.customer.service.model.Customer;
import com.usarbcs.customer.service.repository.CustomerRepository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;



@Slf4j
@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class CustomerRepoTest {

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    public void setUp() {
        final CustomerCommand customerCommand = new CustomerCommand();
        customerCommand.setFirstName("john");
        customerCommand.setLastName("doe");
        customerCommand.setPassword("csadmin123");
        customerCommand.setEmail("john.doe@gmail.com");

        final CustomerCommand customerCommand1 = new CustomerCommand();
        customerCommand1.setFirstName("CarSharing");
        customerCommand1.setLastName("Soft");
        customerCommand1.setEmail("carsharing.soft@gmail.com");

        customerRepository.save(Customer.create(customerCommand));
        customerRepository.save(Customer.create(customerCommand1));
    }

    @Test
    public void should_i_get_all_customers(){
        /*List<Customer> customers = customerRepository.findAll();
        Assertions.assertThat(customers.size()).isEqualTo(2);
        Assertions.assertThat(customers.get(0).getFirstName()).isEqualTo("john");
        Assertions.assertThat(customers.get(0).getLastName()).isEqualTo("doe");*/
    }
}
