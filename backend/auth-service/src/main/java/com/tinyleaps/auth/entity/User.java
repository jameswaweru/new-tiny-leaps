package com.tinyleaps.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    private String email;

    private String password;

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider;

    @Column(name = "provider_subject")
    private String providerSubject;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    protected User() {}

    public static User newGuest() {
        User u = new User();
        u.id = UUID.randomUUID();
        u.accountType = AccountType.GUEST;
        u.createdAt = Instant.now();
        u.lastActiveAt = u.createdAt;
        return u;
    }

    public static User newRegistered(AuthProvider provider, String subject, String email, String displayName) {
        User u = newGuest();
        u.upgrade(provider, subject, email, displayName);
        return u;
    }

    public void upgrade(AuthProvider provider, String subject, String email, String displayName) {
        this.accountType = AccountType.REGISTERED;
        this.authProvider = provider;
        this.providerSubject = subject;
        if (email != null) this.email = email;
        if (displayName != null && !displayName.isBlank()) this.displayName = displayName;
    }

    public void touch() { this.lastActiveAt = Instant.now(); }
    public boolean isActive() { return "ACTIVE".equals(status); }
    public boolean isGuest() { return accountType == AccountType.GUEST; }

    public UUID getId() { return id; }
    public AccountType getAccountType() { return accountType; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastActiveAt() { return lastActiveAt; }
}
