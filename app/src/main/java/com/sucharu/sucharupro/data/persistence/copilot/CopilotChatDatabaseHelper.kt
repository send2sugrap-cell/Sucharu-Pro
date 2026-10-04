package com.sucharu.sucharupro.data.persistence.copilot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sucharu.sucharupro.ui.customer.screens.ChatMessageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLite Database OpenHelper for persistent local storage of AI Copilot chat messages.
 */
class CopilotChatDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "sucharu_copilot_chat.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_MESSAGES = "copilot_chat_messages"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_IS_USER = "is_user"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_IS_USER INTEGER NOT NULL,
                $COLUMN_TIMESTAMP TEXT NOT NULL,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    suspend fun getAllMessages(): List<ChatMessageItem> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<ChatMessageItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_ID, COLUMN_TEXT, COLUMN_IS_USER, COLUMN_TIMESTAMP),
            null,
            null,
            null,
            null,
            "$COLUMN_CREATED_AT ASC"
        )

        cursor.use {
            val idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID)
            val textIndex = cursor.getColumnIndexOrThrow(COLUMN_TEXT)
            val isUserIndex = cursor.getColumnIndexOrThrow(COLUMN_IS_USER)
            val timestampIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)

            while (cursor.moveToNext()) {
                messages.add(
                    ChatMessageItem(
                        messageId = cursor.getString(idIndex),
                        text = cursor.getString(textIndex),
                        isUser = cursor.getInt(isUserIndex) == 1,
                        timestamp = cursor.getString(timestampIndex)
                    )
                )
            }
        }
        messages
    }

    suspend fun insertMessage(message: ChatMessageItem, createdAt: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, message.messageId)
            put(COLUMN_TEXT, message.text)
            put(COLUMN_IS_USER, if (message.isUser) 1 else 0)
            put(COLUMN_TIMESTAMP, message.timestamp)
            put(COLUMN_CREATED_AT, createdAt)
        }
        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
    }
}
