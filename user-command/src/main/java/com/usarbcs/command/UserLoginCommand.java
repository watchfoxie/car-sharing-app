package com.usarbcs.command;


import lombok.Getter;

import static com.usarbcs.core.util.Assert.assertRegex;
import static com.usarbcs.core.util.RegexExpressions.EMAIL;

@Getter
public class UserLoginCommand {
    private String email;
    private String password;


    public void validate(){
        assertRegex(email, EMAIL);
    }
}
