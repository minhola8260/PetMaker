# f08-API Key 안전 관리 및 설정 기능

- **관련 요구사항**: `FR-08`
- **관련 파일**: [SettingsDialog.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/components/SettingsDialog.kt), [ApiKeyManager.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/ApiKeyManager.kt)

---

## 1. 동작 설명
사용자는 기상 데이터 및 AI 연동용 API Key들(OpenWeatherMap, Gemini, OpenAI)을 설정 창에서 직접 입력하고 기기에 안전하게 보관하여 소스코드 유출 시의 보안을 확보할 수 있습니다. 또한 가상 날씨 및 가상 시간대 등의 디버그/테스트용 유틸리티 도구를 본 다이얼로그에서 제어할 수 있습니다.

---

## 2. 기술 구현 방식
- **SharedPreferences 기반 격리 보관**:
  텍스트 필드로 입력한 API 키들은 [ApiKeyManager](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/ApiKeyManager.kt) 모듈을 통해 기기 샌드박스의 `api_keys_prefs` 라는 SharedPreferences에 직렬화 보관 처리하여 앱 종료 시에도 영구 보관되며 런타임에 즉시 갱신되어 반영됩니다.
- **기본값 로드 (.env 연계)**:
  앱 기동 시 SharedPreferences에 저장된 키가 비어 있을 경우, Gradle 빌드 시 주입된 `BuildConfig`의 기본 디폴트 키값(로컬 환경의 `.env` 파일에 기록된 키값)으로 자동 로드하여 사용자 입력 불편을 최소화합니다.
- **표시/숨김 토글 필드**:
  설정 다이얼로그 각 API 키 입력 필드 우측에 Password Visual Transformation을 활용한 마스킹 표시/숨김 버튼을 결합해 자격 증명의 노출을 예방합니다.
- **키 유효성 체크**:
  `hasRequiredKeys()` 메소드를 통해 필수 키들(OpenWeatherMap, Gemini, OpenAI API 키)의 등록 여부를 체크하여 펫 생성 버튼 활성화 여부 조건으로 사용합니다.
- **가상 테스트 및 유틸리티 도구**:
  - **가상 날씨 설정**: API 연동 기상 외에 사용자가 수동으로 맑음, 비, 눈, 흐림 상태를 강제 주입하여 기상 애니메이션과 이미지 생성 조건을 즉각 테스트하도록 지원합니다.
  - **가상 시간대 설정**: 아침, 오후, 저녁, 밤 시간대를 강제로 주입하여 펫 콘셉트와 지도 테마 변화를 제어할 수 있습니다.
  - **테스트 편의 기능**: 현재 위치(위경도 및 Geocoder 주소) 표시, 저장된 펫 목록 초기화(전체 삭제), 펫 도감 레벨 정보 표시 등을 결합하였습니다.
