package com.tazkara.identity.repository;

import com.tazkara.identity.domain.EmailAddress;
import com.tazkara.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);
}
