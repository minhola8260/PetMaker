package com.example.petmaker.data.local

/**
 * 앱 전역에서 공유되는 상수 및 유틸리티 함수 모음.
 */
object PetConstants {

    /** 이름 중복/규칙 위반 시 폴백으로 사용하는 한글 이름 후보 목록 */
    val FALLBACK_KOREAN_NAMES = listOf(
        "몽실이", "꼬미", "초롱이", "하늘이", "둥실이", "뽀짝이", "방울이", "아롱이", "다롱이",
        "뭉치", "토리", "코코", "보리", "두부", "호두", "망고", "포동이", "별이", "달이",
        "구름이", "단풍이", "이슬이", "샛별이", "소리", "가람이", "나래", "다솜", "라온",
        "마루", "바람이", "아라", "온누리", "한울", "새론", "봄이", "여름이", "가을이",
        "겨울이", "솔이", "송이", "나비", "방글이", "튼튼이", "씽씽이", "희망이", "사랑이"
    )

    /**
     * 기존 이름 목록과 중복되지 않는 고유 이름을 반환합니다.
     *
     * @param candidate   먼저 시도할 이름 (null 이면 폴백 목록에서 무작위 선택)
     * @param existingNames 이미 사용 중인 이름 목록 (대소문자 구분 없음)
     * @return 중복되지 않는 고유 한글 이름
     */
    fun generateUniqueName(candidate: String? = null, existingNames: List<String>): String {
        val existingSet = existingNames.map { it.lowercase() }.toSet()
        val base = candidate?.takeIf { it.isNotEmpty() } ?: FALLBACK_KOREAN_NAMES.random()

        if (!existingSet.contains(base.lowercase())) return base

        for (name in FALLBACK_KOREAN_NAMES.shuffled()) {
            if (!existingSet.contains(name.lowercase())) return name
        }

        // 후보를 모두 소진한 경우 랜덤 한글 글자 suffix 추가
        val randomChar = ('가'..'힣').random()
        return "$base$randomChar"
    }

    /**
     * 한글 날씨 설명 → OpenWeatherMap main 분류 영문 문자열 변환.
     */
    fun weatherDescToMain(desc: String): String = when {
        desc.contains("비") -> "Rain"
        desc.contains("눈") -> "Snow"
        desc.contains("흐림") || desc.contains("구름") || desc.contains("안개") -> "Clouds"
        else -> "Clear"
    }
}
