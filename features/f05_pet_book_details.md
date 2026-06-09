# f05-펫 도감 열람 및 상세 정보 확인

- **관련 요구사항**: `FR-05`
- **관련 파일**: [CollectionScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/CollectionScreen.kt), [DetailModal.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/DetailModal.kt)

---

## 1. 동작 설명
사용자는 펫 도감 화면에서 자신이 이제껏 수집한 모든 고유 펫의 목록(이름, 수집일자, 특성 2개 요약, 생성 날씨/기온)을 카드 형태로 열람하며, 카드 선택 시 모달 팝업 형태로 상세 프로필을 확인할 수 있습니다.

---

## 2. 기술 구현 방식
- **실시간 도감 리스트 관찰**:
  Room Local DB 데이터셋을 Kotlin Flow 데이터 스트림 형태로 실시간 관찰(`asFlow().collectAsState`)하여 `LazyColumn`을 통해 Compose UI에 리스트 카드를 자동 리로드합니다.
- **도감 요약 카드**:
  각 도감 카드에는 펫 썸네일(돋보기 오버레이 뱃지 포함), 이름, 타임스탬프 기반 날짜 표기(`yyyy. M. d.`), 2개의 키워드 태그, 기상 상태별 매핑 이모지(☀️, 🌧️, ❄️, ☁️) 및 기온 정보가 요약되어 가시적으로 나타납니다.
- **Empty State UI**:
  수집된 펫이 없을 경우 🎭 마스크 이모지와 함께 "아직 수집한 펫이 없어요"라는 친절한 Empty State 가이드 화면을 드로잉합니다.
- **상세 프로필 모달 다이얼로그 (`DetailModal`)**:
  도감 카드를 선택하면 팝업 상세 모달 창이 표출됩니다. 펫 프로필(클릭 시 풀스크린 뷰어 실행), 이름, 상세 생성 환경 메타(기온, 날씨, 시간대, 주소)를 개별 서브카드로 격리해 렌더링하며 외형 특징, 내면 성격, 전체 특성 뱃지와 초 단위 정밀 수집일을 표출합니다.
