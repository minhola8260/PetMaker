# f06-날씨별 배경 애니메이션

- **관련 요구사항**: `FR-06`
- **관련 파일**: [WeatherBackground.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/components/WeatherBackground.kt)

---

## 1. 동작 설명
시스템은 실시간 날씨 유형에 따라 메인 화면 배경에 다채로운 날씨 테마 컬러 그라디언트를 연출하고 특수 입자 기상 애니메이션(빗방울, 눈송이)을 화면 전역에 가동하여 사실감을 높입니다.

---

## 2. 기술 구현 방식
- **그라디언트 및 부드러운 전환**:
  날씨 상태(CLEAR, CLOUDS, RAIN, SNOW)에 따라 배경색을 1초 간격의 `animateColorAsState` 크로스페이드 트랜지션 애니메이션을 통해 화면 배경 컬러를 스무스하게 보간 변경합니다.
- **비(RAIN) 입자 물리 애니메이션**:
  20개의 빗방울 개별 좌표(`Raindrop` 데이터 클래스) X, Y축 벡터 값을 코루틴 `withFrameMillis` 주기 호출마다 수직 하강 가산 처리하며, 기기 하단 경계선(2200f) 돌파 시 상단 Y축 좌표로 무작위 X축 위치와 함께 재할당하여 무한 낙하하는 비주얼을 나타냅니다.
- **눈(SNOW) 입자 흩날림 애니메이션**:
  30개의 원형 입자 눈송이(`Snowflake` 데이터 클래스) 하강 연산 시 삼각함수 `Math.sin(flake.angle) * 1.2f` 주기를 연속 변동시켜 좌우로 불규칙하게 바람에 흩날리며 떨어지는 입체적인 낙하 시뮬레이션을 구현했습니다.
