# f01-실시간 환경 정보 수집 및 표시

- **관련 요구사항**: `FR-01`
- **관련 파일**: [MainActivity.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/MainActivity.kt), [MainScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/MainScreen.kt), [WeatherApi.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/remote/WeatherApi.kt)

---

## 1. 동작 설명
사용자는 앱의 메인 화면에서 현재 위치의 날씨, 기온, 시간대 정보를 직관적인 UI로 확인하며 현재 시간이 초 단위로 실시간 업데이트되어 기상 카드 상단에 노출됩니다.

---

## 2. 기술 구현 방식
- **GPS 수집 및 지오코딩**:
  `FusedLocationProviderClient`를 활용하여 기기의 실시간 GPS 좌표(위도, 경도)를 획득하고, `Geocoder`를 통해 한국어 기반 구/동 단위(예: "서울 종로구")로 주소를 동적 파싱하여 화면에 업데이트합니다.
- **기상 데이터 연동**:
  OpenWeatherMap API([WeatherApi](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/remote/WeatherApi.kt))를 호출하여 실시간 기온, 체감 온도, 습도, 풍속, 기압 및 기상 유형(비, 눈, 맑음, 흐림)을 수집합니다.
- **시간대 매핑 및 시계**:
  기기 관측 시간(Hour)에 따라 시간대 텍스트(Morning, Afternoon, Evening, Night)를 도출하고, `LaunchedEffect` 내 코루틴 지연 루프(`delay(1000)`)를 통해 초 단위 실시간 시계를 UI에 렌더링합니다.
