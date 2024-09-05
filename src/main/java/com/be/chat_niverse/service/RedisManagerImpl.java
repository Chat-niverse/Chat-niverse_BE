package com.be.chat_niverse.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class RedisManagerImpl implements RedisManager {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 단순 값을 저장하는 메서드
    @Override
    public void setValues(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 값을 가져오는 메서드
    @Override
    public String getValue(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

//    // TTL(유효 기간)을 설정하여 값을 저장하는 메서드
//    @Override
//    public void setValuesWithDuration(String key, String value, long duration, TimeUnit unit) {
//        redisTemplate.opsForValue().set(key, value, duration, unit);
//    }

    // 키-값 삭제
    @Override
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }

    // String, String 값을 저장하는 메서드

    // 세계관 :world
    // 캐릭터 설정 :charsetting
    // 최종 목표 :aim
    // 플레이 로그 :playlog
    // 플레이 요약(엔딩) :ending
    // 오버 데이터 :gameover
    // GPT한줄평 :gpteval
    public void setTextKeyTextValue(String username, String value, String innerKey) {
        String key = username + innerKey;
        redisTemplate.opsForValue().set(key, value);
    }

    // String : String 값을 가져오는 메서드
    public String getTextKeyTextValue(String username, String innerKey) {
        String key = username + innerKey;
        return (String) redisTemplate.opsForValue().get(key);
    }

    /*
    Map<String, Integer> playerStatus = new HashMap<>();
    playerStatus.put("strength", 10);
    playerStatus.put("perception", 7);
    playerStatus.put("endurance", 5);
    playerStatus.put("charisma", 8);
    playerStatus.put("intelligence", 9);
    playerStatus.put("luck", 6);

    setPlayerStatus("player1", playerStatus);
    저장 방법
     */

    public void setPlayerStatus(String username, Map<String, Integer> status) {
        String key = username + ":status";

        // 스탯이 0 미만으로 떨어지지 않도록 제약을 설정
        status.forEach((stat, value) -> {
            if (value < 0) {
                throw new IllegalArgumentException(stat + " cannot be less than 0");
            }
        });

        // Redis 해시에 스테이터스 값을 저장
        redisTemplate.opsForHash().putAll(key, status);
    }

    /*
    사용 방법
    Map<String, Integer> playerStatus = getPlayerStatus("player1");
    playerStatus.forEach((stat, value) -> {
        System.out.println(stat + ": " + value);
    });
     */
    // 플레이어 스테이터스 조회
    public Map<String, Integer> getPlayerStatus(String username) {
        String key = username + ":status";
        Map<Object, Object> rawStatus = redisTemplate.opsForHash().entries(key);

        // HashMap으로 변환하여 반환
        Map<String, Integer> status = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawStatus.entrySet()) {
            status.put((String) entry.getKey(), (Integer) entry.getValue());
        }

        return status;
    }

    /*
    사용 방법
    Integer strength = getPlayerStrength("player1");
    System.out.println("Strength: " + strength);
     */
    // 플레이어 근력 조회
    public Integer getPlayerStrength(String username) {
        String key = username + ":status";
        return (Integer) redisTemplate.opsForHash().get(key, "strength");
    }

    // 인벤토리에 새 아이템 추가
    public void addItemToInventory(String username, String item, int quantity) {
        String key = username + ":inventory";
        redisTemplate.opsForHash().put(key, item, quantity);
    }

    // 특정 아이템의 수량 가져오기
    public Integer getItemQuantityFromInventory(String username, String item) {
        String key = username + ":inventory";
        return (Integer) redisTemplate.opsForHash().get(key, item);
    }


    // 전체 인벤토리 조회
    /*
    Map<String, Integer> inventory = getInventory("player1");
    inventory.forEach((item, quantity) -> {
        System.out.println(item + ": " + quantity);
    });

     */
    public Map<String, Integer> getInventory(String username) {
        String key = username + ":inventory";
        Map<Object, Object> rawInventory = redisTemplate.opsForHash().entries(key);

        // HashMap으로 변환하여 반환
        Map<String, Integer> inventory = new HashMap<>();

        for (Map.Entry<Object, Object> entry : rawInventory.entrySet()) {
            String item = (String) entry.getKey();
            Integer quantity = (Integer) entry.getValue();

            // 수량이 0인 아이템은 삭제
            if (quantity == 0) {
                redisTemplate.opsForHash().delete(key, item);
            } else {
                inventory.put(item, quantity);  // 0이 아닌 아이템만 인벤토리에 추가
            }
        }

        return inventory;
    }

    // 인벤토리 전체 무게 조회
    public int getInventorySum(String username) {
        String key = username + ":inventory";
        Map<Object, Object> rawInventory = redisTemplate.opsForHash().entries(key);

        int inventorySum = 0;

        for (Map.Entry<Object, Object> entry : rawInventory.entrySet()) {
            inventorySum += (Integer) entry.getValue();
        }

        return inventorySum;
    }

    // 플레이어 라이프 관리
    public void setPlayerLife(String username, int life) {
        String key = username+":life";
        if (life >= 0 && life <= 3) {
            redisTemplate.opsForValue().set(key, life);
        } else {
            throw new IllegalArgumentException("Life value must be between 0 and 3.");
        }
    }

    // 플레이어 라이프 조회
    public Object getPlayerLife(String username){
        String key = username+":life";
        return redisTemplate.opsForValue().get(key);
    }

    // 플레이어의 인벤토리 아이템 추가/업데이트
    public void setItemToInventory(String username, String item, int quantity) {
        String key = username + ":inventory";
        redisTemplate.opsForHash().put(key, item, quantity);
    }

    // 인벤토리 아이템 개수 조회
    public Object getItemFromInventory(String username, String item) {
        String key = username + ":inventory";
        return redisTemplate.opsForHash().get(key, item);
    }
}

