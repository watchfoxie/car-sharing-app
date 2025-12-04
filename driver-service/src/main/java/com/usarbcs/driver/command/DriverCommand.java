package com.usarbcs.driver.command;


import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import static com.usarbcs.core.util.Assert.assertNotNull;
import static com.usarbcs.core.util.Assert.assertRegex;
import static com.usarbcs.core.util.RegexExpressions.ALPHABETIC_MIN_2_CHARS;

@Getter
@Setter
@NoArgsConstructor
public class DriverCommand {

    private String firstName;
    private String lastName;
    private List<AddressCommand> addressCommands;

    public void validate(){
        assertRegex(firstName, ALPHABETIC_MIN_2_CHARS);
        assertRegex(lastName, ALPHABETIC_MIN_2_CHARS);
        assertNotNull(addressCommands, ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        if(addressCommands.isEmpty()){
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
        addressCommands.forEach(AddressCommand::validate);
    }
}
