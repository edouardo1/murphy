package com.example.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.flow.StateFlow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.db.MediaItem
import com.example.viewmodel.GenerationState
import com.example.viewmodel.StoryboardShot
import com.example.viewmodel.VideoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BackgroundAtmosphere() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1115))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4F378B).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f),
                    radius = size.width * 1.1f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF21005D).copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.9f),
                    radius = size.width * 1.2f
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainView(viewModel: VideoViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Bottom navigation/tab state
    var selectedTab by remember { mutableStateOf(0) }
    
    // View states from ViewModel
    val videoIdea by viewModel.videoIdea.collectAsState()
    val selectedMood by viewModel.selectedMood.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()
    val referenceImageUri by viewModel.referenceImageUri.collectAsState()
    val referenceImageBitmap by viewModel.referenceImageBitmap.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val activeMediaItem by viewModel.activeMediaItem.collectAsState()
    val history by viewModel.historyState.collectAsState()

    // Setup photo picker launcher
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setReferenceImage(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundAtmosphere()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "CINEGEN STUDIO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFFD0BCFF)
                                )
                            )
                            Text(
                                text = "Visual Director",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    color = Color(0xFFE2E2E6)
                                )
                            )
                        }
                    },
                    actions = {
                        if (viewModel.isApiKeyPlaceholder) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 16.dp)
                            ) {
                                Text(
                                    text = "NO API KEY",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = { /* Help / Settings placeholder */ },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .border(1.dp, Color(0xFF49454F).copy(alpha = 0.6f), CircleShape)
                                .background(Color(0xFF1C1B1F).copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFE2E2E6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = Color(0xFF0F1115).copy(alpha = 0.85f),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.VideoCall, contentDescription = "Studio") },
                        label = { Text("Studio") },
                        modifier = Modifier.testTag("tab_studio"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF381E72),
                            selectedTextColor = Color(0xFFD0BCFF),
                            indicatorColor = Color(0xFFD0BCFF),
                            unselectedIconColor = Color(0xFF938F99),
                            unselectedTextColor = Color(0xFF938F99)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.History, contentDescription = "Library") },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("tab_library"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF381E72),
                            selectedTextColor = Color(0xFFD0BCFF),
                            indicatorColor = Color(0xFFD0BCFF),
                            unselectedIconColor = Color(0xFF938F99),
                            unselectedTextColor = Color(0xFF938F99)
                        )
                    )
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = Color.Transparent
            ) {
            when (selectedTab) {
                0 -> {
                    // Studio page
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        // Global warning banner if API key is placeholder
                        if (viewModel.isApiKeyPlaceholder) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Missing Key",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "API Key Required",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel. Running in simulation mode without it.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Presets Row
                        item {
                            Column {
                                Text(
                                    text = "Ready Templates",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PresetPill(
                                        label = "Movie Scene",
                                        icon = Icons.Default.Movie,
                                        testTag = "preset_cinematic_button",
                                        onClick = { viewModel.applyPreset("Cinematic Movie Scene") }
                                    )
                                    PresetPill(
                                        label = "YouTube Hook",
                                        icon = Icons.Default.TrendingUp,
                                        testTag = "preset_youtube_button",
                                        onClick = { viewModel.applyPreset("YouTube Short Hook") }
                                    )
                                    PresetPill(
                                        label = "Emotional Story",
                                        icon = Icons.Default.Favorite,
                                        testTag = "preset_emotional_button",
                                        onClick = { viewModel.applyPreset("Emotional Story") }
                                    )
                                }
                            }
                        }

                        // Input field: Video Idea
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1C1B1F).copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Video Idea",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD0BCFF),
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = videoIdea,
                                        onValueChange = { viewModel.videoIdea.value = it },
                                        placeholder = {
                                            Text(
                                                text = "Describe your video idea (e.g. A woman walking in a futuristic neon city at night)",
                                                fontSize = 14.sp,
                                                color = Color(0xFF938F99)
                                            )
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE2E2E6)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .testTag("video_idea_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F1115).copy(alpha = 0.6f),
                                            unfocusedContainerColor = Color(0xFF0F1115).copy(alpha = 0.6f),
                                            focusedBorderColor = Color(0xFFD0BCFF),
                                            unfocusedBorderColor = Color(0xFF49454F)
                                        )
                                    )
                                }
                            }
                        }

                        // Attributes Dropdowns Row
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1C1B1F).copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Cinematic Directives",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD0BCFF),
                                            letterSpacing = 0.5.sp
                                        )
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Mood Selection
                                        DropdownSelector(
                                            label = "Mood",
                                            selectedValue = selectedMood,
                                            options = listOf("Cinematic", "Dark", "Emotional", "Happy", "Dramatic", "Suspenseful", "Inspirational"),
                                            testTag = "mood_dropdown",
                                            onValueSelected = { viewModel.selectedMood.value = it }
                                        )

                                        // Style Selection
                                        DropdownSelector(
                                            label = "Visual Style",
                                            selectedValue = selectedStyle,
                                            options = listOf("Ultra-realistic", "Photorealistic cinematic", "Anime style", "3D animation", "Documentary style", "Sci-fi futuristic", "Vintage film"),
                                            testTag = "style_dropdown",
                                            onValueSelected = { viewModel.selectedStyle.value = it }
                                        )

                                        // Duration Selection
                                        DropdownSelector(
                                            label = "Duration Target",
                                            selectedValue = selectedDuration,
                                            options = listOf("15 seconds", "30 seconds", "60 seconds"),
                                            testTag = "duration_dropdown",
                                            onValueSelected = { viewModel.selectedDuration.value = it }
                                        )
                                    }
                                }
                            }
                        }

                        // Optional Image input
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1C1B1F).copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Reference Image (Optional)",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD0BCFF),
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF0F1115).copy(alpha = 0.6f))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFF49454F),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    pickMediaLauncher.launch(
                                                        PickVisualMediaRequest(
                                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                                        )
                                                    )
                                                }
                                                .testTag("reference_image_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (referenceImageBitmap != null) {
                                                Image(
                                                    bitmap = referenceImageBitmap!!.asImageBitmap(),
                                                    contentDescription = "Reference",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = "Add image",
                                                    tint = Color(0xFFD0BCFF).copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(
                                                text = if (referenceImageUri != null) "Reference uploaded" else "Add Reference Image",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE2E2E6))
                                            )
                                            Text(
                                                text = "Will guide characters, composition, layout structure, color grading.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF938F99)
                                            )
                                        }

                                        if (referenceImageUri != null) {
                                            IconButton(
                                                onClick = { viewModel.setReferenceImage(null) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear image",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons: Generate Video & Regenerate
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.generateVideo(isRegenerate = false) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("generate_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD0BCFF),
                                        contentColor = Color(0xFF381E72)
                                    )
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Magic", tint = Color(0xFF381E72))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Generate Cinematic Story",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    )
                                }

                                if (activeMediaItem != null) {
                                    OutlinedButton(
                                        onClick = { viewModel.generateVideo(isRegenerate = true) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("regenerate_button"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFF4A4458),
                                            contentColor = Color(0xFFE8DEF8)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFF49454F))
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Again", tint = Color(0xFFE8DEF8))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Regenerate Variations",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE8DEF8))
                                        )
                                    }
                                }
                            }
                        }

                        // Show compilation state & previews
                        item {
                            AnimatedVisibility(
                                visible = activeMediaItem != null && generationState !is GenerationState.Loading,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                activeMediaItem?.let { item ->
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        
                                        Text(
                                            text = "Active Generation Workspace",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )

                                        // Render the custom preview player
                                        CinematicPreviewPlayer(
                                            item = item,
                                            storyboard = viewModel.parseStoryboardFromItem(item),
                                            onDownloadSimulated = {
                                                Toast.makeText(context, "Asset compiled and saved to Gallery successfully!", Toast.LENGTH_LONG).show()
                                            }
                                        )

                                        // Expandable Compilation stats/prompts details
                                        NodeDetailsBlock(item = item)
                                    }
                                }
                            }
                        }

                        // State Handler overlay representations
                        item {
                            when (val state = generationState) {
                                is GenerationState.Loading -> {
                                    CinematicRenderProgress(status = state.status)
                                }
                                is GenerationState.Error -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Generation Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
                1 -> {
                    // Library page
                    LibraryScreen(
                        history = history,
                        onSelectItem = { item ->
                            viewModel.viewMediaItem(item)
                            selectedTab = 0
                        },
                        onDeleteItem = { item ->
                            viewModel.deleteMediaItem(item)
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun PresetPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    InputChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(icon, contentDescription = label, modifier = Modifier.size(14.dp)) },
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
fun DropdownSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    testTag: String,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CinematicRenderProgress(status: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "Rotator")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                // Pulse halo ring
                val scaleFactor by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                Canvas(modifier = Modifier.size(80.dp).scale(scaleFactor)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFB300).copy(alpha = 0.15f),
                                Color(0xFFFFB300).copy(alpha = 0f)
                            )
                        )
                    )
                }

                // Rotating film reel icon
                Icon(
                    imageVector = Icons.Default.FilterFrames,
                    contentDescription = "Processing Reel",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer(rotationZ = rotationAngle)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rendering Studio Model",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CinematicPreviewPlayer(
    item: MediaItem,
    storyboard: List<StoryboardShot>,
    onDownloadSimulated: () -> Unit
) {
    val durationSec = when (item.duration) {
        "15 seconds" -> 15f
        "30 seconds" -> 30f
        "60 seconds" -> 60f
        else -> 15f
    }

    var isPlaying by remember { mutableStateOf(true) }
    var playProgress by remember { mutableStateOf(0f) }

    // Coroutine standard clock tracker
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(100)
                if (playProgress < durationSec) {
                    playProgress += 0.1f
                } else {
                    playProgress = 0f // Loop
                }
            }
        }
    }

    // Infinite transitions for Ken Burns visual effect
    val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
    val panScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_scale"
    )
    val panOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = SineTransitionSpec),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_offset"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Rendered image with subtle pan/scale
            if (item.imagePath != null) {
                val imageFile = File(item.imagePath)
                if (imageFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Compiled Render Frame",
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(if (isPlaying) panScale else 1.05f)
                            .graphicsLayer(
                                translationX = if (isPlaying) panOffset else 0f,
                                translationY = if (isPlaying) panOffset * 0.4f else 0f
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MovieCreation, contentDescription = "Asset", tint = Color.LightGray)
                }
            }

            // High aesthetic cinematic overlays
            // 1. Top Bar overlay (Rec indicator & Visual style badge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color.Red else Color.LightGray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "REC" else "PAUS",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.visualStyle.uppercase(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // 2. Audio Visualizer lines overlay (bottom aspect)
            if (isPlaying) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 48.dp)
                        .height(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(6) { index ->
                        val duration = remember { (400..800).random() }
                        val animHeight by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(duration, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar_$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight(animHeight)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // 3. Subtitles/Script dialogue representation (dynamic depending on timeline)
            val activeShotIndex = (playProgress / durationSec * storyboard.size).toInt().coerceIn(0, storyboard.size - 1)
            val currentShot = storyboard.getOrNull(activeShotIndex)
            
            if (currentShot != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "[CAMERA: ${currentShot.cameraMovement}] - ${currentShot.visualDescription}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 4. Integrated Player bottom controls card (translucent)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Column {
                    // Seek line
                    Slider(
                        value = playProgress,
                        onValueChange = { playProgress = it },
                        valueRange = 0f..durationSec,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .testTag("slider_player"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play Control",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Elapsed indicator
                            val elapsedMin = (playProgress.toInt()) / 60
                            val elapsedSec = (playProgress.toInt()) % 60
                            val totalMin = (durationSec.toInt()) / 60
                            val totalSec = (durationSec.toInt()) % 60
                            Text(
                                text = String.format("%02d:%02d / %02d:%02d", elapsedMin, elapsedSec, totalMin, totalSec),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Options Drawer inside preview
                        IconButton(
                            onClick = onDownloadSimulated,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download rendering",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Storyboard Timeline Display
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Director's Frame Sequence (${storyboard.size} Shots)",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        storyboard.forEach { shot ->
            val isActive = isPlaying && ((playProgress / durationSec * storyboard.size).toInt() == shot.id - 1)
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isActive) 1.dp else 0.dp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${shot.id}",
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shot.title,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = shot.visualDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Camera: ${shot.cameraMovement}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NodeDetailsBlock(item: MediaItem) {
    var expandedNode1 by remember { mutableStateOf(false) }
    var expandedNode2 by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Node 1
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedNode1 = !expandedNode1 }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Node 1: Compiled Layout Prompt",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (expandedNode1) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expandedNode1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = item.node1Prompt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Node 2
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedNode2 = !expandedNode2 }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Node 2: Refined Cinematic Prompt",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (expandedNode2) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expandedNode2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = item.node2RefinedPrompt,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    history: List<MediaItem>,
    onSelectItem: (MediaItem) -> Unit,
    onDeleteItem: (MediaItem) -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Empty",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Generations Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Go to the Studio tab to compile and refine your first cinematic AI storyboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Cinematic Film Reels",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectItem(item) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.DarkGray)
                            ) {
                                if (item.imagePath != null) {
                                    val imgFile = File(item.imagePath)
                                    if (imgFile.exists()) {
                                        AsyncImage(
                                            model = imgFile,
                                            contentDescription = "Frame preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = item.duration.replace(" seconds", "s"),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = item.videoIdea,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.mood} • ${item.visualStyle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { onDeleteItem(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val SineTransitionSpec = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)
