package com.aryan.project7.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;

@Data
@Builder
@RedisHash("RefreshToken") // This tells Spring to store it in Redis
public class RefreshTokenRedis implements Serializable {
    @Id
    private String tokenHash; // SHA-256 hash of the token
    private String userId;
    private long expiryDate;

    @TimeToLive // Spring Data Redis uses this to automatically expire the key
    private Long ttl;
}