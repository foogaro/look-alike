package com.foogaro.repositories;

import com.foogaro.dtos.CapturedImageData;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface CapturedImageDataRepository extends RedisDocumentRepository<CapturedImageData, String> {
}
