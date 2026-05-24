package com.aryan.project7.repository;

import com.aryan.project7.entity.RefreshTokenRedis;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRedisRepo extends CrudRepository<RefreshTokenRedis, String> {
}
