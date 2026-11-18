package xy.readerone.id.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import xy.readerone.id.model.DocItem
import java.io.File
import java.util.Locale
import java.util.UUID

class FileRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("reader_library", Context.MODE_PRIVATE)
    val workspaceDir: File
        get() = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

    fun listDocuments(): List<DocItem> {
        val items = mutableListOf<DocItem>()
        // 1. Scan internal workspace documents
        workspaceDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                items.add(
                    DocItem(
                        id = file.absolutePath,
                        name = file.name,
                        uriString = Uri.fromFile(file).toString(),
                        extension = file.extension.uppercase(Locale.ROOT).ifEmpty { "FILE" },
                        sizeBytes = file.length(),
                        lastModified = file.lastModified()
                    )
                )
            }
        }
        // 2. Load recent SAF / opened URIs
        val json = prefs.getString("recent_uris", "[]") ?: "[]"
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val uriStr = obj.optString("uri")
            if (items.none { it.uriString == uriStr }) {
                items.add(
                    DocItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        uriString = uriStr,
                        extension = obj.optString("ext"),
                        sizeBytes = obj.optLong("size", 0L),
                        lastModified = obj.optLong("date", System.currentTimeMillis())
                    )
                )
            }
        }
        return items.sortedByDescending { it.lastModified }
    }

    fun registerUri(uri: Uri): DocItem {
        var name = "Dokumen"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val ext = name.substringAfterLast('.', "FILE").uppercase(Locale.ROOT)
        val item = DocItem(
            id = UUID.randomUUID().toString(),
            name = name,
            uriString = uri.toString(),
            extension = ext,
            sizeBytes = size,
            lastModified = System.currentTimeMillis()
        )
        val arr = JSONArray(prefs.getString("recent_uris", "[]") ?: "[]")
        val updated = JSONArray()
        updated.put(JSONObject().apply {
            put("id", item.id)
            put("name", item.name)
            put("uri", item.uriString)
            put("ext", item.extension)
            put("size", item.sizeBytes)
            put("date", item.lastModified)
        })
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("uri") != item.uriString) updated.put(obj)
        }
        prefs.edit().putString("recent_uris", updated.toString()).apply()
        return item
    }

    fun removeItem(item: DocItem) {
        if (item.uri.scheme == "file") {
            File(item.id).delete()
        }
        val arr = JSONArray(prefs.getString("recent_uris", "[]") ?: "[]")
        val updated = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optString("uri") != item.uriString) updated.put(obj)
        }
        prefs.edit().putString("recent_uris", updated.toString()).apply()
    }
}
