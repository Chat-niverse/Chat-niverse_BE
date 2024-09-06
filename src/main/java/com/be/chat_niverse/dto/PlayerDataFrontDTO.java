package com.be.chat_niverse.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PlayerDataFrontDTO {
    private int isStart; // 시작인지 판단

    private String username; // 플레이어 닉네임
    private String worldview; // 플레이어가 설정한 세계관
    private String charsetting; // 캐릭터 설정
    private String aim; // 플레이어 목표

    private Map<String, Integer> status; // 플레이어 현재 status
    private int life; // 플레이어 life, 3으로 시작 0이 되면 게임오버
    private Map<String, Integer> inventory; // 플레이어 인벤토리
    private boolean isfull; // 플레이어 가방 가득 찼는지 여부
    private String playlog; // 플레이어 플레이로그
    private String gptsays; // 플레이어가 목표를 달성하면 보여줄 한줄 평

    private String selectedchoice; // 플레이어가 선택한 선택지
    private Map<String, String> choices; // 선택지 목록, key는 "first", "second", "third", "fourth" (null 허용)
    private String imageurl; // 프론트로 보내줄 사진 url
}
