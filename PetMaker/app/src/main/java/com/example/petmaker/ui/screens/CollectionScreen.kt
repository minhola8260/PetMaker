package com.example.petmaker.ui.screens

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.data.local.PetEntity
import com.example.petmaker.data.local.ApiKeyManager
import com.example.petmaker.ui.components.RotatingSparkles
import com.example.petmaker.ui.components.StarParticleEffect
import androidx.lifecycle.asFlow
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    apiKeyManager: ApiKeyManager,
    autoOpenPartner: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val petDao = remember { PetDatabase.getInstance(context).petDao() }
    val petListState = petDao.getAllPets().asFlow().collectAsState(initial = emptyList())
    val petList = petListState.value

    var melonCandiesCount by remember { mutableStateOf(apiKeyManager.melonCandiesCount) }
    var premiumTicketsCount by remember { mutableStateOf(apiKeyManager.premiumTicketsCount) }

    val collectedCount = petList.size
    val pokedexLevel = remember(collectedCount) {
        var lvl = 1
        while (5 * (lvl + 1) * lvl <= collectedCount) {
            lvl++
        }
        lvl
    }

    var showGiftDialog by remember { mutableStateOf(false) }
    var giftChestState by remember { mutableStateOf("CLOSED") }
    var currentRewardLevel by remember { mutableStateOf(1) }

    var selectedPet by remember { mutableStateOf<PetEntity?>(null) }
    var hasAutoOpened by remember { mutableStateOf(false) }

    LaunchedEffect(petList, autoOpenPartner) {
        if (autoOpenPartner && !hasAutoOpened && selectedPet == null) {
            val partner = petList.find { it.isPartner }
            if (partner != null) {
                selectedPet = partner
                hasAutoOpened = true
            }
        }
    }
    
    // 편집 및 선택 기능 상태
    var isEditMode by remember { mutableStateOf(false) }
    val selectedPets = remember { mutableStateListOf<PetEntity>() }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSingleDeleteConfirmation by remember { mutableStateOf(false) }
    var petToDeleteByModal by remember { mutableStateOf<PetEntity?>(null) }
    var fullscreenImagePath by remember { mutableStateOf<String?>(null) }

    // 필터 및 검색 상태
    var searchQuery by remember { mutableStateOf("") }
    var selectedWeatherFilter by remember { mutableStateOf("전체") }
    var selectedTimezoneFilter by remember { mutableStateOf("전체") }

    // 필터 처리된 리스트
    val filteredList = remember(petList, searchQuery, selectedWeatherFilter, selectedTimezoneFilter) {
        petList.filter { pet ->
            val matchesSearch = pet.name.contains(searchQuery, ignoreCase = true) ||
                                pet.description.contains(searchQuery, ignoreCase = true)
            
            val matchesWeather = if (selectedWeatherFilter == "전체") {
                true
            } else {
                pet.weather.contains(selectedWeatherFilter)
            }
            
            val matchesTimezone = if (selectedTimezoneFilter == "전체") {
                true
            } else {
                val targetTimezoneEng = when (selectedTimezoneFilter) {
                    "아침" -> "Morning"
                    "오후" -> "Afternoon"
                    "저녁" -> "Evening"
                    "밤" -> "Night"
                    else -> ""
                }
                pet.timezone.contains(targetTimezoneEng, ignoreCase = true) ||
                pet.timezone.contains(selectedTimezoneFilter)
            }
            
            matchesSearch && matchesWeather && matchesTimezone
        }
    }

    // 모드 전환 시 선택 리스트 자동 클리어
    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            selectedPets.clear()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0624), Color(0xFF05020D))
                )
            ),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "모험가 도감",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (petList.isNotEmpty()) {
                        TextButton(
                            onClick = { isEditMode = !isEditMode }
                        ) {
                            Text(
                                text = if (isEditMode) "완료" else "방생 관리",
                                color = if (isEditMode) Color(0xFF00E676) else Color(0xFFE040FB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF130924).copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            if (isEditMode) {
                BottomAppBar(
                    containerColor = Color(0xFF130924),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (selectedPets.size == filteredList.size) {
                                    selectedPets.clear()
                                } else {
                                    selectedPets.clear()
                                    selectedPets.addAll(filteredList)
                                }
                            }
                        ) {
                            Text(
                                text = if (selectedPets.size == filteredList.size) "선택 해제" else "전체 선택",
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { showDeleteConfirmation = true },
                            enabled = selectedPets.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5252),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "선택 방생 (${selectedPets.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. 도감 완성도 대시보드 (게임화 경험치 바)
            PokedexDashboard(
                collectedCount = petList.size,
                melonCandiesCount = melonCandiesCount,
                premiumTicketsCount = premiumTicketsCount,
                apiKeyManager = apiKeyManager,
                onClaimReward = {
                    currentRewardLevel = pokedexLevel
                    giftChestState = "CLOSED"
                    showGiftDialog = true
                }
            )

            // 2. 검색 및 필터링 헤더
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("도감 펫 검색...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedBorderColor = Color(0xFFE040FB),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        cursorColor = Color(0xFFE040FB)
                    )
                )

                // 필터 스크롤러
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 날씨 대분류
                    Text(text = "기상:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    val weatherFilters = listOf("전체", "맑음", "비", "눈", "흐림")
                    weatherFilters.forEach { filter ->
                        val isSelected = selectedWeatherFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFFE040FB) else Color.White.copy(alpha = 0.07f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedWeatherFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when (filter) {
                                    "전체" -> "전체"
                                    "맑음" -> "☀️"
                                    "비" -> "🌧️"
                                    "눈" -> "❄️"
                                    "흐림" -> "☁️"
                                    else -> filter
                                },
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 시간대
                    Text(text = "시간:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    val timezoneFilters = listOf("전체", "아침", "오후", "저녁", "밤")
                    timezoneFilters.forEach { filter ->
                        val isSelected = selectedTimezoneFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFF00B0FF) else Color.White.copy(alpha = 0.07f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTimezoneFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(top = 8.dp))

            // 3. 도감 그리드 영역
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // 검색 및 필터 비활성화 상태일 때는 빈 락 슬롯 패딩을 주어 완성 의지 부여
                val showLockedSlots = searchQuery.isEmpty() && selectedWeatherFilter == "전체" && selectedTimezoneFilter == "전체"
                val displaySlotsCount = if (showLockedSlots) {
                    maxOf(12, ((filteredList.size + 2) / 3) * 3) // 3열 기준 올림처리, 최소 12슬롯
                } else {
                    filteredList.size
                }

                if (filteredList.isEmpty() && !showLockedSlots) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🔮", fontSize = 60.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "일치하는 펫이 없습니다",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "필터나 검색어를 변경해 보세요.",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else if (petList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "🥚", fontSize = 70.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "도감이 텅 비어 있습니다",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "바깥으로 모험을 떠나 소환 포탈에서 첫 펫을 소환해 보세요!",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displaySlotsCount) { index ->
                            if (index < filteredList.size) {
                                val pet = filteredList[index]
                                val isSelected = selectedPets.any { it.id == pet.id }
                                PetGridCard(
                                    pet = pet,
                                    index = index + 1,
                                    isEditMode = isEditMode,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isEditMode) {
                                            val existing = selectedPets.find { it.id == pet.id }
                                            if (existing != null) {
                                                selectedPets.remove(existing)
                                            } else {
                                                selectedPets.add(pet)
                                            }
                                        } else {
                                            selectedPet = pet
                                        }
                                    },
                                    onImageClick = {
                                        fullscreenImagePath = pet.imagePath
                                    }
                                )
                            } else {
                                LockedGridCard(index = index + 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. 단일 방생 확인 대화상자
    if (showSingleDeleteConfirmation && petToDeleteByModal != null) {
        AlertDialog(
            onDismissRequest = { 
                showSingleDeleteConfirmation = false
                petToDeleteByModal = null 
            },
            title = { Text("펫 자연 방생", fontWeight = FontWeight.Bold) },
            text = { Text("정말 '${petToDeleteByModal!!.name}'을(를) 자연으로 방생(삭제)하시겠습니까? 방생된 펫은 돌아오지 않습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = petToDeleteByModal
                        if (target != null) {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    petDao.deletePet(target)
                                }
                                showSingleDeleteConfirmation = false
                                petToDeleteByModal = null
                                selectedPet = null // 상세창 닫기
                                Toast.makeText(context, "펫이 넓은 자연으로 돌아갔습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("방생하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSingleDeleteConfirmation = false
                    petToDeleteByModal = null 
                }) {
                    Text("취소")
                }
            }
        )
    }

    // 2. 다중 방생 확인 대화상자
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("선택한 펫 방생", fontWeight = FontWeight.Bold) },
            text = { Text("선택한 ${selectedPets.size}마리의 펫을 도감에서 삭제하고 자연으로 돌려보낼까요?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                selectedPets.forEach { pet ->
                                    petDao.deletePet(pet)
                                }
                            }
                            selectedPets.clear()
                            isEditMode = false
                            showDeleteConfirmation = false
                            Toast.makeText(context, "펫들을 자연으로 방생하였습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("방생")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 3. 상세 정보 다이얼로그
    if (selectedPet != null) {
        val livePet = petList.find { it.id == selectedPet!!.id } ?: selectedPet!!
        DetailModal(
            pet = livePet,
            melonCandiesCount = melonCandiesCount,
            onMelonCandiesCountChanged = { newCount ->
                apiKeyManager.melonCandiesCount = newCount
                melonCandiesCount = newCount
            },
            onDismiss = { selectedPet = null },
            onDelete = {
                petToDeleteByModal = livePet
                showSingleDeleteConfirmation = true
            }
        )
    }

    // 4. 전체화면 보기
    if (fullscreenImagePath != null) {
        FullscreenImageDialog(
            imagePath = fullscreenImagePath!!,
            onDismiss = { fullscreenImagePath = null }
        )
    }

    // 5. 레벨업 선물 다이얼로그
    if (showGiftDialog) {
        LevelUpGiftDialog(
            level = currentRewardLevel,
            apiKeyManager = apiKeyManager,
            initialState = giftChestState,
            onRewardClaimed = { newCandies, newTickets ->
                melonCandiesCount = newCandies
                premiumTicketsCount = newTickets
            },
            onDismiss = { showGiftDialog = false }
        )
    }
}

// 도감 경험치/레벨 대시보드
@Composable
fun PokedexDashboard(
    collectedCount: Int,
    melonCandiesCount: Int,
    premiumTicketsCount: Int,
    apiKeyManager: ApiKeyManager,
    onClaimReward: () -> Unit
) {
    val maxSlots = 50
    val progress = (collectedCount.toFloat() / maxSlots).coerceIn(0f, 1f)
    val pokedexLevel = remember(collectedCount) {
        var lvl = 1
        while (5 * (lvl + 1) * lvl <= collectedCount) {
            lvl++
        }
        lvl
    }
    val levelProgress = remember(collectedCount, pokedexLevel) {
        val currentLevelReq = 5 * pokedexLevel * (pokedexLevel - 1)
        val nextLevelDiff = pokedexLevel * 10
        ((collectedCount - currentLevelReq).toFloat() / nextLevelDiff).coerceIn(0f, 1f)
    }
    val hasUnclaimedReward = pokedexLevel > apiKeyManager.lastClaimedPokedexLevel

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF160F2C).copy(alpha = 0.8f)
            ),
            border = BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 도감 원형 완성도
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFE040FB),
                        trackColor = Color.White.copy(alpha = 0.08f),
                        strokeWidth = 5.dp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 도감 레벨 & 칭호 & 스펙
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "도감 LV.$pokedexLevel",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when (pokedexLevel) {
                                    in 1..2 -> "초보 관찰자"
                                    in 3..5 -> "중급 훈련사"
                                    in 6..10 -> "숙련된 탐험가"
                                    else -> "그랜드 마스터"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // 미수령 보상이 있을 때만 선물 아이콘 표시
                        if (hasUnclaimedReward) {
                            IconButton(
                                onClick = onClaimReward,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = "🎁",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 도감 완성 수 게이지 바 (다음 레벨로 가는 진행도)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(levelProgress)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFE040FB), Color(0xFF00B0FF))
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "레벨업까지: ${collectedCount - (5 * pokedexLevel * (pokedexLevel - 1))} / ${pokedexLevel * 10} 마리 (총 ${collectedCount}마리 발견)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // 보유 재화 목록 표기
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.06f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🍬", fontSize = 16.sp)
                    Text(
                        text = "멜론 사탕: ${melonCandiesCount}개",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🎫", fontSize = 16.sp)
                    Text(
                        text = "프리미엄 소환권: ${premiumTicketsCount}개",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// 획득한 펫 카드
@Composable
fun PetGridCard(
    pet: PetEntity,
    index: Int,
    isEditMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val elementColor = remember(pet.weather) {
        when {
            pet.weather.contains("비") -> Color(0xFF00B0FF)
            pet.weather.contains("눈") -> Color(0xFF00E5FF)
            pet.weather.contains("흐림") || pet.weather.contains("구름") -> Color(0xFF90A4AE)
            else -> Color(0xFFFF9100)
        }
    }

    val indexText = String.format(Locale.US, "#%03d", index)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A132C).copy(alpha = 0.9f)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isSelected && isEditMode) {
                Color(0xFFFF5252)
            } else {
                elementColor.copy(alpha = 0.5f)
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 도감 인덱스 번호 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = indexText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    
                    // 날씨 및 파트너 아이콘 미니 뱃지
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (pet.isPartner) {
                            Text(text = "👑", fontSize = 10.sp)
                        }
                        Text(
                            text = when {
                                pet.weather.contains("비") -> "🌧️"
                                pet.weather.contains("눈") -> "❄️"
                                pet.weather.contains("흐림") || pet.weather.contains("구름") -> "☁️"
                                else -> "☀️"
                            },
                            fontSize = 10.sp
                        )
                    }
                }

                // 펫 썸네일 (원형 액자)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .then(
                            if (!isEditMode) {
                                Modifier.clickable { onImageClick() }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = pet.imagePath,
                        contentDescription = "Pet Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // 이름 표시
                Text(
                    text = pet.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // 삭제용 체크박스 오버레이
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) Color(0xFFFF5252).copy(alpha = 0.12f) else Color.Transparent
                        )
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.size(24.dp).align(Alignment.TopStart),
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFFF5252),
                            uncheckedColor = Color.White.copy(alpha = 0.5f),
                            checkmarkColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

// 미수집된 물음표 락 카드
@Composable
fun LockedGridCard(index: Int) {
    val indexText = String.format(Locale.US, "#%03d", index)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF130D22).copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            1.dp,
            Brush.sweepGradient(listOf(Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0.08f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = indexText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.15f)
                )
            }

            // 미지수 락 아이콘 실루엣
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.15f)
                )
            }

            Text(
                text = "???",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.2f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LevelUpGiftDialog(
    level: Int,
    apiKeyManager: ApiKeyManager,
    initialState: String = "CLOSED",
    onRewardClaimed: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var chestState by remember { mutableStateOf(initialState) } // "CLOSED", "SHAKING", "OPENED"
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 흔들림 애니메이션용 rotation
    val rotation = remember { Animatable(0f) }
    // 맥박(Pulse) 스케일 애니메이션용
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 흔들림 애니메이션 효과 실행
    LaunchedEffect(chestState) {
        if (chestState == "SHAKING") {
            // 흔들림 연출 (좌우로 흔들림)
            repeat(4) {
                rotation.animateTo(15f, animationSpec = tween(70, easing = LinearEasing))
                rotation.animateTo(-15f, animationSpec = tween(70, easing = LinearEasing))
            }
            rotation.animateTo(0f, animationSpec = tween(50))
            chestState = "OPENED"
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (chestState == "OPENED") {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = chestState == "OPENED",
            dismissOnClickOutside = chestState == "OPENED"
        ),
        containerColor = Color(0xFF130924).copy(alpha = 0.95f),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (chestState == "OPENED") "🎉 레벨업 보상 획득!" else "🎁 도감 LV.$level 달성 축하 선물!",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (chestState != "OPENED") {
                    Text(
                        text = "보물상자를 터치하여 개봉해 보세요!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(150.dp)
                            .scale(if (chestState == "CLOSED") pulseScale else 1f)
                            .rotate(rotation.value)
                            .clickable {
                                if (chestState == "CLOSED") {
                                    chestState = "SHAKING"
                                }
                            }
                    ) {
                        RotatingSparkles(modifier = Modifier.size(150.dp))
                        Text(
                            text = "👑",
                            fontSize = 80.sp
                        )
                    }
                } else {
                    Text(
                        text = "도감 성장을 축하하는 특별한 선물이 지급되었습니다!",
                        color = Color(0xFFFFE082),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        StarParticleEffect(modifier = Modifier.fillMaxSize())
                        Text(
                            text = "🔓",
                            fontSize = 70.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                    ) {
                        RewardItemCard(icon = "🍬", name = "멜론 사탕", count = "x10")
                        if (level % 5 == 0) {
                            RewardItemCard(icon = "🎫", name = "프리미엄 소환권", count = "x1")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (chestState == "OPENED") {
                Button(
                    onClick = {
                        if (level > apiKeyManager.lastClaimedPokedexLevel) {
                            apiKeyManager.melonCandiesCount += 10
                            var ticketsAdded = 0
                            if (level % 5 == 0) {
                                apiKeyManager.premiumTicketsCount += 1
                                ticketsAdded = 1
                            }
                            apiKeyManager.lastClaimedPokedexLevel = level
                            onRewardClaimed(apiKeyManager.melonCandiesCount, apiKeyManager.premiumTicketsCount)
                            val toastMsg = if (ticketsAdded > 0) {
                                "멜론 사탕 10개와 프리미엄 소환권 1개가 지급되었습니다! 🎁"
                            } else {
                                "멜론 사탕 10개가 지급되었습니다! 🍬"
                            }
                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "이미 해당 레벨의 보상을 수령하셨습니다.", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE040FB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("확인", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    )
}

@Composable
fun RewardItemCard(icon: String, name: String, count: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E133C)
        ),
        border = BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(110.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = name, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Text(text = count, fontSize = 14.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
        }
    }
}
