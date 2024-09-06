package com.be.chat_niverse.service;

import com.be.chat_niverse.dto.PlayerDataAiDTO;
import com.be.chat_niverse.dto.PlayerDataFrontDTO;
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

    public GameService(RedisManagerImpl redisManager) {
        this.redisManager = redisManager;
    }

    // 게임 진행을 처리하는 비즈니스 로직
    public PlayerDataFrontDTO processGame(PlayerDataFrontDTO playerDataFrontDTO) {
        String username = playerDataFrontDTO.getUsername();

        // Redis에서 기존 데이터 가져오기
        Map<String, Integer> status = redisManager.getPlayerStatus(username);
        Integer life = (Integer) redisManager.getPlayerLife(username);  // null 가능성을 고려해 Integer로 받음
        Map<String, Integer> inventory = redisManager.getInventory(username);

        PlayerDataAiDTO playerDataAiDTO;
        // 1이면 시작 username, worldview, charsetting, aim 이 들어감
        if(playerDataFrontDTO.getIsStart() == 1){
            playerDataAiDTO = PlayerDataAiDTO.builder()
                    .username(username)
                    .worldview(playerDataFrontDTO.getWorldview())
                    .charsetting(playerDataFrontDTO.getCharsetting())
                    .aim(playerDataFrontDTO.getAim())
                    .build();
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
        Map<String, Object> aiResponse = sendToAiModule(playerDataAiDTO);

        // 여기서부터는 업데이트 데이터 전송
        // AI 응답 처리 및 Redis에 저장
        // aiResponse에는 이제 status, life, inventory, playlog, gptsays, choices, imageurl이 옴.
        // frontText라는 변수가 있는데 이건 프론트로 전달해줄 스토리를 담고 있는 String형 변수야.
        // if (life == 0 && aiResponse.get("gptsays") == null) 가 true면  playlog를 frontText에 그대로 저장하고 redis에서 username으로 키값을 접근해서 삭제
        // if (aiResponse.get("gptsays") != null) 가 true aiResponse.get("playlog") + aiResponse.get("gptsays")를 frontText에 저장하고 redis에서 키값을 접근해서 삭제
        // 위의 조건 다 해당 안되면 redis에서 username으로 키값을 접근해서 Redis의 playlog를 조회해서 tempStory 변수에 저장하고 aiResponse.get("playlog")값을 더해서 Redis의 username으로 접근해서 Redis의 username으로 접근해서 playlog 키값에 tempStory 저장
        // redis의 username으로 접근해서 각 status, life, inventory, gptsays를 aiResponse.get("키값")으로 각 키 값에 해당하는 value를 aiResponse에서 찾아서 그걸로 업데이트시킴.

//        String nextStory = (String) aiResponse.get("nextstory");
//        String gptSays = (String) aiResponse.get("gptsays");
//
//        redisManager.setTextKeyTextValue(username, (String) aiResponse.get("worldview"), ":worldview");
        String frontText = null;
        Integer lifeFromAI = Integer.parseInt((String) aiResponse.get("life"));
        // 게임 오버
        if(lifeFromAI == 0 && aiResponse.get("gptsays") == null){
            frontText = (String) aiResponse.get("playlog");
            redisManager.deleteValue(username);
        }
        // 게임 엔딩
        else if(aiResponse.get("gptsays") != null){
            frontText = (String) aiResponse.get("playlog") + (String) aiResponse.get("gptsays");
            redisManager.deleteValue(username);
        }
        // 게임 진행
        else{
            redisManager.setTextKeyTextValue(username, (String) aiResponse.get("worldview"), ":worldview");
            redisManager.setTextKeyTextValue(username, (String) aiResponse.get("charsetting"), ":charsetting");
            redisManager.setTextKeyTextValue(username, (String) aiResponse.get("aim"), ":aim");

            frontText = (String) aiResponse.get("playlog");
            String savedText = redisManager.getTextKeyTextValue(username, ":playlog");
            savedText += " ";
            savedText = savedText.concat(frontText);
            redisManager.setTextKeyTextValue(username, savedText, ":playlog");

            if(aiResponse.get("status") != null){
                // 체크 요망
                redisManager.setPlayerStatus(username, (Map<String, Integer>) aiResponse.get("status"));
            }
            if(aiResponse.get("life") != null){
                redisManager.setPlayerLife(username, Integer.parseInt((String) aiResponse.get("life")));
            }
            if(aiResponse.get("inventory") != null){
                Map<Object, Object> rawInventory = (Map<Object, Object>) aiResponse.get("inventory");
                for (Map.Entry<Object, Object> entry : rawInventory.entrySet()) {
                    redisManager.setItemToInventory(username, (String) entry.getKey(), Integer.parseInt((String) entry.getValue()));
                }
            }

        }

        // 인벤토리와 스테이터스 가져오기
        Map<String, Integer> updatedInventory = redisManager.getInventory(username);
        Map<String, Integer> updatedStatus = redisManager.getPlayerStatus(username);

        // status에서 근력 값 추출
        int strength = status.getOrDefault("strength", 0);  // 근력 값이 없을 경우 0으로 설정
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
                .life(Integer.parseInt((String)redisManager.getPlayerLife(username)))
                .inventory(updatedInventory)
                .isfull(isFull)
                .playlog(redisManager.getTextKeyTextValue(username, ":playlog"))
                .gptsays((String) aiResponse.get("gptsays"))
                .choices((Map<String, String>)aiResponse.get("choices"))
                .imageurl((String) aiResponse.get("imageurl"))
                .build();
    }

    // AI 모듈에 보낼 DTO 생성 (dice는 AI 요청에 포함됨)
    private PlayerDataAiDTO createPlayerDataAiDTO(PlayerDataFrontDTO playerDataFrontDTO, Map<String, Integer> status, Integer life, Map<String, Integer> inventory) {
        return PlayerDataAiDTO.builder()
                .username(playerDataFrontDTO.getUsername())
                .worldview(playerDataFrontDTO.getWorldview())
                .charsetting(playerDataFrontDTO.getCharsetting())
                .aim(playerDataFrontDTO.getAim())
                .status(status)
                .life(life)
                .inventory(inventory)
                .playlog(playerDataFrontDTO.getPlaylog())
                .dice((int) (Math.random() * 20) + 1)  // AI 요청 시 dice 값 생성
                .selectedchoice(playerDataFrontDTO.getSelectedchoice())  // selectedchoice가 null일 수 있으므로 그대로 전달
                .gptsays(null)  // 처음에는 GPT의 한줄평 없음
                .choices(playerDataFrontDTO.getChoices())  // 선택지를 그대로 전달 (null 가능)
                .build();
    }

    // AI 모듈에 데이터 전송 및 응답 받기
    private Map<String, Object> sendToAiModule(PlayerDataAiDTO playerDataAiDTO) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<PlayerDataAiDTO> request = new HttpEntity<>(playerDataAiDTO, headers);
        String aiUrl = "http://13.124.84.163/ai/process";

        ResponseEntity<Map> responseEntity = restTemplate.exchange(aiUrl, HttpMethod.POST, request, Map.class);
        return responseEntity.getBody();
    }

    // 프론트에 보낼 DTO 생성 (프론트에는 dice 값을 전달하지 않음)
    private PlayerDataFrontDTO createPlayerDataFrontDTO(PlayerDataFrontDTO playerDataFrontDTO, Map<String, Integer> updatedStatus, Integer life, Map<String, Integer> updatedInventory, boolean isFull, String nextStory, String gptSays) {
        return PlayerDataFrontDTO.builder()
                .username(playerDataFrontDTO.getUsername())
                .worldview(playerDataFrontDTO.getWorldview())
                .charsetting(playerDataFrontDTO.getCharsetting())
                .aim(playerDataFrontDTO.getAim())
                .status(updatedStatus)
                .life(life)
                .inventory(updatedInventory)
                .isfull(isFull)
                .playlog(nextStory)
                .gptsays(gptSays)
                .selectedchoice(playerDataFrontDTO.getSelectedchoice())
                .choices(playerDataFrontDTO.getChoices())  // 선택지 추가
                .build();
    }
}
