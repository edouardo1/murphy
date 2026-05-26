package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.ImageConfig
import com.example.api.InlineData
import com.example.api.Part
import com.example.api.ResponseFormat
import com.example.api.RetrofitClient
import com.example.db.MediaItem
import com.example.db.VideoDatabase
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

@JsonClass(generateAdapter = true)
data class StoryboardShot(
    val id: Int,
    val title: String,
    val durationSec: Int,
    val visualDescription: String,
    val cameraMovement: String
)

@JsonClass(generateAdapter = true)
data class VideoPromptModel(
    val node1Prompt: String,
    val node2RefinedPrompt: String,
    val storyboard: List<StoryboardShot>
)

sealed class GenerationState {
    object Idle : GenerationState()
    data class Loading(val status: String) : GenerationState()
    data class Success(val item: MediaItem, val storyboard: List<StoryboardShot>) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

class VideoViewModel(private val context: Context) : ViewModel() {

    private val database = VideoDatabase.getDatabase(context)
    private val dao = database.mediaItemDao()

    val historyState: StateFlow<List<MediaItem>> = dao.getAllMediaItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Input states
    val videoIdea = MutableStateFlow("")
    val selectedMood = MutableStateFlow("Cinematic")
    val selectedStyle = MutableStateFlow("Ultra-realistic")
    val selectedDuration = MutableStateFlow("15 seconds")
    val referenceImageUri = MutableStateFlow<Uri?>(null)
    val referenceImageBitmap = MutableStateFlow<Bitmap?>(null)

    // Execution state
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // Currently playing/viewed media item state
    private val _activeMediaItem = MutableStateFlow<MediaItem?>(null)
    val activeMediaItem: StateFlow<MediaItem?> = _activeMediaItem.asStateFlow()

    private val moshi = Moshi.Builder()
        .build()

    // Key Validation
    val isApiKeyPlaceholder: Boolean by lazy {
        val key = BuildConfig.GEMINI_API_KEY
        key.isBlank() || key == "MY_GEMINI_API_KEY" || key.contains("PLACEHOLDER")
    }

    fun applyPreset(presetType: String) {
        when (presetType) {
            "Cinematic Movie Scene" -> {
                videoIdea.value = "A cyber-detective standing in a rain-slicked alleyways of futuristic Neo-Tokyo under a giant holographic billboard"
                selectedMood.value = "Dramatic"
                selectedStyle.value = "Photorealistic cinematic"
                selectedDuration.value = "15 seconds"
            }
            "YouTube Short Hook" -> {
                videoIdea.value = "A close-up high-speed slow-motion tracking shot of hot glowing sparks flying off a cosmic blacksmith anvil as a glowing sword is forged"
                selectedMood.value = "Inspirational"
                selectedStyle.value = "Sci-fi futuristic"
                selectedDuration.value = "15 seconds"
            }
            "Emotional Story" -> {
                videoIdea.value = "An elderly astronaut gazing fondly out of the cabin dome window at a blue star nursery nebula, clutching a dusty photograph"
                selectedMood.value = "Emotional"
                selectedStyle.value = "Ultra-realistic"
                selectedDuration.value = "30 seconds"
            }
        }
    }

