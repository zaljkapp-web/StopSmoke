package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmokingDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "SmokingDbHelper"
        private const val DATABASE_NAME = "smoking_database.db"
        private const val DATABASE_VERSION = 1

        // Table cigarette_logs
        private const val TABLE_LOGS = "cigarette_logs"
        private const val COLUMN_LOG_ID = "id"
        private const val COLUMN_LOG_TIMESTAMP = "timestamp"
        private const val COLUMN_LOG_AMOUNT = "amount"
        private const val COLUMN_LOG_TYPE = "type"
        private const val COLUMN_LOG_DATE_STRING = "dateString"

        // Table day_overrides
        private const val TABLE_OVERRIDES = "day_overrides"
        private const val COLUMN_OVERRIDE_DATE_STRING = "dateString"
        private const val COLUMN_OVERRIDE_IS_VACATION = "isVacation"
        private const val COLUMN_OVERRIDE_OVERTIME_START = "overtimeStartHours"
        private const val COLUMN_OVERRIDE_OVERTIME_END = "overtimeEndHours"

        @Volatile
        private var INSTANCE: SmokingDatabaseHelper? = null

        fun getInstance(context: Context): SmokingDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = SmokingDatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val _allLogs = MutableStateFlow<List<CigaretteLog>>(emptyList())
    val allLogsFlow: StateFlow<List<CigaretteLog>> = _allLogs.asStateFlow()

    private val _allOverrides = MutableStateFlow<List<DayOverride>>(emptyList())
    val allOverridesFlow: StateFlow<List<DayOverride>> = _allOverrides.asStateFlow()

    init {
        // Read initial data to populate Flows
        refreshFlows()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createLogsTable = """
            CREATE TABLE $TABLE_LOGS (
                $COLUMN_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LOG_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_LOG_AMOUNT INTEGER NOT NULL,
                $COLUMN_LOG_TYPE TEXT NOT NULL,
                $COLUMN_LOG_DATE_STRING TEXT NOT NULL
            )
        """.trimIndent()

        val createOverridesTable = """
            CREATE TABLE $TABLE_OVERRIDES (
                $COLUMN_OVERRIDE_DATE_STRING TEXT PRIMARY KEY,
                $COLUMN_OVERRIDE_IS_VACATION INTEGER NOT NULL DEFAULT 0,
                $COLUMN_OVERRIDE_OVERTIME_START INTEGER NOT NULL DEFAULT 0,
                $COLUMN_OVERRIDE_OVERTIME_END INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()

        db.execSQL(createLogsTable)
        db.execSQL(createOverridesTable)
        Log.d(TAG, "Created database tables")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OVERRIDES")
        onCreate(db)
    }

    private fun refreshFlows() {
        _allLogs.value = getAllLogs()
        _allOverrides.value = getAllOverrides()
    }

    // Cigarette Logs operations
    fun getAllLogs(): List<CigaretteLog> {
        val list = mutableListOf<CigaretteLog>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_LOGS ORDER BY $COLUMN_LOG_TIMESTAMP DESC", null)
        cursor.use { c ->
            val idIndex = c.getColumnIndex(COLUMN_LOG_ID)
            val tsIndex = c.getColumnIndex(COLUMN_LOG_TIMESTAMP)
            val amtIndex = c.getColumnIndex(COLUMN_LOG_AMOUNT)
            val typeIndex = c.getColumnIndex(COLUMN_LOG_TYPE)
            val dateIndex = c.getColumnIndex(COLUMN_LOG_DATE_STRING)

            while (c.moveToNext()) {
                list.add(
                    CigaretteLog(
                        id = if (idIndex >= 0) c.getInt(idIndex) else 0,
                        timestamp = if (tsIndex >= 0) c.getLong(tsIndex) else 0L,
                        amount = if (amtIndex >= 0) c.getInt(amtIndex) else 0,
                        type = if (typeIndex >= 0) c.getString(typeIndex) else "NORMAL",
                        dateString = if (dateIndex >= 0) c.getString(dateIndex) else ""
                    )
                )
            }
        }
        return list
    }

    fun getLogsForDay(dateString: String): List<CigaretteLog> {
        val list = mutableListOf<CigaretteLog>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_LOGS WHERE $COLUMN_LOG_DATE_STRING = ? ORDER BY $COLUMN_LOG_TIMESTAMP ASC",
            arrayOf(dateString)
        )
        cursor.use { c ->
            val idIndex = c.getColumnIndex(COLUMN_LOG_ID)
            val tsIndex = c.getColumnIndex(COLUMN_LOG_TIMESTAMP)
            val amtIndex = c.getColumnIndex(COLUMN_LOG_AMOUNT)
            val typeIndex = c.getColumnIndex(COLUMN_LOG_TYPE)
            val dateIndex = c.getColumnIndex(COLUMN_LOG_DATE_STRING)

            while (c.moveToNext()) {
                list.add(
                    CigaretteLog(
                        id = if (idIndex >= 0) c.getInt(idIndex) else 0,
                        timestamp = if (tsIndex >= 0) c.getLong(tsIndex) else 0L,
                        amount = if (amtIndex >= 0) c.getInt(amtIndex) else 0,
                        type = if (typeIndex >= 0) c.getString(typeIndex) else "NORMAL",
                        dateString = if (dateIndex >= 0) c.getString(dateIndex) else ""
                    )
                )
            }
        }
        return list
    }

    fun insertLog(log: CigaretteLog) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOG_TIMESTAMP, log.timestamp)
            put(COLUMN_LOG_AMOUNT, log.amount)
            put(COLUMN_LOG_TYPE, log.type)
            put(COLUMN_LOG_DATE_STRING, log.dateString)
        }
        db.insert(TABLE_LOGS, null, values)
        refreshFlows()
    }

    fun deleteLog(log: CigaretteLog) {
        val db = writableDatabase
        db.delete(TABLE_LOGS, "$COLUMN_LOG_ID = ?", arrayOf(log.id.toString()))
        refreshFlows()
    }

    fun deleteLogById(id: Int) {
        val db = writableDatabase
        db.delete(TABLE_LOGS, "$COLUMN_LOG_ID = ?", arrayOf(id.toString()))
        refreshFlows()
    }

    // Day Overrides operations
    fun getAllOverrides(): List<DayOverride> {
        val list = mutableListOf<DayOverride>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OVERRIDES", null)
        cursor.use { c ->
            val dateIndex = c.getColumnIndex(COLUMN_OVERRIDE_DATE_STRING)
            val vacIndex = c.getColumnIndex(COLUMN_OVERRIDE_IS_VACATION)
            val startIndex = c.getColumnIndex(COLUMN_OVERRIDE_OVERTIME_START)
            val endIndex = c.getColumnIndex(COLUMN_OVERRIDE_OVERTIME_END)

            while (c.moveToNext()) {
                list.add(
                    DayOverride(
                        dateString = if (dateIndex >= 0) c.getString(dateIndex) else "",
                        isVacation = if (vacIndex >= 0) c.getInt(vacIndex) == 1 else false,
                        overtimeStartHours = if (startIndex >= 0) c.getInt(startIndex) else 0,
                        overtimeEndHours = if (endIndex >= 0) c.getInt(endIndex) else 0
                    )
                )
            }
        }
        return list
    }

    fun getOverrideForDay(dateString: String): DayOverride? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_OVERRIDES WHERE $COLUMN_OVERRIDE_DATE_STRING = ? LIMIT 1",
            arrayOf(dateString)
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                val dateIndex = c.getColumnIndex(COLUMN_OVERRIDE_DATE_STRING)
                val vacIndex = c.getColumnIndex(COLUMN_OVERRIDE_IS_VACATION)
                val startIndex = c.getColumnIndex(COLUMN_OVERRIDE_OVERTIME_START)
                val endIndex = c.getColumnIndex(COLUMN_OVERRIDE_OVERTIME_END)

                return DayOverride(
                    dateString = if (dateIndex >= 0) c.getString(dateIndex) else "",
                    isVacation = if (vacIndex >= 0) c.getInt(vacIndex) == 1 else false,
                    overtimeStartHours = if (startIndex >= 0) c.getInt(startIndex) else 0,
                    overtimeEndHours = if (endIndex >= 0) c.getInt(endIndex) else 0
                )
            }
        }
        return null
    }

    fun insertOrUpdateOverride(override: DayOverride) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_OVERRIDE_DATE_STRING, override.dateString)
            put(COLUMN_OVERRIDE_IS_VACATION, if (override.isVacation) 1 else 0)
            put(COLUMN_OVERRIDE_OVERTIME_START, override.overtimeStartHours)
            put(COLUMN_OVERRIDE_OVERTIME_END, override.overtimeEndHours)
        }
        db.insertWithOnConflict(TABLE_OVERRIDES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        refreshFlows()
    }

    fun deleteOverride(override: DayOverride) {
        val db = writableDatabase
        db.delete(TABLE_OVERRIDES, "$COLUMN_OVERRIDE_DATE_STRING = ?", arrayOf(override.dateString))
        refreshFlows()
    }
}
