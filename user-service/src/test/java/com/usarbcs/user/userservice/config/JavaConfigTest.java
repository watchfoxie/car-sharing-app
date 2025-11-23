package com.usarbcs.user.userservice.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class JavaConfigTest {

    private final JavaConfig javaConfig = new JavaConfig();
    private final PasswordEncoder passwordEncoder = javaConfig.passwordEncoder();

    @Test
    void exposesDefaultAdminCredentials() {
        UserDetailsService userDetailsService = javaConfig.userDetailsService(
                passwordEncoder,
                new AdminSecurityProperties("custom-admin", "custom-pass"));

        UserDetails defaultAdmin = userDetailsService.loadUserByUsername("csadmin");
        assertThat(passwordEncoder.matches("csadmin123", defaultAdmin.getPassword())).isTrue();
    }

    @Test
    void keepsConfiguredAdminWhenDifferentFromDefault() {
        UserDetailsService userDetailsService = javaConfig.userDetailsService(
                passwordEncoder,
                new AdminSecurityProperties("custom-admin", "custom-pass"));

        UserDetails customAdmin = userDetailsService.loadUserByUsername("custom-admin");
        assertThat(passwordEncoder.matches("custom-pass", customAdmin.getPassword())).isTrue();
    }

    @Test
    void avoidsDuplicatingDefaultWhenPropertiesMatch() {
        UserDetailsService userDetailsService = javaConfig.userDetailsService(
                passwordEncoder,
                new AdminSecurityProperties("csadmin", "new-pass"));

        UserDetails defaultAdmin = userDetailsService.loadUserByUsername("csadmin");
        assertThat(passwordEncoder.matches("csadmin123", defaultAdmin.getPassword())).isTrue();
    }
}
