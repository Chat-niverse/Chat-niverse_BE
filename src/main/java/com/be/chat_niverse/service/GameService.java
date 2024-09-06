package com.be.chat_niverse.service;

import com.be.chat_niverse.controller.GameController;
import com.be.chat_niverse.dto.PlayerDataAiDTO;
import com.be.chat_niverse.dto.PlayerDataFrontDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
@Service
@Transactional
public class GameService {

    private final RedisManagerImpl redisManager;
    private static final Logger gameServiceLogger = LoggerFactory.getLogger(GameService.class);

    public GameService(RedisManagerImpl redisManager) {
        this.redisManager = redisManager;
    }

    // 게임 진행을 처리하는 비즈니스 로직
    public PlayerDataFrontDTO processGame(PlayerDataFrontDTO playerDataFrontDTO) {
        String username = playerDataFrontDTO.getUsername();

        // Redis에서 기존 데이터 가져오기
        Map<String, String> status = redisManager.getPlayerStatus(username);
        String life = (String) redisManager.getPlayerLife(username);  // null 가능성을 고려해 Integer로 받음
        Map<String, String> inventory = redisManager.getInventory(username);


        PlayerDataAiDTO playerDataAiDTO;

        // 1이면 시작 username, worldview, charsetting, aim 이 들어감
        if(playerDataFrontDTO.getIsStart() == 1){
            playerDataAiDTO = PlayerDataAiDTO.builder()
                    .username(username)
                    .worldview(playerDataFrontDTO.getWorldview())
                    .charsetting(playerDataFrontDTO.getCharsetting())
                    .aim(playerDataFrontDTO.getAim())
                    .build();
            // AI로 보내기 전에 로그로 aim 값 확인
            gameServiceLogger.info("AI로 보내는 aim 값: {}", playerDataFrontDTO.getAim());
        }else{
            // 0이면 username, status, life, inventory, playlog, dice, selectedchoice
            playerDataAiDTO = PlayerDataAiDTO.builder()
                    .username(username)
                    .status(playerDataFrontDTO.getStatus())
                    .life(life)
                    .inventory(inventory)
                    .playlog(playerDataFrontDTO.getPlaylog())
                    .dice((int) (Math.random() * 20) + 1)
                    .selectedchoice(playerDataFrontDTO.getSelectedchoice())
                    .build();
        }

        // AI 모듈로 데이터 전송 및 응답 처리
        PlayerDataAiDTO aiResponse = sendToAiModule(playerDataAiDTO);
        // 확인함
        //gameServiceLogger.info("목표 : {}", aiResponse.getAim());

        String frontText = null;
        gameServiceLogger.info("피통 : {}", aiResponse.getLife());
        String lifeFromAI = aiResponse.getLife();

        gameServiceLogger.info("gpt 한줄평 : {}", aiResponse.getGptsays());
        // 게임 오버
        if(lifeFromAI.equals("0") && (aiResponse.getGptsays() == null || aiResponse.getGptsays().isEmpty())){
            gameServiceLogger.info("게임 오버");
            frontText = aiResponse.getPlaylog();
            redisManager.deleteValue(username);
        }
        // 게임 엔딩
        else if(aiResponse.getGptsays().length() >= 1){
            gameServiceLogger.info("게임 엔딩");
            frontText = aiResponse.getPlaylog() + aiResponse.getGptsays();
            redisManager.deleteValue(username);
        }
        // 게임 진행
        else{
            gameServiceLogger.info(("일반적인 게임 진행"));
            redisManager.setTextKeyTextValue(username, aiResponse.getWorldview(), ":worldview");
            gameServiceLogger.info("세계관 저장된것 : {}", redisManager.getTextKeyTextValue(username, ":worldview"));
            redisManager.setTextKeyTextValue(username, aiResponse.getCharsetting(), ":charsetting");
            gameServiceLogger.info("캐릭터 성향 저장된 것 : {}", redisManager.getTextKeyTextValue(username, ":charsetting"));
            redisManager.setTextKeyTextValue(username, aiResponse.getAim(), ":aim");
            gameServiceLogger.info("캐릭터 목표 : {}", redisManager.getTextKeyTextValue(username, ":aim"));

            frontText = aiResponse.getPlaylog();
            String savedText = redisManager.getTextKeyTextValue(username, ":playlog");
            savedText += " ";
            savedText = savedText.concat(frontText);
            redisManager.setTextKeyTextValue(username, savedText, ":playlog");

            if(aiResponse.getStatus() != null){
                // 체크 요망

                redisManager.setPlayerStatus(username, aiResponse.getStatus());
                gameServiceLogger.info("redis에 저장된 스테이터스 : {}", redisManager.getPlayerStatus(username));
            }
            if(aiResponse.getLife() != null){
                gameServiceLogger.info("피통 : {}", aiResponse.getLife());
                Map<String, String> playerStatus = aiResponse.getStatus();
                redisManager.setPlayerLife(username, String.valueOf(aiResponse.getLife()));
            }
            if(aiResponse.getInventory() != null){
                gameServiceLogger.info("인벤토리 {}", aiResponse.getInventory());
                Map<String, String> rawInventory = aiResponse.getInventory();
                for (Map.Entry<String, String> entry : rawInventory.entrySet()) {
                    redisManager.setItemToInventory(username, entry.getKey(), entry.getValue());
                }
            }

        }

        // 인벤토리와 스테이터스 가져오기
        Map<String, String> updatedInventory = redisManager.getInventory(username);
        Map<String, String> updatedStatus = redisManager.getPlayerStatus(username);

        // status에서 근력 값 추출
        int strength = Integer.parseInt(redisManager.getPlayerStrength(username)); // 근력 값이 없을 경우 0으로 설정
        int inventoryWeight = redisManager.getInventorySum(username);
        boolean isFull = inventoryWeight > strength;  // 가방이 근력보다 무거운지 여부

        // 프론트로 보낼 데이터 생성 (dice는 프론트로 전달되지 않음)
        return PlayerDataFrontDTO.builder()
                .isStart(0)
                .username(username)
                .worldview(null)
                .charsetting(null)
                .aim(null)
                .status(updatedStatus)
                .life((String) redisManager.getPlayerLife(username))
                .inventory(updatedInventory)
                .isfull(isFull)
                .playlog(redisManager.getTextKeyTextValue(username, ":playlog"))
                .gptsays(aiResponse.getGptsays())
                .choices(aiResponse.getChoices())
                .imageurl(aiResponse.getImageurl())
                .build();
    }

