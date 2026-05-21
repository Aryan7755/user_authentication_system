package com.aryan.project7.config;

import com.aryan.project7.repository.RefreshTokenRepo;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TokenCleanupScheduler {
    private final RefreshTokenRepo refreshTokenRepo;

    @Scheduled(cron = "0 0 2 * * ?")  // 2 AM daily
    public void cleanup() {
        refreshTokenRepo.cleanupExpiredTokens();
    }
}