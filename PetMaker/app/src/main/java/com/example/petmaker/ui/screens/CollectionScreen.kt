package com.example.petmaker.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.petmaker.data.local.PetDatabase
import com.example.petmaker.data.local.PetEntity
import androidx.lifecycle.asFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val petDao = remember { PetDatabase.getInstance(context).petDao() }
    val petListState = petDao.getAllPets().asFlow().collectAsState(initial = emptyList())
    val petList = petListState.value

    var selectedPet by remember { mutableStateOf<PetEntity?>(null) }
    
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
                    colors = listOf(Color(0xFF1A0C36), Color(0xFF0D061F))
                )
            ),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "펫 도감",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (filteredList.size == petList.size) "${petList.size}마리 수집" else "검색 결과 ${filteredList.size}마리 / 총 ${petList.size}마리",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
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
                                text = if (isEditMode) "완료" else "편집",
                                color = if (isEditMode) Color(0xFF00E676) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF120C24)
                )
            )
        },
        bottomBar = {
            if (isEditMode) {
                BottomAppBar(
                    containerColor = Color(0xFF160F2C),
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
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "삭제 (${selectedPets.size})", fontWeight = FontWeight.Bold)
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
            // 1. 검색 및 필터링 헤더
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 검색 입력창
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("이름 또는 설명으로 검색...", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = Color(0xFFE040FB),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color(0xFFE040FB)
                    )
                )

                // 날씨 필터 Row
                Text(
                    text = "날씨 필터",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val weatherFilters = listOf("전체", "맑음", "비", "눈", "흐림")
                    weatherFilters.forEach { filter ->
                        val isSelected = selectedWeatherFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFFE040FB) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedWeatherFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (filter) {
                                    "전체" -> "전체"
                                    "맑음" -> "☀️ 맑음"
                                    "비" -> "🌧️ 비"
                                    "눈" -> "❄️ 눈"
                                    "흐림" -> "☁️ 흐림"
                                    else -> filter
                                },
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // 시간대 필터 Row
                Text(
                    text = "시간대 필터",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val timezoneFilters = listOf("전체", "아침", "오후", "저녁", "밤")
                    timezoneFilters.forEach { filter ->
                        val isSelected = selectedTimezoneFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTimezoneFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = when (filter) {
                                    "전체" -> "전체"
                                    "아침" -> "🌅 아침"
                                    "오후" -> "☀️ 오후"
                                    "저녁" -> "🌇 저녁"
                                    "밤" -> "🌃 밤"
                                    else -> filter
                                },
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(top = 6.dp))

            // 2. 도감 리스트 본문
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (filteredList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎭",
                            fontSize = 80.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Text(
                            text = if (petList.isEmpty()) "아직 수집한 펫이 없어요" else "필터 조건에 맞는 펫이 없어요",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = if (petList.isEmpty()) "환경에 맞는 특별한 펫을 만나보세요!" else "검색어나 필터를 변경해 보세요",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList) { pet ->
                            val isSelected = selectedPets.contains(pet)
                            PetItemCard(
                                pet = pet,
                                isEditMode = isEditMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isEditMode) {
                                        if (isSelected) selectedPets.remove(pet) else selectedPets.add(pet)
                                    } else {
                                        selectedPet = pet
                                    }
                                },
                                onImageClick = {
                                    fullscreenImagePath = pet.imagePath
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. 단일 삭제 확인 모달
    if (showSingleDeleteConfirmation && petToDeleteByModal != null) {
        AlertDialog(
            onDismissRequest = { 
                showSingleDeleteConfirmation = false
                petToDeleteByModal = null 
            },
            title = { Text("펫 방생") },
            text = { Text("정말 '${petToDeleteByModal!!.name}'을(를) 자연으로 방생(삭제)하시겠습니까?") },
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
                                selectedPet = null // 상세창도 닫기
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("방생")
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

    // 2. 다중 선택 삭제 확인 모달
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("선택한 펫 방생") },
            text = { Text("선택한 ${selectedPets.size}마리의 펫을 도감에서 방생(삭제)하시겠습니까?") },
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
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
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

    // 3. 모달창 상세 정보 표시
    if (selectedPet != null) {
        DetailModal(
            pet = selectedPet!!,
            onDismiss = { selectedPet = null },
            onDelete = {
                petToDeleteByModal = selectedPet
                showSingleDeleteConfirmation = true
            }
        )
    }

    // 4. 전체화면 사진 보기 다이얼로그 표시
    if (fullscreenImagePath != null) {
        FullscreenImageDialog(
            imagePath = fullscreenImagePath!!,
            onDismiss = { fullscreenImagePath = null }
        )
    }
}

@Composable
fun PetItemCard(
    pet: PetEntity,
    isEditMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val formattedDate = DateFormat.format("yyyy. M. d.", pet.timestamp).toString()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 편집 모드 체크박스 표시
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFE040FB),
                        uncheckedColor = Color.White.copy(alpha = 0.6f),
                        checkmarkColor = Color.White
                    )
                )
            }

            // 원형 이미지 썸네일
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
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
                
                // 썸네일에 돋보기 아이콘 뱃지 오버레이
                if (!isEditMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .background(Color(0xFFE040FB), CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔍", fontSize = 10.sp)
                    }
                }
            }

            // 펫 정보 텍스트 영역
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = pet.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "🗓️ $formattedDate",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )

                // 2개 이하의 특성 뱃지 노출
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pet.traits.take(2).forEach { trait ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = trait,
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // 날씨 이모티콘 뱃지 매핑
                val weatherIcon = when {
                    pet.weather.contains("비") -> "🌧️"
                    pet.weather.contains("눈") -> "❄️"
                    pet.weather.contains("흐림") || pet.weather.contains("구름") -> "☁️"
                    else -> "☀️"
                }

                Text(
                    text = "$weatherIcon ${pet.temperature.toInt()}°C",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
