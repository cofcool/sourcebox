package net.cofcool.sourcebox.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class PendingChange(
    val id: Long,
    val entity: String,
    val entityId: String,
    val operation: String,
    val payload: String
)

class ActionExportDatabase(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, TABLE_ACTIONS)
        createTable(db, TABLE_NOTES)
        createTable(db, TABLE_COMMANDS)
        createTypesTable(db)
        createSyncTable(db)
        createPendingChangesTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createTable(db, TABLE_NOTES)
            createTable(db, TABLE_COMMANDS)
            createSyncTable(db)
            db.execSQL(
                """
                INSERT OR IGNORE INTO $TABLE_SYNC_ALL(dataset, synced_at)
                SELECT 'actions', synced_at FROM $TABLE_SYNC WHERE id = 1
                """.trimIndent()
            )
        }
        if (oldVersion < 3) {
            listOf(TABLE_ACTIONS, TABLE_NOTES, TABLE_COMMANDS).forEach { table ->
                db.execSQL("ALTER TABLE $table ADD COLUMN dirty INTEGER NOT NULL DEFAULT 0")
            }
            createPendingChangesTable(db)
        }
        if (oldVersion < 4) {
            createTypesTable(db)
        }
    }

    fun replaceExportData(
        actions: List<ActionExport>,
        notes: List<NoteRecord>,
        commands: List<CommandRecord>,
        actionTypes: ActionTypeResponse,
        syncedAt: String
    ) {
        validateIds(actions.map { it.record.id }, "行动记录")
        validateIds(notes.map { it.id }, "便笺")
        validateIds(commands.mapNotNull { it.id }, "命令")
        if (commands.any { it.id.isNullOrBlank() }) {
            throw IOException("exportAll 返回了缺少 ID 的命令")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            replaceRows(db, TABLE_ACTIONS, actions.map { it.record.id to json.encodeToString(it) })
            replaceRows(db, TABLE_NOTES, notes.map { it.id to json.encodeToString(it) })
            replaceRows(db, TABLE_COMMANDS, commands.map { it.id!! to json.encodeToString(it) })
            replaceTypes(db, actionTypes)
            listOf("actions", "notes", "commands", "types").forEach { dataset ->
                val syncValues = ContentValues().apply {
                    put("dataset", dataset)
                    put("synced_at", syncedAt)
                }
                db.insertWithOnConflict(
                    TABLE_SYNC_ALL,
                    null,
                    syncValues,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceCommands(commands: List<CommandRecord>, syncedAt: String) {
        validateIds(commands.mapNotNull { it.id }, "命令")
        if (commands.any { it.id.isNullOrBlank() }) {
            throw IOException("exportAll 返回了缺少 ID 的命令")
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            replaceRows(db, TABLE_COMMANDS, commands.map { it.id!! to json.encodeToString(it) })
            val values = ContentValues().apply {
                put("dataset", "commands")
                put("synced_at", syncedAt)
            }
            check(db.insertWithOnConflict(TABLE_SYNC_ALL, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) {
                "保存命令同步时间失败"
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun notes(): List<NoteRecord> =
        readPayloads(TABLE_NOTES, NoteRecord.serializer())
            .filter { it.state == "NORMAL" }
            .sortedByDescending { it.date.orEmpty() }

    fun commands(query: String): List<CommandRecord> {
        val trimmed = query.trim()
        val parameters = if (trimmed.contains('=')) parseQuery(trimmed) else emptyMap()
        val source = readPayloads(TABLE_COMMANDS, CommandRecord.serializer())
        val cmdSearch = parameters["cmd"]
        if (!cmdSearch.isNullOrEmpty()) {
            return source.filter { it.cmd.contains(cmdSearch, ignoreCase = true) }
                .sortedByDescending { it.frequency ?: 0 }
                .take(6)
        }

        val search = parameters["q"] ?: trimmed.removePrefix("?")
        val tokens = if (search == "ALL") emptyList() else search.split(" ").filter(String::isNotBlank)
        return source.filter { command ->
            (parameters["id"].isNullOrBlank() || command.id == parameters["id"]) &&
                (parameters["alias"].isNullOrBlank() || command.alias == parameters["alias"]) &&
                tokens.all { token ->
                    when {
                        token.startsWith("@") -> command.alias == token
                        token.startsWith("#") -> token in command.tags.orEmpty()
                        else -> command.id == token.md5()
                    }
                }
            }
            .sortedByDescending { it.createTime.orEmpty() }
            .take(50)
    }

    fun actions(prefix: String, query: String, page: Int = 1, size: Int = 50): List<ActionRecord> {
        require(page > 0 && size > 0) { "page and size must be positive" }
        val parameters = parseQuery(query)
        val nameSearch = if (prefix == "record") {
            parameters["name"] ?: query.takeUnless { it.contains('=') }?.trim().orEmpty()
        } else {
            parameters["name"].orEmpty()
        }
        val actions = readPayloads(TABLE_ACTIONS, ActionExport.serializer())
            .map { it.record }
            .filter { action ->
                if (prefix == "record") {
                    action.type == prefix &&
                        (nameSearch.isBlank() || action.name.contains(nameSearch, ignoreCase = true))
                } else {
                    action.type == prefix &&
                        (parameters["id"].isNullOrBlank() || action.id == parameters["id"]) &&
                        (parameters["name"].isNullOrBlank() ||
                            action.name.contains(parameters.getValue("name"), ignoreCase = true)) &&
                        (parameters["state"].isNullOrBlank() || action.state == parameters["state"]) &&
                        (parameters["category"].isNullOrBlank() || action.category == parameters["category"])
                }
            }
            .sortedByDescending { it.createTime.orEmpty() }
        val offset = (page - 1L) * size
        if (offset >= actions.size) return emptyList()
        return actions.drop(offset.toInt()).take(size)
    }

    fun actionCategories(): List<String> =
        actionTypes().types.filter { it.type == "category" }
            .map { it.name }
            .distinct()
            .sorted()

    fun actionLabels(): List<String> =
        actionTypes().types.filter { it.type == "label" }
            .map { it.name }
            .distinct()
            .sorted()

    fun actionTypes(): ActionTypeResponse =
        readableDatabase.rawQuery(
            "SELECT payload FROM $TABLE_TYPES WHERE id = 1",
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) json.decodeFromString(ActionTypeResponse.serializer(), cursor.getString(0))
            else ActionTypeResponse()
        }

    fun actionDetails(actionId: String): ActionExport? =
        readPayload(TABLE_ACTIONS, actionId, ActionExport.serializer())

    fun saveNote(note: NoteRecord) {
        upsert(TABLE_NOTES, note.id, json.encodeToString(note))
    }

    fun markNoteDirty(note: NoteRecord) {
        enqueueUpsert(TABLE_NOTES, "note", note.id, "save", json.encodeToString(note))
    }

    fun deleteNote(id: String) {
        delete(TABLE_NOTES, id)
    }

    fun markNoteDeleted(id: String) {
        enqueueDelete(TABLE_NOTES, "note", id)
    }

    fun saveCommand(command: CommandRecord) {
        val id = command.id ?: throw IOException("命令缺少 ID")
        upsert(TABLE_COMMANDS, id, json.encodeToString(command))
    }

    fun markCommandDirty(command: CommandRecord) {
        val id = command.id ?: throw IOException("命令缺少 ID")
        enqueueUpsert(TABLE_COMMANDS, "command", id, "save", json.encodeToString(command))
    }

    fun deleteCommand(id: String) {
        delete(TABLE_COMMANDS, id)
    }

    fun markCommandDeleted(id: String) {
        enqueueDelete(TABLE_COMMANDS, "command", id)
    }

    fun markCommandHistoryCollectionDirty() {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("entity", "command")
            put("entity_id", COMMAND_HISTORY_COLLECTION_ID)
            put("operation", "store")
            put("payload", "ALL")
        }
        check(db.insertOrThrow(TABLE_PENDING, null, values) != -1L) {
            "保存命令历史收集任务失败"
        }
    }

    fun saveAction(action: ActionRecord) {
        val existing = actionDetails(action.id)
        upsert(
            TABLE_ACTIONS,
            action.id,
            json.encodeToString(existing?.copy(record = action) ?: ActionExport(action))
        )
    }

    fun markActionDirty(action: ActionRecord) {
        val existing = actionDetails(action.id)
        val exported = existing?.copy(record = action) ?: ActionExport(action)
        enqueueUpsert(TABLE_ACTIONS, "action", action.id, "save", json.encodeToString(exported))
    }

    fun saveActionComment(actionId: String, comment: ActionComment) {
        val action = actionDetails(actionId) ?: throw IOException("本地找不到行动记录：$actionId")
        upsert(
            TABLE_ACTIONS,
            actionId,
            json.encodeToString(action.copy(comments = action.comments + comment))
        )
    }

    fun markActionCommentDirty(actionId: String, comment: ActionComment) {
        val action = actionDetails(actionId) ?: throw IOException("本地找不到行动记录：$actionId")
        val updated = action.copy(comments = action.comments + comment)
        enqueueUpsert(
            TABLE_ACTIONS,
            "action",
            actionId,
            "comment",
            json.encodeToString(comment),
            json.encodeToString(updated)
        )
    }

    fun saveActionChecklist(actionId: String, checklist: ActionCheckList) {
        val action = actionDetails(actionId) ?: throw IOException("本地找不到行动记录：$actionId")
        val updated = action.checklists.filterNot { it.id == checklist.id } + checklist
        upsert(TABLE_ACTIONS, actionId, json.encodeToString(action.copy(checklists = updated)))
    }

    fun markActionChecklistDirty(
        actionId: String,
        checklist: ActionCheckList,
        progressComment: ActionComment
    ) {
        val action = actionDetails(actionId) ?: throw IOException("本地找不到行动记录：$actionId")
        val updated = action.copy(
            checklists = action.checklists.filterNot { it.id == checklist.id } + checklist,
            comments = action.comments + progressComment
        )
        enqueueUpsert(
            TABLE_ACTIONS,
            "action",
            actionId,
            "checklist_save",
            json.encodeToString(checklist),
            json.encodeToString(updated)
        )
    }

    fun deleteActionChecklist(id: String) {
        removeChecklist(id, dirty = false)
    }

    fun markActionChecklistDeleted(id: String): String {
        val actionId = removeChecklist(id, dirty = true)
            ?: throw IOException("本地找不到 Checklist：$id")
        return actionId
    }

    private fun removeChecklist(id: String, dirty: Boolean): String? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val actions = readPayloads(db, TABLE_ACTIONS, ActionExport.serializer())
            val action = actions.firstOrNull { item -> item.checklists.any { it.id == id } }
                ?: return null
            val changed = action.copy(checklists = action.checklists.filterNot { it.id == id })
            if (dirty) {
                enqueueChange(db, "action", action.record.id, "checklist_delete", id, TABLE_ACTIONS,
                    json.encodeToString(changed))
            } else {
                upsert(db, TABLE_ACTIONS, action.record.id, json.encodeToString(changed), dirty = 0)
            }
            db.setTransactionSuccessful()
            return action.record.id
        } finally {
            db.endTransaction()
        }
    }

    fun pendingChanges(): List<PendingChange> =
        readableDatabase.rawQuery(
            "SELECT id, entity, entity_id, operation, payload FROM $TABLE_PENDING ORDER BY id",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        PendingChange(
                            cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4)
                        )
                    )
                }
            }
        }

    fun pendingChangeCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_PENDING", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }

    fun completePendingChange(change: PendingChange, canonicalPayload: String? = null) {
        val table = tableForEntity(change.entity)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_PENDING, "id = ?", arrayOf(change.id.toString()))
            val stillDirty = db.rawQuery(
                "SELECT 1 FROM $TABLE_PENDING WHERE entity = ? AND entity_id = ? LIMIT 1",
                arrayOf(change.entity, change.entityId)
            ).use { it.moveToFirst() }
            if (stillDirty) {
                db.execSQL("UPDATE $table SET dirty = 1 WHERE id = ?", arrayOf(change.entityId))
            } else if (canonicalPayload != null) {
                upsert(db, table, change.entityId, canonicalPayload, dirty = 0)
            } else {
                db.execSQL("UPDATE $table SET dirty = 0 WHERE id = ?", arrayOf(change.entityId))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun actionCount(): Int = rowCount(TABLE_ACTIONS)

    fun noteCount(): Int = rowCount(TABLE_NOTES)

    fun commandCount(): Int = rowCount(TABLE_COMMANDS)

    fun lastSyncedAt(): String? = lastSyncedAt("actions")

    fun lastSyncedAt(dataset: String): String? =
        readableDatabase.rawQuery(
            "SELECT synced_at FROM $TABLE_SYNC_ALL WHERE dataset = ?",
            arrayOf(dataset)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun rowCount(table: String): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            if (!cursor.moveToFirst()) return 0
            return cursor.getInt(0)
        }
    }

    private fun validateIds(ids: List<String>, dataset: String) {
        val invalidId = ids.groupingBy { it }.eachCount()
            .entries.firstOrNull { it.key.isBlank() || it.value > 1 }
        if (invalidId != null) {
            throw IOException("exportAll 返回了无效或重复的${dataset} ID：${invalidId.key}")
        }
    }

    private fun replaceRows(db: SQLiteDatabase, table: String, rows: List<Pair<String, String>>) {
        val dirtyRows = db.rawQuery("SELECT id, payload FROM $table WHERE dirty = 1", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getString(1))
            }.toMap()
        }
        db.delete(table, null, null)
        rows.filterNot { it.first in dirtyRows }.forEach { (id, payload) ->
            val values = ContentValues().apply {
                put("id", id)
                put("payload", payload)
                put("dirty", 0)
            }
            check(db.insertOrThrow(table, null, values) != -1L) { "写入本地数据失败：$id" }
        }
        dirtyRows.forEach { (id, payload) -> upsert(db, table, id, payload, dirty = 1) }
    }

    private fun <T> readPayloads(table: String, serializer: kotlinx.serialization.KSerializer<T>): List<T> =
        readPayloads(readableDatabase, table, serializer)

    private fun <T> readPayloads(
        db: SQLiteDatabase,
        table: String,
        serializer: kotlinx.serialization.KSerializer<T>
    ): List<T> =
        db.rawQuery("SELECT payload FROM $table", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(json.decodeFromString(serializer, cursor.getString(0)))
            }
        }

    private fun <T> readPayload(
        table: String,
        id: String,
        serializer: kotlinx.serialization.KSerializer<T>
    ): T? =
        readableDatabase.rawQuery(
            "SELECT payload FROM $table WHERE id = ?",
            arrayOf(id)
        ).use { cursor ->
            if (cursor.moveToFirst()) json.decodeFromString(serializer, cursor.getString(0)) else null
        }

    private fun upsert(table: String, id: String, payload: String) =
        upsert(writableDatabase, table, id, payload)

    private fun upsert(db: SQLiteDatabase, table: String, id: String, payload: String, dirty: Int = 0) {
        val values = ContentValues().apply {
            put("id", id)
            put("payload", payload)
            put("dirty", dirty)
        }
        check(db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) {
            "更新本地数据失败：$id"
        }
    }

    private fun enqueueUpsert(
        table: String,
        entity: String,
        entityId: String,
        operation: String,
        payload: String,
        localPayload: String = payload
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            upsert(db, table, entityId, localPayload, dirty = 1)
            enqueueChange(db, entity, entityId, operation, payload, table, localPayload)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun enqueueDelete(table: String, entity: String, entityId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(table, "id = ?", arrayOf(entityId))
            enqueueChange(db, entity, entityId, "delete", entityId, table, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun enqueueChange(
        db: SQLiteDatabase,
        entity: String,
        entityId: String,
        operation: String,
        payload: String,
        table: String,
        localPayload: String?
    ) {
        val values = ContentValues().apply {
            put("entity", entity)
            put("entity_id", entityId)
            put("operation", operation)
            put("payload", payload)
        }
        check(db.insertOrThrow(TABLE_PENDING, null, values) != -1L) { "保存待同步修改失败：$entityId" }
        if (localPayload != null) {
            upsert(db, table, entityId, localPayload, dirty = 1)
        } else {
            db.execSQL("UPDATE $table SET dirty = 1 WHERE id = ?", arrayOf(entityId))
        }
    }

    private fun tableForEntity(entity: String): String = when (entity) {
        "note" -> TABLE_NOTES
        "command" -> TABLE_COMMANDS
        "action" -> TABLE_ACTIONS
        else -> throw IOException("未知的待同步数据类型：$entity")
    }

    private fun delete(table: String, id: String) {
        writableDatabase.delete(table, "id = ?", arrayOf(id))
    }

    private fun parseQuery(query: String): Map<String, String> =
        query.removePrefix("?")
            .split("&")
            .filter(String::isNotBlank)
            .mapNotNull { pair ->
                val separator = pair.indexOf('=')
                if (separator < 0) return@mapNotNull null
                val key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8)
                val value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8)
                key.lowercase() to value
            }
            .toMap()

    private fun String.md5(): String =
        MessageDigest.getInstance("MD5")
            .digest(toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun createTable(db: SQLiteDatabase, table: String) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $table (
                id TEXT PRIMARY KEY NOT NULL,
                payload TEXT NOT NULL,
                dirty INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun createPendingChangesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PENDING (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                entity TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                operation TEXT NOT NULL,
                payload TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createSyncTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SYNC_ALL (
                dataset TEXT PRIMARY KEY NOT NULL,
                synced_at TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createTypesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_TYPES (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                payload TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun replaceTypes(db: SQLiteDatabase, types: ActionTypeResponse) {
        val values = ContentValues().apply {
            put("id", 1)
            put("payload", json.encodeToString(types))
        }
        check(db.insertWithOnConflict(TABLE_TYPES, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) {
            "保存行动类型失败"
        }
    }

    companion object {
        private const val DATABASE_NAME = "sourcebox-local.db"
        private const val DATABASE_VERSION = 4
        private const val TABLE_ACTIONS = "exported_actions"
        private const val TABLE_NOTES = "exported_notes"
        private const val TABLE_COMMANDS = "exported_commands"
        private const val TABLE_SYNC = "sync_metadata"
        private const val TABLE_SYNC_ALL = "dataset_sync_metadata"
        private const val TABLE_PENDING = "pending_changes"
        private const val TABLE_TYPES = "exported_action_types"
        const val COMMAND_HISTORY_COLLECTION_ID = "__command_history_collection__"
        private val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
