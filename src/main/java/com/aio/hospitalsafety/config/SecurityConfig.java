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
                        // 로그인 화면과 정적 파일은 로그인하지 않아도 접근할 수 있다.
                        .requestMatchers( 
                                "/", "/domain", "/role", "/login", "/login/user",
                                "/signup", "/api/users/check-user-id",
                                "/css/**", "/JS/**", "/image/**", "/error"
                        ).permitAll()
                        .requestMatchers(
                                "/admin/**",
                                "/api/admin/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/dashboard/session").hasRole("USER")
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
                                // [2026-09-16 변경] 아이디·비밀번호 중 어느 값이 틀렸는지 노출하지 않고 공통 오류로 안내한다.
                                case UsernameNotFoundException ignored ->
                                        "credentials";
                                case BadCredentialsException ignored ->
                                        "credentials";
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
                        .logoutSuccessHandler((request, response, authentication) -> {
                            var oldSession = request.getSession(false);
                            String hospitalDomain = oldSession == null
                                    ? null
                                    : (String) oldSession.getAttribute(
                                            com.aio.hospitalsafety.common.SessionConstants.HOSPITAL_DOMAIN
                                    );

                            if (oldSession != null) {
                                oldSession.invalidate();
                            }

                            if (hospitalDomain != null && !hospitalDomain.isBlank()) {
                                request.getSession(true).setAttribute(
                                        com.aio.hospitalsafety.common.SessionConstants.HOSPITAL_DOMAIN,
                                        hospitalDomain
                                );
                                response.sendRedirect(request.getContextPath() + "/login");
                                return;
                            }

                            response.sendRedirect(request.getContextPath() + "/");
                        })
                        .invalidateHttpSession(false)
                        .clearAuthentication(true)
                )
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .expiredUrl("/")
                        .sessionRegistry(sessionRegistry)
                );

        // [2026.09.17] 추가한 내용: 관리자 문서 안에서 같은 사이트의 조치기록 화면을 표시해도 전체화면이 유지되게 합니다.
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

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
    DaoAuthenticationProvider userAuthenticationProvider(UserDetailsService userDetailsService,
                                                        PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        // 아이디 조회 실패와 해당 계정의 비밀번호 불일치를 구분한다.
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    /** 비밀번호를 BCrypt로 해시하고 입력값과 저장된 해시의 일치 여부를 검증한다. */
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
