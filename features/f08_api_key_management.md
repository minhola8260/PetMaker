# f08-API Key 안전 관리 및 설정 기능

- **관련 요구사항**: `FR-08`
- **관련 파일**: [SettingsDialog.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/components/SettingsDialog.kt), [ApiKeyManager.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/ApiKeyManager.kt)

---

## 1. 동작 설명
사용자는 기상 데이터 및 AI 연동용 API Key들(OpenWeatherMap, Gemini, Replicate, HuggingFace)을 직접 입력하고 기기에 안전하게 보관하여 코드 하드코딩 노출을 피할 수 있습니다.

---

## 2. 기술 구현 방식
- **SharedPreferences 기반 격리 보관**:
  텍스트 필드로 입력한 API 키들은 [ApiKeyManager](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/ApiKeyManager.kt) 모듈을 통해 기기 샌드박스의 `api_keys_prefs` 라는 SharedPreferences에 직렬화 보관 처리하여 앱 종료 시에도 영구 보관되며 런타임에 즉시 갱신되어 반영됩니다.
- **표시/숨김 토글 필드**:
  설정 다이얼로그 텍스트 필드 우측에 Password Visual Transformation을 활용한 마스킹 표시/숨김 버튼을 결합해 개인 토큰 자격 증명의 외부 노출을 예방합니다.
- **키 유효성 체크**:
  `hasRequiredKeys()` 메소드를 통해 필수 키들(날씨 API 키, Gemini API 키, Replicate 혹은 HuggingFace 키 중 1개)의 등록 상태를 유기적으로 체크하여 펫 생성 진입 활성화 가부 조건으로 사용합니다.
