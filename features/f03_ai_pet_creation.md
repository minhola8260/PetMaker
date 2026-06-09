# f03-AI 기반 고유 펫 생성

- **관련 요구사항**: `FR-03`
- **관련 파일**: [GenerationScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/GenerationScreen.kt), [GeminiApi.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/remote/GeminiApi.kt), [ResultScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/ResultScreen.kt), [PetAnimations.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/components/PetAnimations.kt)

---

## 1. 동작 설명
사용자가 펫 발견 진입 시, 시스템은 실시간 수집된 환경 정보(위치, 기온, 날씨, 시간대)를 결합하여 Google Gemini API에 전달하고, 환경 맥락에 조화되는 고유 펫의 정보(이름, 상세 외형, 특징, 성격)를 반환받습니다. 생성 완료 시 특별한 파티클 효과와 함께 결과가 표시됩니다.

---

## 2. 기술 구현 방식
- **Gemini API 연동 및 자동 폴백 체인**:
  [GeminiApi](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/remote/GeminiApi.kt) (`gemini-3.5-flash` 모델 기본값)을 호출하여 기상 조건 매개변수 프롬프트를 전달하며 JSON 구조 응답을 수신합니다. 만약 503(일시적 과부하)이나 런타임 에러가 발생하면 1초 대기 후 최대 2회까지 자동 재시도(Retry)하며, 최종 실패 시 대안 모델들(`gemini-3.1-flash-lite`, `gemini-2.5-flash`, `gemini-2.0-flash`)로 순차 폴백(Fallback)을 수행해 무중단 정보 생성을 보장합니다. 수신 후 Gson을 사용해 펫 명칭(`name`), 상세 외형 묘사 설명(`description`), 영문 이미지 생성 프롬프트(`english_visual_prompt`), 성격(`personality`), 3개 이하의 특성 단어 리스트(`traits`)로 이루어진 `PetCreationData` 모델 클래스로 자동 직렬화 해제합니다.
- **로딩 및 별가루 파티클 효과**:
  - 생성 중에는 보라-핑크 그라디언트 배경 위에서 부드럽게 회전하는 대형 Sparkles 반짝이 그래픽(`RotatingSparkles`)과 점 3개가 튀는 인디케이터(`ThreeDotLoading`) 로딩창을 가동합니다.
  - 생성 완료 후 결과 화면 진입 시, 최초 2초 동안 화면 전체에 여러 파스텔 색상의 별 입자가 퍼지며 흩날리는 축하 특수 연출(`StarParticleEffect`)을 구동한 뒤 펫 카드를 렌더링합니다.
