package com.smouldering_durtles.wk.db

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.database.AbstractCursor
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.util.ObjectSupport.orElse
import com.smouldering_durtles.wk.util.ObjectSupport.safe
import com.smouldering_durtles.wk.util.SearchUtil
import java.nio.charset.StandardCharsets

/**
 * Content provider for search results, used for incremental search.
 */
class SubjectContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor = safe({ SubjectCursor(emptyList()) }) {
        if (selectionArgs == null || selectionArgs.isEmpty() || selectionArgs[0] == null || context == null) {
            SubjectCursor(emptyList())
        } else {
            val query = selectionArgs[0]
            val subjects = SearchUtil.searchSubjectSuggestions(query)
            SubjectCursor(subjects)
        }
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.android.search.suggest"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = throw UnsupportedOperationException()

    /**
     * Cursor implementation for search results.
     *
     * @param subjects the subjects for this search result.
     */
    private class SubjectCursor(subjects: List<Subject>) : AbstractCursor() {
        private val subjects: List<Subject> = ArrayList(subjects)

        override fun getCount(): Int = subjects.size

        override fun getColumnNames(): Array<String> = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
        )

        override fun getString(column: Int): String? {
            if (isBeforeFirst || isAfterLast) {
                return null
            }
            val subject = subjects[position]
            return when (column) {
                0, 3 -> subject.id.toString()
                1 -> String.format(
                    "%s %s - %s",
                    subject.getSearchSuggestionType(),
                    orElse(subject.getCharacters(), orElse(subject.getSlug(), "")),
                    subject.getOneMeaning()
                )
                2 -> subject.getMeaningRichText("").toString()
                else -> null
            }
        }

        override fun getBlob(column: Int): ByteArray {
            val value = getString(column) ?: return ByteArray(0)
            return value.toByteArray(StandardCharsets.UTF_8)
        }

        override fun getInt(column: Int): Int = getLong(column).toInt()

        override fun getShort(column: Int): Short = getLong(column).toShort()

        override fun getLong(column: Int): Long {
            if (isBeforeFirst || isAfterLast) {
                return 0
            }
            val subject = subjects[position]
            if (column == 0 || column == 3) {
                return subject.id
            }
            return 0
        }

        override fun getFloat(column: Int): Float = getLong(column).toFloat()

        override fun getDouble(column: Int): Double = getLong(column).toDouble()

        override fun getType(column: Int): Int = when (column) {
            0, 3 -> FIELD_TYPE_INTEGER
            1, 2 -> FIELD_TYPE_STRING
            else -> FIELD_TYPE_NULL
        }

        override fun isNull(column: Int): Boolean {
            if (isBeforeFirst || isAfterLast) {
                return true
            }
            return getType(column) == FIELD_TYPE_NULL
        }
    }
}
