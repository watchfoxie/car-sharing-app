package com.usarbcs.user.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

class JavaConfigTest {

    private final JavaConfig javaConfig = new JavaConfig();
    private final PasswordEncoder passwordEncoder = javaConfig.passwordEncoder();

    @Test
    void exposesDefaultAdminCredentials() {
        ReactiveUserDetailsService userDetailsService = javaConfig.reactiveUserDetailsService(
                passwordEncoder,
                new AdminSecurityProperties("custom-admin", "custom-pass"));

        UserDetails defaultAdmin = userDetailsService.findByUsername("csadmin").block();
        assertThat(passwordEncoder.matches("csadmin123", defaultAdmin.getPassword())).isTrue();
    }

    @Test
    void keepsConfiguredAdminWhenDifferentFromDefault() {
        ReactiveUserDetailsService userDetailsService = javaConfig.reactiveUserDetailsService(
                passwordEncoder,
                new AdminSecurityProperties("custom-admin", "custom-pass"));

        UserDetails customAdmin = userDetailsService.findByUsername("custom-admin").block();
        assertThat(passwordEncoder.matches("custom-pass", customAdmin.getPassword())).isTrue();
    }

    @Test
    void avoidsDuplicatingDefaultWhenPropertiesMatch() {
        ReactiveUserDetailsService userDetailsService = javaConfig.reactiveUserDetailsService(
                passwordEncoder,
                new AdminSecurityProperties("csadmin", "new-pass"));

        UserDetails defaultAdmin = userDetailsService.findByUsername("csadmin").block();
        assertThat(passwordEncoder.matches("csadmin123", defaultAdmin.getPassword())).isTrue();
    }
}
