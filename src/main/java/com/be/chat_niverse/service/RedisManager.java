package com.be.chat_niverse.service;

import org.springframework.stereotype.Service;

// Redis에 직접 접근하는 기능을 관리하는 인터페이스
@Service
public interface RedisManager {
    void setValues(String key, String value);

//    //구현체에서 Long, TimeUnit 상수로 duration 관리하기
//    void setValuesWithDuration(String key, String value);

    String getValue(String key);

    void deleteValue(String key);
}