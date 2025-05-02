package com.growith.tailo.security.jwt.service;

import com.growith.tailo.common.exception.ResourceNotFoundException;
import com.growith.tailo.member.entity.Member;
import com.growith.tailo.member.repository.MemberRepository;
import com.growith.tailo.security.jwt.component.JwtUtil;
import com.growith.tailo.security.jwt.entity.RefreshToken;
import com.growith.tailo.security.jwt.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NewTokenServiceTest {

    @InjectMocks
    private NewTokenService newTokenService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Test
    void whenMemberNotFound_thenThrowException() {
        // given
        given(memberRepository.findByAccountId("unknown")).willReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () ->
                newTokenService.createNewAccess("unknown", request));
    }

    @Test
    void whenRefreshTokenNotFound_thenThrowException() {
        // given
        Member member = Member.builder().accountId("testId").build();
        given(memberRepository.findByAccountId("testId")).willReturn(Optional.of(member));
        given(refreshTokenRepository.findByAccountId("testId")).willReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () ->
                newTokenService.createNewAccess("testId", request));
    }

    @Test
    void whenRefreshTokenInvalid_thenThrowUnauthorized() {
        // given
        Member member = Member.builder().accountId("testId").build();
        RefreshToken token = RefreshToken.builder().accountId("testId").token("invalidToken").build();
        given(memberRepository.findByAccountId("testId")).willReturn(Optional.of(member));
        given(refreshTokenRepository.findByAccountId("testId")).willReturn(Optional.of(token));
        given(jwtUtil.validateRefreshToken("invalidToken")).willReturn(false);

        // when & then
        assertThrows(ResponseStatusException.class, () ->
                newTokenService.createNewAccess("testId", request));
    }

    @Test
    void whenValidRequest_thenReturnAccessToken() {
        // given
        Member member = Member.builder().accountId("testId").build();
        RefreshToken token = RefreshToken.builder().accountId("testId").token("validToken").build();
        given(memberRepository.findByAccountId("testId")).willReturn(Optional.of(member));
        given(refreshTokenRepository.findByAccountId("testId")).willReturn(Optional.of(token));
        given(jwtUtil.validateRefreshToken("validToken")).willReturn(true);
        given(jwtUtil.generateAccessToken(member)).willReturn("newAccessToken");

        // when
        String result = newTokenService.createNewAccess("testId", request);

        // then
        assertEquals("newAccessToken", result);
    }
}
