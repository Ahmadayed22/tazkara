package com.tazkara.identity.service;

import com.tazkara.common.error.DomainException;
import com.tazkara.identity.domain.EmailAddress;
import com.tazkara.identity.domain.User;
import com.tazkara.identity.domain.UserFactory;
import com.tazkara.identity.exception.IdentityErrorCode;
import com.tazkara.identity.repository.UserRepository;
import com.tazkara.identity.dto.LoginRequest;
import com.tazkara.identity.dto.RegisterRequest;
import com.tazkara.identity.dto.TokenResponse;
import com.tazkara.identity.dto.UserProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import  com.tazkara.identity.config.JwtTokenIssuer;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer tokenIssuer;
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, UserFactory userFactory,
                       PasswordEncoder passwordEncoder, JwtTokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.dummyPasswordHash = passwordEncoder.encode("timing-equaliser-not-a-real-password-1");
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        EmailAddress email = EmailAddress.of(request.email());
        if (userRepository.existsByEmail(email)) {
            throw emailTaken();
        }
        User user = userFactory.create(request.fullName(), request.email(), request.password(),
                request.role(), request.organizationName());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Two registrations with the same email at the same moment: the unique constraint decides.
            throw emailTaken();
        }
        log.info("Registered user id={} role={}", user.getId(), user.getRole());
        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(EmailAddress.of(request.email()));
        String hashToCheck = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (user.isEmpty() || !passwordMatches) {
            log.info("Failed login attempt");
            throw new DomainException(IdentityErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS_MESSAGE);
        }
        JwtTokenIssuer.IssuedToken token = tokenIssuer.issue(user.get());
        log.info("User id={} logged in", user.get().getId());
        return new TokenResponse(token.value(), "Bearer", token.expiresAt(), token.expiresInSeconds());
    }

    private static DomainException emailTaken() {
        return new DomainException(IdentityErrorCode.EMAIL_ALREADY_REGISTERED, "An account with this email already exists");
    }
}
