# f09-펫 이미지 수집 및 자동 폴백 시스템

- **관련 요구사항**: `FR-09`
- **관련 파일**: [GenerationScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/GenerationScreen.kt), [FluxApi.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/remote/FluxApi.kt)

---

## 1. 동작 설명
시스템은 Gemini AI가 수립한 영문 3D 렌더링 프롬프트 정보를 가공해 고화질의 펫 디자인 이미지를 생성하며, 유료 API(Replicate) 호출 시 크레딧이나 키 에러 등의 예외가 검출되면 무료 우회 서버(HuggingFace)로 자동 전환(Fallback) 처리하여 무중단 이미지 공급을 구축합니다.

---

## 2. 기술 구현 방식
- **1단계 시도 (Replicate 비동기 예측 및 폴링)**:
  `black-forest-labs/flux-schnell` API를 호출해 이미지 생성을 비동기로 등록합니다. 예측 시작 응답 수신 후, `urls.get` 상태 엔드포인트를 대상으로 1.5초 주기씩 최대 12회(총 18초) 스레드를 대기시키며 결과 예측이 완료(`succeeded`)될 때까지 실시간 상태 검사를 폴링합니다.
- **2단계 자동 우회 (HuggingFace 동기 추론 폴백)**:
  1단계 도중 네트워크 장애, Replicate 키 설정 부재, 혹은 크레딧 초과 에러(HTTP 402 등)가 감지되어 에러 예외가 throw되면, 해당 예외 트랩을 타고 HuggingFace FLUX.1-schnell 무료 추론 허브 컴포넌트로 요청을 즉시 강제 전환(Fallback)합니다.
- **바이너리 물리 파일 저장**:
  어느 단계를 통과하든 획득한 비주얼 이미지 바이이트 스트림 데이터를 디바이스 캐시 디렉토리에 WebP 파일(`temp_pet_xxxx.webp`)로 로컬 캐싱하여 런타임 이미지 바인딩 로드를 가능케 합니다.
