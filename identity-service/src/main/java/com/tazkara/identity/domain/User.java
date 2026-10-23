package com.tazkara.identity.domain;

import com.tazkara.common.error.DomainException;
import com.tazkara.common.security.Role;
import com.tazkara.identity.exception.IdentityErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {

    @Id
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Embedded
    private EmailAddress email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "organization_name", length = 150)
    private String organizationName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Getter(AccessLevel.NONE)
    private Long version;

    User(Long id, String fullName, EmailAddress email, String passwordHash, Role role,
         String organizationName, Instant now) {
        this.id = id;
        this.fullName = fullName.trim();
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.organizationName = normalizeOrganization(role, organizationName);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String newFullName, String newOrganizationName, Instant now) {
        this.fullName = newFullName.trim();
        if (role == Role.ORGANIZER) {
            this.organizationName = normalizeOrganization(role, newOrganizationName);
        }
        this.updatedAt = now;
    }

    private static String normalizeOrganization(Role role, String organizationName) {
        if (role != Role.ORGANIZER) {
            return null;
        }
        if (organizationName == null || organizationName.isBlank()) {
            throw new DomainException(IdentityErrorCode.ORGANIZATION_NAME_REQUIRED,
                    "Organization name is required for organizers");
        }
        return organizationName.trim();
    }
}
