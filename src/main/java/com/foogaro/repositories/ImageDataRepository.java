package com.foogaro.repositories;

import com.foogaro.dtos.ImageData;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface ImageDataRepository extends RedisDocumentRepository<ImageData, String> {
}
