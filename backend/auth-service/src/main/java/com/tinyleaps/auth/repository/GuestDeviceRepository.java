package com.tinyleaps.auth.repository;

import com.tinyleaps.auth.entity.GuestDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GuestDeviceRepository extends JpaRepository<GuestDevice, UUID> {
    Optional<GuestDevice> findByDeviceId(String deviceId);
}
