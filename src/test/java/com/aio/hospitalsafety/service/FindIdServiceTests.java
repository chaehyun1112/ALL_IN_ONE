// PGH
package com.aio.hospitalsafety.service;

import com.aio.hospitalsafety.mapper.UserMapper;
import com.aio.hospitalsafety.service.FindIdService.FindIdRequestResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindIdServiceTests {

    @Mock
    private UserMapper userMapper;

    @Mock
    private MailService mailService;

    @Test
    void sendsSixDigitCodeAndReturnsUserIdWhenExactlyOneMatch() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(List.of("EMP001"));
        FindIdService service = new FindIdService(userMapper, mailService);

        FindIdRequestResult result = service.requestVerificationCode("HOSP01", "홍길동", "hong@example.com");

        assertThat(result.status()).isEqualTo(FindIdRequestResult.Status.SUCCESS);
        assertThat(result.userId()).isEqualTo("EMP001");
        assertThat(result.code()).matches("^[0-9]{6}$");

        ArgumentCaptor<String> sentCode = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendFindIdVerificationCode(anyString(), sentCode.capture());
        assertThat(sentCode.getValue()).isEqualTo(result.code());
    }

    @Test
    void returnsNotFoundWhenNoAccountMatches() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "없는사람", "nobody@example.com"))
                .thenReturn(List.of());
        FindIdService service = new FindIdService(userMapper, mailService);

        FindIdRequestResult result = service.requestVerificationCode("HOSP01", "없는사람", "nobody@example.com");

        assertThat(result.status()).isEqualTo(FindIdRequestResult.Status.NOT_FOUND);
        assertThat(result.userId()).isNull();
        assertThat(result.code()).isNull();
    }

    @Test
    void returnsNotFoundWhenMultipleAccountsAmbiguouslyMatch() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(List.of("EMP001", "EMP002"));
        FindIdService service = new FindIdService(userMapper, mailService);

        FindIdRequestResult result = service.requestVerificationCode("HOSP01", "홍길동", "hong@example.com");

        assertThat(result.status()).isEqualTo(FindIdRequestResult.Status.NOT_FOUND);
    }

    @Test
    void returnsSendFailedWhenMailSendingThrows() {
        when(userMapper.findMatchingUserIdsForFindId("HOSP01", "홍길동", "hong@example.com"))
                .thenReturn(List.of("EMP001"));
        doThrow(new MailAuthenticationException("smtp auth failed"))
                .when(mailService).sendFindIdVerificationCode(anyString(), anyString());
        FindIdService service = new FindIdService(userMapper, mailService);

        FindIdRequestResult result = service.requestVerificationCode("HOSP01", "홍길동", "hong@example.com");

        assertThat(result.status()).isEqualTo(FindIdRequestResult.Status.SEND_FAILED);
    }
}
