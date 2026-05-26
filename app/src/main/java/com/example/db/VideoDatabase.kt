package com.example.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoIdea: String,
    val mood: String,
    val visualStyle: String,
    val duration: String,
    val node1Prompt: String,
    val node2RefinedPrompt: String,
    val imagePath: String?,
    val storyboardJson: String?,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun getAllMediaItemsFlow(): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItem): Long

    @Query("DELETE FROM media_items WHERE id = :itemId")
    suspend fun deleteMediaItem(itemId: Int)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}

@Database(entities = [MediaItem::class], version = 1, exportSchema = false)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao

    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null

        fun getDatabase(context: Context): VideoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "video_generator_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
