package com.tinyleaps.auth.repository;

import com.tinyleaps.auth.entity.AuthProvider;
import com.tinyleaps.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuthProviderAndProviderSubject(AuthProvider provider, String providerSubject);
    Optional<User> findByEmailIgnoreCase(String email);
}
