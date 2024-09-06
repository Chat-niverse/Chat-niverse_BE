package com.be.chat_niverse.controller;

import com.be.chat_niverse.config.BaseResponse;
import com.be.chat_niverse.dto.PlayerDataFrontDTO;
import com.be.chat_niverse.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private static final Logger gameLogger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService gameService){
        this.gameService = gameService;
    }

    @PostMapping("/start")
    public BaseResponse<PlayerDataFrontDTO> startGameProcess(@RequestBody PlayerDataFrontDTO playerDataFrontDTO) {
        PlayerDataFrontDTO responseDTO = gameService.processGame(playerDataFrontDTO);
        return new BaseResponse<>(responseDTO);
    }

}
