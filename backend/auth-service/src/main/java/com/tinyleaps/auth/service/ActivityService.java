package com.tinyleaps.auth.service;

import com.tinyleaps.auth.entity.ActivityType;
import com.tinyleaps.auth.entity.UserActivity;
import com.tinyleaps.auth.repository.UserActivityRepository;
import com.tinyleaps.common.logging.TLLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Persists the USER ACTIVITIES audit trail. */
@Service
public class ActivityService {

    private final UserActivityRepository repository;

    public ActivityService(UserActivityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID userId, ActivityType type, String description, String metadata, String deviceId) {
        repository.save(new UserActivity(userId, type, description, metadata, deviceId));
        TLLog.info("ACTIVITY_" + type.name(), description);
    }

    /** Records even if the surrounding transaction is about to fail (e.g. a security event). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordInNewTx(UUID userId, ActivityType type, String description, String metadata, String deviceId) {
        record(userId, type, description, metadata, deviceId);
    }

    @Transactional(readOnly = true)
    public Page<UserActivity> list(UUID userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }
}
