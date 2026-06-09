# f02-환경 기반 펫 생성 가능 여부 판정

- **관련 요구사항**: `FR-02`
- **관련 파일**: [MainScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/MainScreen.kt)

---

## 1. 동작 설명
시스템은 실시간 환경 데이터가 기기에 적재된 상태에서 내부 주기 판정을 수행하여 특정 확률로 펫 발견 활성화 상태("새로운 펫 발견!" 버튼)를 사용자에게 제공하고, 불가능할 시 숨깁니다.

---

## 2. 기술 구현 방식
- **주기적 출현 확률 체크**:
  메인 화면이 켜져 있는 동안 백그라운드 코루틴 `LaunchedEffect(Unit)` 내에서 10초 주기마다 루프가 실행되며, `Random.nextFloat() < 0.5f` 식을 계산하여 50% 확률로 펫 발견 상태 변수(`isPetFound`)를 판정합니다.
- **버튼 애니메이션 연출**:
  - 발견 상태가 활성화(`isPetFound == true`)되면 `AnimatedVisibility` 컴포넌트를 통해 페이드인 효과와 함께 보라/핑크빛 색상의 "✨ 새로운 펫 발견!" 버튼이 나타납니다.
  - 사용자 주목을 유도하기 위해 `rememberInfiniteTransition`을 활용하여 800ms 주기로 버튼 스케일을 1.0에서 1.06배로 역방향 반복 펄싱(Pulse)하는 특수 연출과 Sparkles 반짝이 아이콘을 결합하였습니다.
  - 발견 상태가 해제되면 버튼이 자동으로 페이드아웃 및 숨김 처리됩니다.