    // AI 모듈에 데이터 전송 및 응답 받기
    private PlayerDataAiDTO sendToAiModule(PlayerDataAiDTO playerDataAiDTO) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<PlayerDataAiDTO> request = new HttpEntity<>(playerDataAiDTO, headers);
        String aiUrl = "http://52.79.97.201:5000/ai/process";

        try {
            // AI 모듈에서 통신할 때 getBody하면 ResponseEntity에 설정한 제네릭 T 값으로 반환해줌.
            // 여기서 DTO 클래스를 T로 설정하고 DTO.class 파일을 파라미터로 주면 됨.
            ResponseEntity<PlayerDataAiDTO> responseEntity = restTemplate.exchange(aiUrl, HttpMethod.POST, request, PlayerDataAiDTO.class);

            // 응답 내용 확인
            PlayerDataAiDTO aiResponse = responseEntity.getBody();
            gameServiceLogger.info("AI 응답 전체: {}", aiResponse);  // 전체 DTO를 로그로 출력
            gameServiceLogger.info("AI 응답에서 aim 값: {}", aiResponse.getAim());  // aim 값만 따로 출력
            return aiResponse;

        } catch (Exception e) {
            gameServiceLogger.error("AI 모듈과의 통신 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("AI 모듈과의 통신 중 오류가 발생했습니다.", e);
        }
    }
}
