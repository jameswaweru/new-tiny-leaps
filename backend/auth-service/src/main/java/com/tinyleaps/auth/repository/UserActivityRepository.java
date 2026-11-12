package com.tinyleaps.auth.repository;

import com.tinyleaps.auth.entity.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserActivityRepository extends JpaRepository<UserActivity, UUID> {
    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
