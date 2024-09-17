# Chat-Niverse 백엔드

이 프로젝트는 Spring Boot와 Redis를 기반으로 구현된 Chat-Niverse 애플리케이션의 백엔드입니다. 
이 시스템은 ChatGPT API로 AI와 통신하여 게임의 진행 상황을 처리하며, 플레이어의 상태를 관리합니다. 
게임은 플레이어가 목표를 달성하거나, 생명이 0이 되어 게임오버가 되는 방식으로 종료됩니다.

## 프로젝트 구조

```
Chat-niverse_BE-develop/
│
├── .github/                # GitHub 워크플로우 및 설정 파일
├── build.gradle            # Gradle 빌드 파일
├── gradle/                 # Gradle 래퍼 관련 파일
├── gradlew                 # 유닉스 시스템용 Gradle 래퍼
├── gradlew.bat             # 윈도우 시스템용 Gradle 래퍼
├── settings.gradle         # Gradle 설정 파일
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/be/chat_niverse/
    │   │       ├── ChatNiverseApplication.java  # 애플리케이션 진입점
    │   │       ├── config/                      # Redis 설정 파일
    │   │       ├── controller/                  # REST API 엔드포인트
    │   │       ├── dto/                         # 데이터 전송 객체
    │   │       ├── exception/                   # 예외 처리
    │   │       └── service/                     # 비즈니스 로직
    │   └── resources/
    │       └── application.yml                  # 애플리케이션 설정 파일
    └── test/                                    # 테스트 파일
```

## 주요 기능

### 1. 게임 시작 (POST /api/start)
플레이어가 게임을 시작할 때 호출되는 엔드포인트입니다. 
플레이어의 데이터는 프론트엔드에서 전달되고, AI 모듈과 상호작용을 통해 게임 상태가 업데이트됩니다.

**요청**:
- **Body**: `PlayerDataFrontDTO` (플레이어의 이름, 캐릭터 설정, 목표 등 포함)

**응답**:
- AI 모듈로부터 받은 데이터를 기반으로 게임의 상태를 업데이트한 후 `PlayerDataFrontDTO`를 반환합니다.

```
POST /api/start
{
  "username": "player1",
  "worldview": "adventurous",
  "charsetting": "warrior",
  "aim": "find treasure",
  "isStart": 1
}
```

### 2. 게임 진행 로직
- **Redis**에서 플레이어의 현재 상태(능력치, 생명, 인벤토리 등)를 불러옵니다.
- **AI 모듈**과 통신하여 플레이어의 다음 행동을 결정합니다.
- 게임 상태가 갱신되면, 플레이어의 새로운 상태를 Redis에 저장합니다.
- AI의 응답에는 GPT가 제공하는 피드백, 선택지, 이미지 URL 등이 포함됩니다.

### 3. 게임 종료 로직
게임 종료는 두 가지 상황에서 발생합니다:
- **플레이어가 목표를 달성**한 경우: AI 모듈의 응답에 `gptsays`가 포함되면, 플레이어가 목표를 달성하여 게임이 종료된 것으로 간주합니다.
- **게임오버**: 플레이어의 `life`가 0이 되면 게임오버가 발생하고, 게임은 종료됩니다.

#### 목표 달성 예시:
플레이어가 목표를 달성한 경우 AI 응답에 `gptsays` 필드가 포함됩니다. 
이 필드는 플레이어의 성취를 나타내며, 플레이어가 게임에서 성공적으로 목표를 이루었음을 의미합니다.

```
public PlayerDataFrontDTO processGame(PlayerDataFrontDTO playerDataFrontDTO) {
    // GPT의 피드백을 통해 목표 달성 여부 확인
    if (aiResponse.getGptsays() != null) {
        // 플레이어가 목표를 달성했을 때 처리
        playerDataFrontDTO.setGptsays(aiResponse.getGptsays());
        return playerDataFrontDTO;  // 목표 달성 후 플레이어에게 결과를 전달
    }
    return updatedPlayerData;
}
```

#### 게임오버 예시:
플레이어의 생명이 0이 되면 게임오버 처리됩니다.

```
public PlayerDataFrontDTO processGame(PlayerDataFrontDTO playerDataFrontDTO) {
    // 생명이 0이 된 경우
    if (playerDataFrontDTO.getLife().equals("0")) {
        // 게임오버 처리
        return playerDataFrontDTO;  // 플레이어에게 게임오버 결과 전달
    }
    return updatedPlayerData;
}
```

### 4. AI 모듈과의 통신
AI 모듈에 `PlayerDataAiDTO` 데이터를 보내고, 게임에 대한 응답을 받습니다. AI는 게임 진행에 필요한 선택지와 추가적인 정보를 제공합니다.

**AI 모듈과의 통신 예시**:

```
private PlayerDataAiDTO sendToAiModule(PlayerDataAiDTO playerDataAiDTO) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    HttpEntity<PlayerDataAiDTO> request = new HttpEntity<>(playerDataAiDTO, headers);
    String aiUrl = "http://52.79.97.201:5000/ai/process";

    ResponseEntity<PlayerDataAiDTO> responseEntity = restTemplate.exchange(aiUrl, HttpMethod.POST, request, PlayerDataAiDTO.class);
    return responseEntity.getBody();
}
```

### 5. 플레이어 상태 관리 (Redis)
- **Redis**를 사용해 플레이어의 상태를 저장하고 불러옵니다.
- 게임 중 플레이어의 생명, 인벤토리, 로그 등의 정보를 관리합니다.

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.
