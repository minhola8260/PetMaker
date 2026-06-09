# f10-도감 검색/필터링 및 편집(방생) 기능

- **관련 요구사항**: `FR-10`
- **관련 파일**: [CollectionScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/CollectionScreen.kt), [DetailModal.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/DetailModal.kt)

---

## 1. 동작 설명
사용자는 도감 리스트에서 이름 및 외형설명 텍스트 검색창을 통해 수집한 펫을 조회하며, 날씨 카테고리 필터와 시간대 필터를 동시 적용해 복합 필터 검색을 할 수 있습니다. 또한 편집창에서 체크박스를 사용해 여러 마리를 일괄로 DB에서 영구 방생(삭제)하고 썸네일을 전체화면 팝업으로 감상할 수 있습니다.

---

## 2. 기술 구현 방식
- **복합 필터 알고리즘**:
  - `remember` 블록을 활용해 텍스트 쿼리(`searchQuery`), 날씨 필터(`selectedWeatherFilter`), 시간대 필터(`selectedTimezoneFilter`) 세 가지 상태값이 변경될 때마다 펫 DB 목록에 `.filter` 함수 연산을 즉각 수행해 필터링 결과를 실시간 렌더링합니다.
  - 시간대 필터의 경우 한국어 탭(아침, 오후, 저녁, 밤)을 펫 엔티티의 영문 시간대 값(Morning, Afternoon, Evening, Night)과 병렬 맵핑 검사합니다.
- **다중 선택 일괄 삭제 (방생)**:
  - 편집 버튼 클릭 시 체크박스가 좌측 슬라이드 활성화되며, "전체 선택" 및 "선택 해제" 기능을 버튼 하나로 일괄 상태 맵핑합니다.
  - 하단에 선택 수량을 명시한 삭제 버튼을 띄우고 다중 삭제 컨펌 얼럿을 띄워 사용자 오동작을 배제한 뒤 `petDao.deletePet(pet)` 데이터 트랜잭션을 코루틴 내에서 일괄 실행합니다.
- **전체화면 오버스케일 팝업**:
  - 도감 썸네일 카드의 보라색 🔍 돋보기 오버레이 뱃지 혹은 상세 창의 프로필을 터치하면 [FullscreenImageDialog](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/CollectionScreen.kt#L291)가 트리거되어, 95% 반투명 검정 디밍 레이어 위에 종횡비 1:1을 보존하며 전체화면으로 감상할 수 있습니다.
