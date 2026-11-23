package net.cofcool.sourcebox.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

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
        ensureQuerySchema(db)
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
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_pending_entity_operation_id " +
                    "ON $TABLE_PENDING(entity, operation, entity_id)"
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
        if (oldVersion < 5) {
            ensureQuerySchema(db)
            rebuildQueryColumns(db)
        }
    }

    fun replaceExportData(
        actions: List<ActionExport>,
        notes: List<NoteRecord>,
        actionTypes: ActionTypeResponse,
        syncedAt: String
    ) {
        validateIds(actions.map { it.record.id }, "行动记录")
        validateIds(notes.map { it.id }, "便笺")
        val db = writableDatabase
        db.transaction {
            try {
                replaceRows(
                    this,
                    TABLE_ACTIONS,
                    actions.map { it.record.id to json.encodeToString(it) })
                replaceRows(this, TABLE_NOTES, notes.map { it.id to json.encodeToString(it) })
                replaceTypes(this, actionTypes)
                listOf("actions", "notes", "types").forEach { dataset ->
                    val syncValues = ContentValues().apply {
                        put("dataset", dataset)
                        put("synced_at", syncedAt)
                    }
                    insertWithOnConflict(
                        TABLE_SYNC_ALL,
                        null,
                        syncValues,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            } finally {
            }
        }
    }

    fun notes(): List<NoteRecord> =
        queryPayloads(
            TABLE_NOTES,
            NoteRecord.serializer(),
            "note_state = ?",
            arrayOf("NORMAL"),
            "note_date DESC"
        )

    fun commands(query: String): List<CommandRecord> {
        val trimmed = query.trim()
        val parameters = if (trimmed.contains('=')) parseQuery(trimmed) else emptyMap()
        val cmdSearch = parameters["cmd"]
        if (!cmdSearch.isNullOrEmpty()) {
            return queryPayloads(
                TABLE_COMMANDS,
                CommandRecord.serializer(),
                "command_text LIKE ? ESCAPE '\\'",
                arrayOf(likeContains(cmdSearch)),
                "COALESCE(command_frequency, 0) DESC",
                6
            )
        }

        val search = parameters["q"] ?: trimmed.removePrefix("?")
        val tokens = if (search == "ALL") emptyList() else search.split(" ").filter(String::isNotBlank)
        val conditions = mutableListOf<String>()
        val arguments = mutableListOf<String>()
        parameters["id"]?.takeIf(String::isNotBlank)?.let {
            conditions += "id = ?"
            arguments += it
        }
        parameters["alias"]?.takeIf(String::isNotBlank)?.let {
            conditions += "command_alias = ?"
            arguments += it
        }
        tokens.forEach { token ->
            when {
                token.startsWith("@") -> {
                    conditions += "command_alias = ?"
                    arguments += token
                }
                token.startsWith("#") -> {
                    conditions += "instr(',' || command_tags || ',', ',' || ? || ',') > 0"
                    arguments += token
                }
                else -> {
                    conditions += "id = ?"
                    arguments += token.md5()
                }
            }
        }
        return queryPayloads(
            TABLE_COMMANDS,
            CommandRecord.serializer(),
            conditions.joinToString(" AND ").ifBlank { null },
            arguments.toTypedArray(),
            "command_created DESC",
            50
        )
    }

    fun actions(prefix: String, query: String, page: Int = 1, size: Int = 50): List<ActionRecord> {
        require(page > 0 && size > 0) { "page and size must be positive" }
        val parameters = parseQuery(query)
        val nameSearch = if (prefix == "record") {
            parameters["name"] ?: query.takeUnless { it.contains('=') }?.trim().orEmpty()
        } else {
            parameters["name"].orEmpty()
        }
        val conditions = mutableListOf(
            "action_type = ?",
            "NOT EXISTS (SELECT 1 FROM $TABLE_PENDING p WHERE p.entity = 'action' " +
                "AND p.operation = 'habit_delete' AND p.entity_id = $TABLE_ACTIONS.id)"
        )
        val arguments = mutableListOf(prefix)
        if (nameSearch.isNotBlank()) {
            conditions += "action_name COLLATE NOCASE LIKE ? ESCAPE '\\'"
            arguments += likeContains(nameSearch)
        }
        if (prefix == "record") {
            parameters["category"]?.takeIf(String::isNotBlank)?.let {
                conditions += "action_category = ?"
                arguments += it
            }
            parameters["state"]?.takeIf(String::isNotBlank)?.let {
                conditions += "action_state = ?"
                arguments += it
            }
            parameters["startyear"]?.takeIf(String::isNotBlank)?.let {
                conditions += "substr(action_start, 1, 4) = ?"
                arguments += it
            }
            parameters["endyear"]?.takeIf(String::isNotBlank)?.let {
                conditions += "substr(action_end, 1, 4) = ?"
                arguments += it
            }
            parameters["label"]?.takeIf(String::isNotBlank)?.let {
                conditions += "instr(action_labels, ',' || ? || ',') > 0"
                arguments += it.lowercase(Locale.ROOT).trim()
            }
        } else {
            parameters["id"]?.takeIf(String::isNotBlank)?.let {
                conditions += "id = ?"
                arguments += it
            }
            parameters["state"]?.takeIf(String::isNotBlank)?.let {
                conditions += "action_state = ?"
                arguments += it
            }
            parameters["category"]?.takeIf(String::isNotBlank)?.let {
                conditions += "action_category = ?"
                arguments += it
            }
        }
        val offset = (page.toLong() - 1L) * size
        return queryPayloads(
            TABLE_ACTIONS,
            ActionExport.serializer(),
            conditions.joinToString(" AND "),
            arguments.toTypedArray(),
            "action_created DESC",
            size,
            offset
        ).map { it.record }
    }

    fun allActions(prefix: String, query: String = ""): List<ActionRecord> =
        actions(prefix, query, page = 1, size = Int.MAX_VALUE)
            .let { actions ->
                if (prefix == "habit") actions.sortedWith(::compareHabitIndex) else actions
            }

    fun allHabitExports(query: String = ""): List<ActionExport> {
        val nameSearch = query.trim()
        val conditions = mutableListOf(
            "action_type = ?",
            "NOT EXISTS (SELECT 1 FROM $TABLE_PENDING p WHERE p.entity = 'action' " +
                "AND p.operation = 'habit_delete' AND p.entity_id = $TABLE_ACTIONS.id)"
        )
        val arguments = mutableListOf("habit")
        if (nameSearch.isNotBlank()) {
            conditions += "action_name COLLATE NOCASE LIKE ? ESCAPE '\\'"
            arguments += likeContains(nameSearch)
        }
        return queryPayloads(
            TABLE_ACTIONS,
            ActionExport.serializer(),
            conditions.joinToString(" AND "),
            arguments.toTypedArray(),
            "CASE WHEN action_index IS NULL OR trim(action_index) = '' THEN 1 ELSE 0 END, " +
                "CAST(action_index AS INTEGER), action_index COLLATE NOCASE, action_name COLLATE NOCASE"
        )
            .sortedWith { left, right -> compareHabitIndex(left.record, right.record) }
    }

    private fun compareHabitIndex(left: ActionRecord, right: ActionRecord): Int {
        val leftIndex = left.index?.trim().orEmpty()
        val rightIndex = right.index?.trim().orEmpty()
        if (leftIndex.isBlank()) {
            return if (rightIndex.isBlank()) left.name.compareTo(right.name, ignoreCase = true) else 1
        }
        if (rightIndex.isBlank()) return -1

        val numericComparison = leftIndex.toBigIntegerOrNull()?.let { leftNumber ->
            rightIndex.toBigIntegerOrNull()?.let(leftNumber::compareTo)
        }
        if (numericComparison != null && numericComparison != 0) return numericComparison
        return leftIndex.compareTo(rightIndex, ignoreCase = true)
            .takeIf { it != 0 }
            ?: left.name.compareTo(right.name, ignoreCase = true)
    }

    fun actionCandidates(name: String): List<ActionRecord> {
        return queryPayloads(
            TABLE_ACTIONS,
            ActionExport.serializer(),
            "(action_type = ? or action_type = ?) AND action_name COLLATE NOCASE LIKE ? ESCAPE '\\' " +
                "AND NOT EXISTS (SELECT 1 FROM $TABLE_PENDING p WHERE p.entity = 'action' " +
                "AND p.operation = 'habit_delete' AND p.entity_id = $TABLE_ACTIONS.id)",
            arrayOf("record", "book", likeContains(name)),
            "action_created DESC",
            10
        ).map { it.record }
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

    fun actionsStartingOn(date: String): List<ActionRecord> {
        return queryPayloads(
            TABLE_ACTIONS,
            ActionExport.serializer(),
            "action_start LIKE ? AND NOT EXISTS (SELECT 1 FROM $TABLE_PENDING p " +
                "WHERE p.entity = 'action' AND p.operation = 'habit_delete' " +
                "AND p.entity_id = $TABLE_ACTIONS.id)",
            arrayOf("$date%"),
            "action_start ASC"
        ).map { it.record }
    }

    fun actionsStartingOn(date: String, type: String): List<ActionRecord> =
        queryPayloads(
            TABLE_ACTIONS,
            ActionExport.serializer(),
            "action_start LIKE ? AND action_type = ? AND NOT EXISTS (" +
                "SELECT 1 FROM $TABLE_PENDING p WHERE p.entity = 'action' " +
                "AND p.operation = 'habit_delete' AND p.entity_id = $TABLE_ACTIONS.id)",
            arrayOf("$date%", type),
            "action_start ASC"
        ).map { it.record }

    fun randomActions(type: String, state: String, limit: Int): List<ActionRecord> {
        require(limit > 0) { "limit must be positive" }
        return queryPayloads(
            TABLE_ACTIONS,
            ActionExport.serializer(),
            "action_type = ? AND action_state = ? AND NOT EXISTS (" +
                "SELECT 1 FROM $TABLE_PENDING p WHERE p.entity = 'action' " +
                "AND p.operation = 'habit_delete' AND p.entity_id = $TABLE_ACTIONS.id)",
            arrayOf(type, state),
            "RANDOM()",
            limit
        ).map { it.record }
    }

    fun saveNote(note: NoteRecord) {
        upsert(TABLE_NOTES, note.id, json.encodeToString(note))
    }

    fun markNoteDirty(note: NoteRecord) {
        val prepared = note.copy(date = note.date ?: currentDateTime())
        enqueueUpsert(TABLE_NOTES, "note", prepared.id, "save", json.encodeToString(prepared))
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

    fun deleteCommand(id: String) {
        delete(TABLE_COMMANDS, id)
    }

    fun saveAction(action: ActionRecord) {
        val existing = actionDetails(action.id)
        upsert(
            TABLE_ACTIONS,
            action.id,
            json.encodeToString(existing?.copy(record = action) ?: ActionExport(action))
        )
    }

    fun saveHabitRecord(habit: HabitRecord) {
        val existing = actionDetails(habit.action.id)
        val exported = (existing ?: ActionExport(habit.action)).copy(
            record = habit.action,
            checklists = habit.checkLists,
            habit = habit.habit
        )
        upsert(TABLE_ACTIONS, habit.action.id, json.encodeToString(exported))
    }

    fun deleteAction(actionId: String) {
        delete(TABLE_ACTIONS, actionId)
    }

    fun markHabitDeleted(actionId: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val localPayload = actionDetails(actionId)?.let { json.encodeToString(it) }
                ?: throw IOException("本地找不到习惯：$actionId")
            enqueueChange(db, "action", actionId, "habit_delete", actionId, TABLE_ACTIONS, localPayload)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun markHabitDirty(habit: HabitRecord) {
        val now = currentDateTime()
        val prepared = habit.copy(
            action = habit.action.copy(createTime = habit.action.createTime ?: now),
            habit = habit.habit.copy(createTime = habit.habit.createTime ?: now)
        )
        val existing = actionDetails(prepared.action.id)
        val exported = (existing ?: ActionExport(prepared.action)).copy(
            record = prepared.action,
            checklists = prepared.checkLists,
            habit = prepared.habit
        )
        enqueueUpsert(
            TABLE_ACTIONS,
            "action",
            prepared.action.id,
            "habit_save",
            json.encodeToString(prepared),
            json.encodeToString(exported)
        )
    }

    fun markHabitValueDirty(actionId: String, value: Int): ActionCheckList {
        val exported = actionDetails(actionId) ?: throw IOException("本地找不到习惯：$actionId")
        val habit = exported.habit ?: throw IOException("本地找不到习惯定义：$actionId")
        val createTime = currentDateTime()
        val today = createTime.take(10)
        val existingToday = exported.checklists.firstOrNull { it.createTime?.take(10) == today }
        val localId = "$actionId:$today".md5()
        val request = ActionCheckList(
            id = localId,
            actionId = actionId,
            content = "",
            first = 0,
            last = 0,
            current = value,
            createTime = existingToday?.createTime ?: createTime
        )
        val checklist = ActionCheckList(
            id = localId,
            actionId = actionId,
            content = "${habit.freqNum ?: 1}-${habit.freqDen ?: 1}",
            first = 0,
            last = 0,
            current = value,
            createTime = existingToday?.createTime ?: createTime,
            updateTime = createTime
        )
        val changed = exported.copy(
            checklists = exported.checklists.filterNot { it.createTime?.take(10) == today } + checklist
        )
        enqueueUpsert(
            TABLE_ACTIONS,
            "action",
            actionId,
            "habit_value",
            json.encodeToString(HabitValueChange(checklist.id, request)),
            json.encodeToString(changed)
        )
        return checklist
    }

    fun replaceHabitValue(actionId: String, localChecklistId: String, saved: ActionCheckList) {
        val exported = actionDetails(actionId) ?: throw IOException("本地找不到习惯：$actionId")
        val savedDay = saved.createTime?.take(10)
        val changed = exported.copy(
            checklists = exported.checklists.map {
                if (it.id == localChecklistId || (savedDay != null && it.createTime?.take(10) == savedDay)) {
                    saved
                } else {
                    it
                }
            }
        )
        upsert(writableDatabase, TABLE_ACTIONS, actionId, json.encodeToString(changed), dirty = 1)
    }

    fun markActionDirty(action: ActionRecord) {
        val prepared = action.copy(createTime = action.createTime ?: currentDateTime())
        val existing = actionDetails(prepared.id)
        val exported = existing?.copy(record = prepared) ?: ActionExport(prepared)
        enqueueUpsert(TABLE_ACTIONS, "action", prepared.id, "save", json.encodeToString(exported))
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
        val prepared = comment.copy(createTime = comment.createTime ?: currentDateTime())
        val updated = action.copy(comments = action.comments + prepared)
        enqueueUpsert(
            TABLE_ACTIONS,
            "action",
            actionId,
            "comment",
            json.encodeToString(prepared),
            json.encodeToString(updated)
        )
    }

    fun saveActionChecklist(actionId: String, checklist: ActionCheckList) {
        val action = actionDetails(actionId) ?: throw IOException("本地找不到行动记录：$actionId")
        val savedDay = checklist.createTime?.take(10)
        val updated = action.checklists.filterNot {
            it.id == checklist.id || (action.habit != null && savedDay != null && it.createTime?.take(10) == savedDay)
        } + checklist
        upsert(TABLE_ACTIONS, actionId, json.encodeToString(action.copy(checklists = updated)))
    }

    fun markActionChecklistDirty(
        actionId: String,
        checklist: ActionCheckList
    ) {
        val action = actionDetails(actionId) ?: throw IOException("本地找不到行动记录：$actionId")
        val preparedChecklist = checklist.copy(createTime = checklist.createTime ?: currentDateTime())
        val updated = action.copy(
            checklists = action.checklists.filterNot { it.id == preparedChecklist.id } + preparedChecklist,
        )
        enqueueUpsert(
            TABLE_ACTIONS,
            "action",
            actionId,
            "checklist_save",
            json.encodeToString(preparedChecklist),
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

    fun clearLocalData() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            listOf(
                TABLE_ACTIONS,
                TABLE_NOTES,
                TABLE_COMMANDS,
                TABLE_PENDING,
                TABLE_SYNC,
                TABLE_SYNC_ALL,
                TABLE_TYPES
            ).filter { tableExists(db, it) }.forEach { table ->
                check(db.delete(table, null, null) >= 0) { "清除本地数据失败：$table" }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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
            } else if (change.operation == "habit_delete") {
                db.delete(table, "id = ?", arrayOf(change.entityId))
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

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        ).use { it.moveToFirst() }

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
            upsert(db, table, id, payload, dirty = 0)
        }
        dirtyRows.forEach { (id, payload) -> upsert(db, table, id, payload, dirty = 1) }
    }

    private fun <T> queryPayloads(
        table: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        selection: String? = null,
        selectionArgs: Array<String> = emptyArray(),
        orderBy: String? = null,
        limit: Int? = null,
        offset: Long = 0
    ): List<T> {
        val sql = buildString {
            append("SELECT payload FROM $table")
            selection?.let { append(" WHERE $it") }
            orderBy?.let { append(" ORDER BY $it") }
            limit?.let { append(" LIMIT ? OFFSET ?") }
        }
        val arguments = if (limit == null) {
            selectionArgs
        } else {
            selectionArgs + arrayOf(limit.toString(), offset.toString())
        }
        return readableDatabase.rawQuery(sql, arguments).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(json.decodeFromString(serializer, cursor.getString(0)))
                }
            }
        }
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
        val values = queryValues(table, id, payload, dirty)
        check(db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) {
            "更新本地数据失败：$id"
        }
    }

    private fun queryValues(table: String, id: String, payload: String, dirty: Int): ContentValues =
        ContentValues().apply {
            put("id", id)
            put("payload", payload)
            put("dirty", dirty)
            when (table) {
                TABLE_ACTIONS -> {
                    val action = json.decodeFromString(ActionExport.serializer(), payload).record
                    put("action_type", action.type)
                    put("action_name", action.name)
                    put("action_state", action.state)
                    put("action_category", action.category)
                    put(
                        "action_labels",
                        action.labels.orEmpty().split(",")
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .joinToString(",", prefix = ",", postfix = ",") { it.lowercase(Locale.ROOT) }
                    )
                    put("action_start", action.start)
                    put("action_created", action.createTime)
                    put("action_index", action.index?.trim())
                }
                TABLE_NOTES -> {
                    val note = json.decodeFromString(NoteRecord.serializer(), payload)
                    put("note_state", note.state)
                    put("note_date", note.date)
                }
                TABLE_COMMANDS -> {
                    val command = json.decodeFromString(CommandRecord.serializer(), payload)
                    put("command_text", command.cmd)
                    put("command_alias", command.alias)
                    put(
                        "command_tags",
                        command.tags.orEmpty().joinToString(",", prefix = ",", postfix = ",")
                    )
                    put("command_frequency", command.frequency)
                    put("command_created", command.createTime)
                }
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
                val key = pair.substring(0, separator)
                val value = pair.substring(separator + 1)
                key.lowercase() to URLDecoder.decode(value, "UTF-8")
            }
            .toMap()

    private fun likeContains(value: String): String =
        "%${value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")}%"

    private fun String.md5(): String =
        MessageDigest.getInstance("MD5")
            .digest(toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun currentDateTime(): String =
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

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

    private fun ensureQuerySchema(db: SQLiteDatabase) {
        val columns = mapOf(
            TABLE_ACTIONS to listOf(
                "action_type TEXT",
                "action_name TEXT",
                "action_state TEXT",
                "action_category TEXT",
                "action_labels TEXT",
                "action_start TEXT",
                "action_created TEXT",
                "action_index TEXT"
            ),
            TABLE_NOTES to listOf("note_state TEXT", "note_date TEXT"),
            TABLE_COMMANDS to listOf(
                "command_text TEXT",
                "command_alias TEXT",
                "command_tags TEXT",
                "command_frequency INTEGER",
                "command_created TEXT"
            )
        )
        columns.forEach { (table, definitions) ->
            definitions.forEach { definition ->
                val column = definition.substringBefore(' ')
                if (!hasColumn(db, table, column)) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN $definition")
                }
            }
        }
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_actions_type_created " +
                "ON $TABLE_ACTIONS(action_type, action_created DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_actions_type_state_created " +
                "ON $TABLE_ACTIONS(action_type, action_state, action_created DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_actions_type_category_created " +
                "ON $TABLE_ACTIONS(action_type, action_category, action_created DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_actions_type_start " +
                "ON $TABLE_ACTIONS(action_type, action_start)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_actions_start " +
                "ON $TABLE_ACTIONS(action_start)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_actions_type_index " +
                "ON $TABLE_ACTIONS(action_type, action_index)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_notes_state_date " +
                "ON $TABLE_NOTES(note_state, note_date DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_commands_created " +
                "ON $TABLE_COMMANDS(command_created DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_commands_frequency " +
                "ON $TABLE_COMMANDS(command_frequency DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_commands_alias " +
                "ON $TABLE_COMMANDS(command_alias)"
        )
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == column) return@use true
            }
            false
        }

    private fun rebuildQueryColumns(db: SQLiteDatabase) {
        listOf(TABLE_ACTIONS, TABLE_NOTES, TABLE_COMMANDS).forEach { table ->
            val rows = db.rawQuery("SELECT id, payload, dirty FROM $table", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(Triple(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
                    }
                }
            }
            rows.forEach { (id, payload, dirty) -> upsert(db, table, id, payload, dirty) }
        }
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
        private const val DATABASE_VERSION = 5
        private const val TABLE_ACTIONS = "exported_actions"
        private const val TABLE_NOTES = "exported_notes"
        private const val TABLE_COMMANDS = "exported_commands"
        private const val TABLE_SYNC = "sync_metadata"
        private const val TABLE_SYNC_ALL = "dataset_sync_metadata"
        private const val TABLE_PENDING = "pending_changes"
        private const val TABLE_TYPES = "exported_action_types"
        private val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }
    }
}
