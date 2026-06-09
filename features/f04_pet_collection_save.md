# f04-펫 수집 및 영구 저장

- **관련 요구사항**: `FR-04`
- **관련 파일**: [ResultScreen.kt](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/ui/screens/ResultScreen.kt), [PetEntity.java](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/PetEntity.java), [PetDao.java](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/PetDao.java), [PetDatabase.java](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/PetDatabase.java)

---

## 1. 동작 설명
사용자는 생성 완료된 AI 펫 정보와 이미지 리소스를 기기 로컬 데이터베이스 및 앱 전용 보안 디렉토리에 영구 보관할 수 있으며, 소장하지 않고 즉시 뒤로가기(방생)를 수행할 수 있습니다.

---

## 2. 기술 구현 방식
- **이미지 리소스 이전 및 보관**:
  임시 캐시 디렉토리에 위치해 있던 WebP 파일을 앱 샌드박스의 영구 파일 보관소 디렉토리(`context.filesDir`)인 `pet_image_<timestamp>.webp` 경로로 자바 파일 스트림 복사(I/O) 처리를 백그라운드 코루틴에서 실행해 이전시킵니다.
- **Room 데이터베이스 적재**:
  이전 완료된 영구 절대 경로 문자열을 포함하고 펫 이름, 외형 설명, 성격, 특성 태그, 생성 일시 타임스탬프, 그리고 생성 시의 날씨, 기온, 시간대, 주소를 매핑하여 [PetEntity](file:///c:/Users/User/Documents/Mobile/PetMaker/app/src/main/java/com/example/petmaker/data/local/PetEntity.java) 객체를 구축하고 `insertPet` DAO 쿼리를 통해 로컬 SQLite DB에 트랜잭션 삽입합니다.
- **화면 라우팅 복귀**:
  DB 저장이 성공하면 `popUpTo("result") { inclusive = true }` 옵션과 함께 메인 화면으로 리디렉션하며, "다음에 만나기" 취소 버튼 클릭 시에는 DB 삽입 없이 캐시 정리 후 안전하게 메인으로 화면 복귀합니다.
