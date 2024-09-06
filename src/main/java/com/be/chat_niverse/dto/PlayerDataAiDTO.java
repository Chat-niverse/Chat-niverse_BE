package com.be.chat_niverse.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

// AI가 요청하고 응답하는 DTO는
// username, worldview, charsetting, aim,
// status, life, inventory, playlog, dice, selectedchoice, gptsays
@Getter
@Builder
public class PlayerDataAiDTO {
    private String username;
    private String worldview;
    private String charsetting;
    private String aim;

    private Map<String, Object> status;
    private int life;
    private Map<String, Object> inventory;
    private String playlog;

    private int dice;
    private String selectedchoice;
    private String gptsays;
}
