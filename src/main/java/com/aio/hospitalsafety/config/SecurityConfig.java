// PGH
package com.aio.hospitalsafety.config;

import com.aio.hospitalsafety.domain.User;
import com.aio.hospitalsafety.mapper.UserMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * 애플리케이션 전체의 로그인, 로그아웃, URL 접근 권한, 비밀번호 암호화 방식을 설정한다.
 *
 * 로그인 요청 처리 흐름
 * 1. login.html에서 병원 ID를 확인하고 user-login.html로 이동한다.
 * 2. user-login.html이 병원 ID와 직원 ID를 합친 userLoginKey, password를 전송한다.
 * 3. Spring Security가 userDetailsService()를 호출해 병원 ID와 직원 ID로 TB_EMP를 조회한다.
 * 4. Spring Security가 입력 PW와 DB의 BCrypt 해시를 passwordEncoder()로 비교한다.
 * 5. 성공하면 인증 정보를 HTTP Session에 저장하고 /dashboard로 이동한다.
 *
 * @Configuration이 붙은 클래스는 Spring 설정 클래스로 인식된다.
 * 이 클래스 안에서 @Bean으로 반환한 객체들은 Spring 컨테이너가 생성하고 관리한다.
 */
@Configuration
public class SecurityConfig {

    /**
     * Spring Security의 웹 보안 규칙을 설정한다.
     *
     * SecurityFilterChain은 브라우저 요청이 Controller에 도착하기 전에 실행되는 필터 모음이다.
     * 따라서 로그인 여부 확인, 로그인 실패 처리, 로그아웃 처리를 Controller마다
     * 직접 작성하지 않아도 된다.
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        // HttpSecurity는 메서드를 이어서 호출하는 DSL 방식으로 보안 설정을 작성한다.
        // CSRF 설정을 끄지 않았으므로 Spring Security의 CSRF 보호가 기본으로 적용된다.
        http
                // authorizeHttpRequests: URL별 접근 권한을 설정한다.
                .authorizeHttpRequests(auth -> auth
                        // 로그인 화면, 재설정 화면, 정적 파일은 로그인하지 않아도 접근할 수 있다.
                        .requestMatchers("/", "/domain", "/login", "/login/user", "/signup", "/api/users/check-user-id",
                                "/password/reset", "/css/**", "/JS/**", "/image/**", "/error").permitAll()
                        // authenticated()는 역할과 관계없이 "로그인 완료 여부"만 검사한다.
                        // 위에서 허용하지 않은 나머지 URL은 로그인한 사용자만 접근할 수 있다.
                        // TODO(화면 URL 확정 필요): 관제 URL이 정해지면 해당 URL에는
                        // hasAuthority("STATUS_APPROVED") 조건을 별도로 먼저 추가해야 한다.

                        .requestMatchers("/admin/**", "/api/admin/**")
                        .hasRole("ADMIN")

                        .anyRequest().authenticated())
                // formLogin: 직원 ID/PW를 사용하는 세션 기반 로그인을 설정한다.
                .formLogin(form -> form
                        // GET /login 요청으로 우리가 만든 로그인 화면을 보여준다.
                        .loginPage("/login")
                        // POST /login/user 요청만 Spring Security가 가로채 인증 처리한다.
                        // 같은 URL의 GET 요청은 AuthController가 로그인 화면을 반환한다.
                        .loginProcessingUrl("/login/user")
                        // userLoginKey는 "병원 구분 ID|직원 ID" 형식의 내부 인증용 값이다.
                        .usernameParameter("userLoginKey")
                        // 두 번째 인자 true는 로그인 전에 접근하려던 URL보다 대시보드를 우선한다는 뜻이다.
                        .defaultSuccessUrl("/dashboard", true)
                        // 로그인 실패 시 error 쿼리 파라미터를 붙여 화면에 오류를 표시한다.
                        .failureUrl("/login?error")
                        // 로그인 처리와 관련된 URL은 비로그인 상태에서도 접근 가능해야 한다.
                        .permitAll())
                // 로그아웃 요청 역시 Spring Security가 처리한다.
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        // 로그아웃 후 서버 세션과 브라우저의 세션 쿠키를 모두 제거한다.
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session
                    .maximumSessions(-1)
                    .expiredUrl("/")
                    .sessionRegistry(sessionRegistry));


        // 위에서 작성한 규칙을 실제 SecurityFilterChain 객체로 만들어 Spring Bean으로 반환한다.
        return http.build();
    }

    /**
     * Spring Security가 로그인할 때 사용자 정보를 가져오는 방법을 정의한다.
     * 화면에서 받은 병원 ID와 직원 ID로 DB를 조회한 후, 조회 결과를 Security가 이해하는
     * UserDetails 객체로 변환한다. 이 Bean은 로그인할 때마다 Spring Security가 호출한다.
     *
     * @param userMapper Spring이 자동 주입하는 MyBatis Mapper
     * @return 직원 ID를 받아 UserDetails를 반환하는 조회 함수
     */
    @Bean
    UserDetailsService userDetailsService(UserMapper userMapper) {
        // userLoginKey -> { ... }는 UserDetailsService의 loadUserByUsername 메서드를
        // 람다식으로 구현한 것이다.
        return userLoginKey -> {
            // userLoginKey는 user-login.html이 "병원ID|직원ID"로 조합해 전송한다.
            // TODO(ID 문자 규칙 확정 필요): 두 ID에 구분자 |를 허용하지 않는 규칙을 명세에 추가하거나,
            // 허용해야 한다면 구분자 조합 대신 별도 AuthenticationProvider 방식으로 변경한다.
            // limit=2로 분리하여 직원 ID 안에 추가 문자가 있어도 두 부분까지만 만든다.
            String[] parts = userLoginKey.split("\\|", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new UsernameNotFoundException("로그인 형식이 올바르지 않습니다.");
            }
            String hospitalId = parts[0].trim();
            String userId = parts[1].trim();
            // 브라우저 maxlength는 개발자 도구로 우회할 수 있으므로 서버에서도 DB 길이를 검사한다.
            if (hospitalId.length() > 30 || userId.length() > 20) {
                throw new UsernameNotFoundException("로그인 입력 길이가 올바르지 않습니다.");
            }

            // Optional에 값이 없으면 orElseThrow가 인증 실패용 예외를 발생시킨다.
            User user = userMapper.findByHospitalIdAndUserId(hospitalId, userId)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

            // DB의 passwordHash는 이미 BCrypt로 해시된 값이다.
            // Spring Security가 사용자가 입력한 비밀번호와 이 해시를 안전하게 비교한다.
            return new HospitalUserDetails(user);
        };
    }

    /**
     * 비밀번호를 BCrypt 방식으로 해시하고 검증하는 객체를 Bean으로 등록한다.
     * BCrypt는 같은 비밀번호라도 매번 다른 salt를 사용하므로 결과 문자열이 달라진다.
     * 복호화해서 원문을 찾는 방식이 아니라 matches(입력값, 저장된 해시)로 일치 여부만 확인한다.
     * 생성자 인자를 생략했으므로 Spring Security가 제공하는 기본 strength 값을 사용한다.
     */
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
