package com.tinyleaps.auth.service;

import java.time.Instant;

record TokenPair(String accessToken, Instant accessExpiresAt, String refreshToken, Instant refreshExpiresAt) {}