    fun setReferenceImage(uri: Uri?) {
        referenceImageUri.value = uri
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                referenceImageBitmap.value = bitmap
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Failed to load reference image bitmap", e)
                referenceImageBitmap.value = null
            }
        } else {
            referenceImageBitmap.value = null
        }
    }

    fun viewMediaItem(item: MediaItem) {
        _activeMediaItem.value = item
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            dao.deleteMediaItem(item.id)
            if (_activeMediaItem.value?.id == item.id) {
                _activeMediaItem.value = null
            }
            // Delete image file from cache if exists
            item.imagePath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("VideoViewModel", "Failed to delete image file $path", e)
                }
            }
        }
    }

    fun generateVideo(isRegenerate: Boolean = false) {
        if (videoIdea.value.trim().isEmpty()) {
            _generationState.value = GenerationState.Error("Please enter a Video Idea prompt first!")
            return
        }

        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("Initializing semantic prompt compiler...")
            val apiKey = BuildConfig.GEMINI_API_KEY

            if (isApiKeyPlaceholder) {
                _generationState.value = GenerationState.Error("API Key is missing! Please configure GEMINI_API_KEY inside the Secrets panel.")
                return@launch
            }

            try {
                // Compile the Node 1 structure as the primary prompt layout
                val initialIdea = videoIdea.value
                val mood = selectedMood.value
                val style = selectedStyle.value
                val duration = selectedDuration.value
                val hasRefImage = referenceImageBitmap.value != null

                val compiledPromptNode1 = """
                    Create a high-quality cinematic video scene.
                    Scene idea: $initialIdea
                    Mood: $mood
                    Visual style: $style
                    Details:
                    Highly detailed environment
                    Natural realistic motion
                    Strong storytelling composition
                    Dynamic camera movement (tracking shots, close-ups, wide angles)
                    Realistic lighting and shadows
                    Depth of field and motion blur
                    ${if (hasRefImage) "If a reference image is provided, use it to guide composition, subject appearance, and color grading colorways." else ""}
                    Output a concise, vivid video generation prompt optimized.
                """.trimIndent()

                val systemInstructionText = """
                    You are a cinematic film director and AI generator. Return a JSON structure ONLY.
                    You will receive a request instructing you to create and refine cinematic prompts.
                    Return a JSON object matching this schema exactly:
                    {
                      "node1Prompt": "Compile the raw combined input prompt here based on standard scene parameters",
                      "node2RefinedPrompt": "Optimized refined cinematic video prompt: much shorter, includes camera directions, emphasizes realistic motion, extremely cinematic and visually descriptive",
                      "storyboard": [
                        {
                          "id": 1,
                          "title": "Scene 1: Opening Shot",
                          "durationSec": 5,
                          "visualDescription": "Detailed visual description of the opening sequence frame",
                          "cameraMovement": "Low angle slow dolly-in tracking shot"
                        }
                      ]
                    }
                    Fill the storyboard array with 3 to 4 sequential shots mapping the timeline of the total duration ($duration).
                    Ensure your response is valid RFC-compliant JSON with no surrounding text outside the JSON block.
                """.trimIndent()

                // Call Gemini 3.5 Flash to generate the refined camera directions and storyboard JSON
                _generationState.value = GenerationState.Loading("Compiling prompts and storyboard layout...")

                val promptParts = mutableListOf<Part>()
                promptParts.add(Part(text = "Please compile and refine this video concept: \n$compiledPromptNode1"))
                
                // Add optional reference image as inlineData
                referenceImageBitmap.value?.let { bmp ->
                    val base64Bmp = withContext(Dispatchers.Default) {
                        bmp.toBase64()
                    }
                    promptParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Bmp)))
                }

                val requestTextModel = GenerateContentRequest(
                    contents = listOf(Content(parts = promptParts)),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
                    generationConfig = GenerationConfig(
                        temperature = 0.7f,
                        responseFormat = ResponseFormat(responseMimeType = "application/json")
                    )
                )

                val responseText = RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = requestTextModel
                )

                val rawJsonText = responseText.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("Empty text response from Gemini prompt generator.")

                val cleanedJson = cleanJson(rawJsonText)
                val parsedModel = withContext(Dispatchers.Default) {
                    try {
                        moshi.adapter(VideoPromptModel::class.java).fromJson(cleanedJson)
                    } catch (e: Exception) {
                        Log.e("VideoViewModel", "Failed to parse JSON, raw text is: $cleanedJson", e)
                        null
                    }
                } ?: throw Exception("JSON Storyboard parsing error. Trying again.")

                // Call Gemini 2.5 Flash Image to render a stunning high-fidelity cover frame/keyframe
                _generationState.value = GenerationState.Loading("Simulating cinematic lighting & camera tracking frames...")

                val imagePrompt = "Cinematic shot matching style '${style}'. Mood: '${mood}'. ${parsedModel.node2RefinedPrompt}. High fidelity, photorealistic cinematography, volumetric lighting, epic composition, unreal engine rendering, masterpiece."
                
                val imageRequest = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = imagePrompt)))),
                    generationConfig = GenerationConfig(
                        temperature = 0.8f,
                        imageConfig = ImageConfig(aspectRatio = "16:9", imageSize = "1K"),
                        responseModalities = listOf("TEXT", "IMAGE")
                    )
                )

                val responseImage = RetrofitClient.service.generateContent(
                    model = "gemini-2.5-flash-image",
                    apiKey = apiKey,
                    request = imageRequest
                )

                // Get base64 bytes of generated image
                var base64ImageBytes: String? = null
                val imageCandidateParts = responseImage.candidates.firstOrNull()?.content?.parts
                if (imageCandidateParts != null) {
                    for (part in imageCandidateParts) {
                        if (part.inlineData != null && part.inlineData.mimeType.startsWith("image/")) {
                            base64ImageBytes = part.inlineData.data
                            break
                        }
                    }
                }

                if (base64ImageBytes == null) {
                    throw Exception("No rendered image bytes returned by Gemini model.")
                }

                _generationState.value = GenerationState.Loading("Synchronizing local media database...")

                // Save image file locally
                val cachedPath = withContext(Dispatchers.IO) {
                    try {
                        val decodedBytes = Base64.decode(base64ImageBytes, Base64.DEFAULT)
                        val imageFile = File(context.cacheDir, "cinematic_keyframe_${System.currentTimeMillis()}.png")
                        imageFile.writeBytes(decodedBytes)
                        imageFile.absolutePath
                    } catch (e: Exception) {
                        Log.e("VideoViewModel", "Failed to save generated image as file", e)
                        null
                    }
                }

                val mediaItem = MediaItem(
                    videoIdea = initialIdea,
                    mood = mood,
                    visualStyle = style,
                    duration = duration,
                    node1Prompt = parsedModel.node1Prompt,
                    node2RefinedPrompt = parsedModel.node2RefinedPrompt,
                    imagePath = cachedPath,
                    storyboardJson = cleanedJson
                )

                val insertedId = dao.insertMediaItem(mediaItem)
                val finalItem = mediaItem.copy(id = insertedId.toInt())

                _activeMediaItem.value = finalItem
                _generationState.value = GenerationState.Success(finalItem, parsedModel.storyboard)

            } catch (e: Exception) {
                Log.e("VideoViewModel", "Execution failed", e)
                _generationState.value = GenerationState.Error("Generation failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun cleanJson(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substringAfter("```json")
        } else if (str.startsWith("```")) {
            str = str.substringAfter("```")
        }
        if (str.endsWith("```")) {
            str = str.substringBeforeLast("```")
        }
        return str.trim()
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun parseStoryboardFromItem(item: MediaItem?): List<StoryboardShot> {
        if (item?.storyboardJson == null) return emptyList()
        return try {
            val model = moshi.adapter(VideoPromptModel::class.java).fromJson(item.storyboardJson)
            model?.storyboard ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class VideoViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
