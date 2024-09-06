package com.be.chat_niverse.controller;

import com.be.chat_niverse.config.BaseResponse;
import com.be.chat_niverse.dto.PlayerDataFrontDTO;
import com.be.chat_niverse.service.GameService;
import com.be.chat_niverse.service.RedisManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private static final Logger gameLogger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService gameService){
        this.gameService = gameService;
    }


    // 프론트에서 준 DTO에서 selectedchoice 데이터가 null이면 초기시작
    // 아니면 정상 게임플레이
    // 저거 파라미터 받은거랑 Redis 저장 데이터 JSON 형태로 합해서
    // flask로 작성한 AI모듈에 플레이 Data를 JSON 형태로 콜링해서 보내기 (POST, /process) http://13.124.84.163/process
    // @app.route('/process', methods=['POST'])
    //  def process_request():
    // 그리고 콜백으로 받은 데이터를 받아서 Redis에 save하고
    //
    // life가 0이고 GPT한줄평이 null이면 json에서 깐 next_story를 TEXT에 저장
    // GPT한줄평이 null이 아니면 json에서 깐 next_story와 GPT한줄평을 더해서 TEXT에 저장
    // 위 조건들에 다 만족하지 못하면 다음 스토리(TEXT)와 선택지(Choices)는 변수로 빼 놨다가 프론트에 주는 DTO에 넣기.
    // Redis에서 닉네임, 인벤토리, life, TEXT
    // 거기서 나온 근력값과 가방 총합무게 조회해서 isfull 계산해서 주기
    //
    // 프론트가 요청하고 응답하는 DTO는 username, worldview, charsetting, aim,
    // status, life, inventory, isfull, playlog, gptsays,
    // selectedchoice
    //
    // AI가 요청하고 응답하는 DTO는
    // username, worldview, charsetting, aim,
    // status, life, inventory, playlog, dice, selectedchoice, gptsays
    @PostMapping("/start")
    public BaseResponse<PlayerDataFrontDTO> startGameProcess(@RequestBody PlayerDataFrontDTO playerDataFrontDTO) {
        // Controller는 요청을 Service에 넘기고 결과만 받아서 반환합니다.
        PlayerDataFrontDTO responseDTO = gameService.processGame(playerDataFrontDTO);
        return new BaseResponse<>(responseDTO);
    }

}
