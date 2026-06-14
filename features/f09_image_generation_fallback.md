# f09-펫 이미지 수집 및 관리 시스템

- **관련 요구사항**: `FR-09`
- **관련 파일**: [GenerationScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/GenerationScreen.kt), [OpenAiApi.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/remote/OpenAiApi.kt)

---

## 1. 동작 설명
시스템은 Gemini AI가 작성한 영문 고해상도 이미지 생성 프롬프트를 OpenAI 이미지 생성 API에 전송하여 맞춤식 펫 그래픽 일러스트를 실시간으로 제작합니다. 생성 완료된 이미지는 원격 URL 다운로드 혹은 Base64 디코딩을 거쳐 앱 로컬 내부 저장소에 영구 보관 및 캐싱 처리됩니다.

---

## 2. 기술 구현 방식
- **OpenAI 이미지 생성 요청**:
  - `OpenAiApi.generateImage()` API에 생성할 프롬프트와 요청 사항을 실어 전송합니다.
  - 디폴트 모델값인 `gpt-image-2` 및 이미지 규격(`1024x1024`), 가벼운 성능 옵션(`quality = "low"`)으로 요청하여 응답 소요 시간을 최소화합니다.
- **다중 이미지 응답 파싱 및 다운로드 시스템**:
  - **Base64 데이터 우선 디코딩**: 반환된 이미지 JSON 결과 중 `b64_json` 필드에 인코딩 텍스트가 존재할 경우, 즉각 `android.util.Base64` 바이트 디코더를 이용해 로컬 메모리로 로드합니다.
  - **URL 다운로드 폴백**: 만약 `b64_json`이 누락되거나 비어 있고 `url` 필드가 전달된 경우, OkHttpClient 인스턴스를 즉석 가동해 타임아웃 15초 설정을 가진 이미지 바이너리 다운로드 HTTP 요청을 비동기로 실행하여 이미지 바이트 스트림을 획득합니다.
- **바이너리 물리 파일 캐싱**:
  - 획득한 이미지 바이트 데이터를 디스크 쓰기 스트림(`FileOutputStream`)을 활용해 앱 내 임시 캐시 디렉토리(`context.cacheDir`)에 WebP 포맷 파일(`temp_pet_<timestamp>.webp`)로 일시 저장합니다.
  - 사용자가 도감 수집을 동의하는 최종 단계(`ResultScreen`)에서 비로소 임시 폴더 내의 파일을 앱의 영구 내부 저장소 영역(`context.filesDir`)으로 이동해 소장 완료를 진행합니다.
