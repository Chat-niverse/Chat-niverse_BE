package com.be.chat_niverse.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class PlayerDataAiDTO {
    private String username;      // 플레이어 닉네임
    private String worldview;     // 플레이어가 설정한 세계관
    private String charsetting;   // 캐릭터 설정
    private String aim;           // 플레이어 목표

    private Map<String, Integer> status;    // 플레이어 현재 status, key는 스탯 이름, value는 값
    private Integer life;         // 플레이어의 life (null 허용)
    private Map<String, Integer> inventory; // 인벤토리 아이템과 수량 (null 허용)
    private String playlog;       // 플레이어의 플레이 로그 (null 허용)

    private Integer dice;         // 주사위 값 (null 허용, 기본형 int 대신 Integer 사용)
    private String selectedchoice; // 선택한 선택지 (null 허용, 숫자형으로 수정)

    private String gptsays;       // GPT가 제공하는 한줄 평가 (null 허용)
    private Map<String, String> choices; // 선택지 목록, 키는 "first", "second" 등 (null 허용)
    private String imageurl; // AI에게 전달할 image URL 필드 추가
}
