// PGH
package com.aio.hospitalsafety.config;

import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import com.aio.hospitalsafety.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            UserService userService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/domain",
                                "/role",
                                "/login",
                                "/login/user",
                                "/signup",
                                "/api/users/check-user-id",
                                "/css/**",
                                "/JS/**",
                                "/image/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(
                                "/admin/**",
                                "/api/admin/**"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login/user")
                        .usernameParameter("userLoginKey")
                        .successHandler((request, response, authentication) -> {
                            String displayName =
                                    authentication.getPrincipal()
                                            instanceof HospitalUserDetails userDetails
                                            ? userDetails.getUserName()
                                            : authentication.getName();

                            displayName =
                                    displayName == null || displayName.isBlank()
                                            ? authentication.getName()
                                            : displayName.strip();

                            request.getSession().setAttribute(
                                    UserDisplaySession.DISPLAY_NAME,
                                    displayName.substring(
                                            0,
                                            Math.min(displayName.length(), 50)
                                    )
                            );

                            request.getSession().setAttribute(
                                    UserDisplaySession.LOGIN_TIME,
                                    java.time.ZonedDateTime.now(
                                            java.time.ZoneId.of("Asia/Seoul")
                                    ).format(
                                            java.time.format.DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd HH:mm:ss"
                                            )
                                    )
                            );

                            boolean initialUserPassword =
                                    authentication.getPrincipal()
                                            instanceof HospitalUserDetails loginUser
                                    && authentication.getAuthorities()
                                            .stream()
                                            .anyMatch(authority ->
                                                    "ROLE_USER".equals(
                                                            authority.getAuthority()
                                                    ))
                                    && userService.isInitialUserPassword(
                                            loginUser.getHospitalId(),
                                            loginUser.getUsername()
                                    );

                            response.sendRedirect(
                                    request.getContextPath()
                                            + (initialUserPassword
                                            ? "/user/password"
                                            : "/dashboard")
                            );
                        })
                        .failureHandler((request, response, exception) -> {
                            String error = switch (exception) {
                                case UsernameNotFoundException ignored ->
                                        "userId";
                                case BadCredentialsException ignored ->
                                        "password";
                                case DisabledException ignored ->
                                        "disabled";
                                default ->
                                        "unavailable";
                            };

                            new SimpleUrlAuthenticationFailureHandler(
                                    "/login?error=" + error
                            ).onAuthenticationFailure(
                                    request,
                                    response,
                                    exception
                            );
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .expiredUrl("/")
                        .sessionRegistry(sessionRegistry)
                );

        http.addFilterAfter(
                new UserInitialPasswordFilter(userService),
                org.springframework.security.web.access.intercept.AuthorizationFilter.class
        );

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(UserMapper userMapper) {
        return userLoginKey -> {
            String[] parts = userLoginKey.split("\\|", 2);

            if (parts.length != 2
                    || parts[0].isBlank()
                    || parts[1].isBlank()) {
                throw new UsernameNotFoundException(
                        "로그인 형식이 올바르지 않습니다."
                );
            }

            String hospitalId = parts[0].trim();
            String userId = parts[1].trim();

            if (hospitalId.length() > 30
                    || userId.length() > 20) {
                throw new UsernameNotFoundException(
                        "로그인 입력 길이가 올바르지 않습니다."
                );
            }

            User user = userMapper.findByHospitalIdAndUserId(
                    hospitalId,
                    userId
            ).orElseThrow(() ->
                    new UsernameNotFoundException(
                            "사용자를 찾을 수 없습니다."
                    )
            );

            return new HospitalUserDetails(user);
        };
    }

    @Bean
    DaoAuthenticationProvider userAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);

        return provider;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}