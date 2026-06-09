# f11-실시간 기상 센서 및 상세 환경 대시보드

- **관련 요구사항**: `FR-11`
- **관련 파일**: [MainScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/MainScreen.kt)

---

## 1. 동작 설명
사용자는 메인 화면의 기상 카드를 탭하여 상세 2x4 대시보드 오버레이 팝업창을 띄워 위경도, 체감 온도, 습도, 풍속, 기압, 관측 시간대 등을 가독성 높은 차트 그래픽으로 조회합니다. 풍속 세기에 비례해 속도가 바뀌는 바람개비(🌀) 애니메이션이 제공되어 역동적인 재미를 줍니다.

---

## 2. 기술 구현 방식
- **스프링 물리 스케일링 팝업 및 배경 블러**:
  - 날씨 상세 카드 노출 여부(`showWeatherDetails`) 상태에 따라 기상 상세 대시보드가 활성화될 때 `updateTransition`을 사용하여 검은 디밍 배경 알파값은 0.75f로 자연스럽게 오르고, 대시보드 카드는 `spring(dampingRatio = Spring.DampingRatioLowBouncy)` 탄성 계수 애니메이션으로 나타나 통통 튀며 확대 렌더링됩니다.
  - 대시보드가 열리면 뒷배경은 `blur(16.dp)`를 적용하여 메인화면을 뿌옇게 가려 집중도를 향상시켰습니다.
- **2x4 그리드 및 가로 프로그레스 바**:
  - 위경도 좌표, 기온, 체감 온도, 최저/최고 기온 격차 범위, 습도, 풍속, 기압, 시간대 8가지 정보의 세밀한 관측 수치 텍스트를 렌더링합니다.
  - 기온, 체감온도, 습도, 기압 등 변동폭이 큰 6가지 핵심 필드에 대해 시각적으로 가시화된 가로형 수치 프로그레스 바 그래픽을 탑재하였습니다.
- **풍속 기반 바람개비 연동 애니메이션**:
  - [GridDetailItem](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/MainScreen.kt#L631) 컴포넌트는 평균 풍속(m/s) 세기 데이터를 기반으로 `rotationAngle` 무한 회전 주기 스레드를 계산하여, 풍속이 강할수록 바람개비(🌀) 이모티콘 회전율이 빨라지도록 캔버스 및 그래픽 회전 각도 가중치를 매 프레임 다르게 렌더링합니다.
