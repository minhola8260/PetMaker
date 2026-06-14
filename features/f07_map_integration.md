# f07-지도 기반 위치 표시

- **관련 요구사항**: `FR-07`
- **관련 파일**: [MainActivity.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/MainActivity.kt), [MainScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/MainScreen.kt), [MapStyles.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/theme/MapStyles.kt)

---

## 1. 동작 설명
이 기능은 Google Maps API를 연동하여 지도 화면에서 사용자의 실시간 GPS 위치를 추적 표시하고, 사용자 위치 주변에 펫 소환진(포탈) 스폰 영역들을 동적으로 생성합니다. 사용자가 이 소환진 반경 40m 이내로 진입하면 펫 발견 트리거가 활성화되어 펫 생성을 시도할 수 있게 됩니다.

---

## 2. 기술 구현 방식
- **Google Maps SDK 및 Compose Maps 통합**:
  `com.google.maps.android:maps-compose` 라이브러리를 활용하여 Jetpack Compose 기반의 `GoogleMap` 컴포넌트를 메인 화면 배경에 렌더링합니다.
- **실시간 사용자 마킹 및 레이더 스캔**:
  - 사용자의 실시간 위치에 커스텀 마커(그림자용 서클, 모험가 코어 입체 서클, 하이라이트 코어)를 드로잉하여 시각적 깊이감을 줍니다.
  - 사용자를 중심으로 40m 상호작용 레이저 링(`Circle`)과 실시간으로 퍼져나가는 레이더 펄스 스캔 애니메이션을 렌더링하여 환경 탐색의 현장감을 부여합니다.
- **동적 포탈(스폰 스팟) 생성 및 상호작용**:
  - 사용자가 15m 이상 이동할 때마다 일정 확률로 주변에 동적 펫 스폰 위치(`spawnSpots`)가 생성됩니다.
  - 각 스폰 스팟은 지도상에 `MarkerComposable`로 렌더링되며, 아우터 포탈 링, 펄스 파동 이너 링, 그리고 발자국(`🐾`) 코어 구체를 가진 신비로운 소환진 형태로 나타납니다.
  - 카메라 줌 레벨(`stableZoom`)과 연계된 부드러운 마커 스케일링 처리를 더해 지도를 확대/축소해도 어색함 없이 크기가 유지됩니다.
- **거리 계산 및 발견 판정**:
  - 하버사인(Haversine) 공식을 사용하여 사용자의 위치와 스폰 스팟 좌표 간의 최단 거리(m)를 실시간 계산합니다.
  - 사용자가 스폰 스팟의 40m 반경 내로 진입하면 `isNearPortal`이 참(`true`)이 되며, 메인 화면 하단에 펄스 애니메이션이 가미된 "✨ 새로운 펫 발견!" 버튼이 활성화되어 펫 생성을 시작할 수 있습니다.
- **테마 자동 변경 및 맵 조작 모드**:
  - 기상 상태와 시간대(낮/밤)에 최적화된 복합 지도 스타일(`MapStyles.DAY`, `MapStyles.NIGHT`, `MapStyles.RAINY`, `MapStyles.SNOWY`, `MapStyles.CLOUDY`)을 동적으로 적용합니다.
  - 지도를 가볍게 탭하면 모든 UI가 부드럽게 페이드아웃되며 지도 화면을 온전히 크게 감상할 수 있는 조작 포커스 모드로 전환됩니다. 지도를 드래그해 자유롭게 탐색한 뒤, 3초 동안 조작이 없으면 다시 카메라가 사용자 위치로 부드럽게 복귀합니다.
