package com.smouldering_durtles.wk.db

import com.smouldering_durtles.wk.enums.KanjiAcceptedReadingType
import com.smouldering_durtles.wk.enums.SessionItemState
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.enums.SubjectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Characterization tests for [Converters], locking in the **stored** format of every
 * `@TypeConverter` before `db/` is ported to Kotlin + KSP (#55).
 *
 * These matter more than they look. The exported Room schema hash proves the *shape* of the
 * database — column names, types, nullability — and says nothing about the values written into
 * those columns. A port that changed an enum's stored representation (say to `ordinal()`, or to
 * `SubjectType.name()` instead of its `dbTypeName`) would leave the schema byte-identical and
 * silently misread every existing row. Nothing else in the build would notice.
 *
 * So each test asserts the literal string that reaches SQLite, not merely that a round-trip
 * happens to be self-consistent — a round-trip is self-consistent under a changed format too.
 */
class ConvertersTest {
    // ---------------------------------------------------------------- SessionItemState

    @Test
    fun `session item state is stored by enum name`() {
        assertEquals("ACTIVE", Converters.sessionItemStateToString(SessionItemState.ACTIVE))
        assertEquals("PENDING", Converters.sessionItemStateToString(SessionItemState.PENDING))
        assertEquals("REPORTED", Converters.sessionItemStateToString(SessionItemState.REPORTED))
        assertEquals("ABANDONED", Converters.sessionItemStateToString(SessionItemState.ABANDONED))
    }

    @Test
    fun `session item state round-trips for every value`() {
        for (state in SessionItemState.values()) {
            val stored = Converters.sessionItemStateToString(state)
            assertEquals(state, Converters.stringToSessionItemState(stored))
        }
    }

    @Test
    fun `an unrecognised session item state throws rather than defaulting`() {
        // Deliberately not lenient, and now uniformly so: the NEW/STARTED coercion was dropped in
        // #69 along with the legacy databases that could contain them. Silently defaulting would
        // hide data corruption.
        assertThrows(IllegalArgumentException::class.java) {
            Converters.stringToSessionItemState("NOT_A_STATE")
        }
        assertThrows(IllegalArgumentException::class.java) {
            Converters.stringToSessionItemState("STARTED")
        }
    }

    // -------------------------------------------------------------------- SessionType

    @Test
    fun `session type is stored by enum name`() {
        assertEquals("NONE", Converters.sessionTypeToString(SessionType.NONE))
        assertEquals("LESSON", Converters.sessionTypeToString(SessionType.LESSON))
        assertEquals("REVIEW", Converters.sessionTypeToString(SessionType.REVIEW))
        assertEquals("SELF_STUDY", Converters.sessionTypeToString(SessionType.SELF_STUDY))
    }

    @Test
    fun `session type round-trips for every value`() {
        for (type in SessionType.values()) {
            val stored = Converters.sessionTypeToString(type)
            assertEquals(type, Converters.stringToSessionType(stored))
        }
    }

    @Test
    fun `null session type reads and writes as NONE`() {
        // Unlike the two enum converters above, this null is real and stays: session type lives in
        // the key/value property table, and PropertiesDao.getSessionType reads it through a lookup
        // that returns null until a session has ever been started.
        assertEquals("NONE", Converters.sessionTypeToString(null))
        assertEquals(SessionType.NONE, Converters.stringToSessionType(null))
    }

    @Test
    fun `an unrecognised session type throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Converters.stringToSessionType("NOT_A_TYPE")
        }
    }

    // ------------------------------------------------------- KanjiAcceptedReadingType

    @Test
    fun `kanji accepted reading type is stored by enum name`() {
        assertEquals("ONYOMI", Converters.kanjiAcceptedReadingTypeToString(KanjiAcceptedReadingType.ONYOMI))
        assertEquals("KUNYOMI", Converters.kanjiAcceptedReadingTypeToString(KanjiAcceptedReadingType.KUNYOMI))
        assertEquals("NEITHER", Converters.kanjiAcceptedReadingTypeToString(KanjiAcceptedReadingType.NEITHER))
        assertEquals("BOTH", Converters.kanjiAcceptedReadingTypeToString(KanjiAcceptedReadingType.BOTH))
    }

    @Test
    fun `kanji accepted reading type round-trips for every value`() {
        for (type in KanjiAcceptedReadingType.values()) {
            val stored = Converters.kanjiAcceptedReadingTypeToString(type)
            assertEquals(type, Converters.stringToKanjiAcceptedReadingType(stored))
        }
    }

    @Test
    fun `an unrecognised kanji accepted reading type throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Converters.stringToKanjiAcceptedReadingType("NOT_A_READING")
        }
    }

    // -------------------------------------------------------------------- SubjectType

    @Test
    fun `subject type is stored by its dbTypeName, not its enum name`() {
        // The one enum here that does NOT use name(). Porting it to name() would write
        // "WANIKANI_RADICAL" where every existing row says "radical", with no schema change.
        assertEquals("radical", Converters.subjectTypeToString(SubjectType.WANIKANI_RADICAL))
        assertEquals("kanji", Converters.subjectTypeToString(SubjectType.WANIKANI_KANJI))
        assertEquals("vocabulary", Converters.subjectTypeToString(SubjectType.WANIKANI_VOCAB))
        assertEquals("kana_vocabulary", Converters.subjectTypeToString(SubjectType.WANIKANI_KANA_VOCAB))
    }

    @Test
    fun `subject type round-trips for every value`() {
        for (type in SubjectType.values()) {
            val stored = Converters.subjectTypeToString(type)
            assertEquals(type, Converters.stringToSubjectType(stored))
        }
    }

    @Test
    fun `null subject type maps to null in both directions`() {
        // Unlike the other three, this converter is genuinely nullable — the object column is
        // nullable in the schema, so a non-null Kotlin type here would change it to NOT NULL.
        assertNull(Converters.subjectTypeToString(null))
        assertNull(Converters.stringToSubjectType(null))
    }

    @Test
    fun `an unrecognised subject type falls back to radical instead of throwing`() {
        // Asymmetric with the other converters, and intentionally so: SubjectType.from() scans
        // dbTypeNames and defaults. Preserve the fallback — a future WaniKani subject type must
        // not crash the DB read path.
        assertEquals(SubjectType.WANIKANI_RADICAL, Converters.stringToSubjectType("some_future_type"))
        assertEquals(SubjectType.WANIKANI_RADICAL, Converters.stringToSubjectType(""))
    }

    // ------------------------------------------------------------------ ObjectMapper

    @Test
    fun `the shared object mapper is a stable singleton`() {
        assertEquals(Converters.getObjectMapper(), Converters.getObjectMapper())
    }

    @Test
    fun `the shared object mapper writes API timestamps in the WaniKani format`() {
        // Not a @TypeConverter, but this mapper writes JSON into database columns, so its date
        // format is a stored format too. Six-digit microseconds and a literal Z, UTC.
        val instant = java.util.Date(0L)

        val json = Converters.getObjectMapper().writeValueAsString(instant)

        assertEquals("\"1970-01-01T00:00:00.000000Z\"", json)
    }

    @Test
    fun `the shared object mapper ignores unknown properties`() {
        // Deserialization leniency: a new field appearing in stored JSON (or an API payload)
        // must not fail the read. kotlinx.serialization throws by default, so #58/#59 must set
        // ignoreUnknownKeys to preserve this.
        val mapper = Converters.getObjectMapper()

        val parsed = mapper.readValue(
            """{"knownField":"x","aFieldWeHaveNeverSeen":42}""",
            KnownFieldHolder::class.java,
        )

        assertEquals("x", parsed.knownField)
    }

    /** Minimal target for the unknown-property test above. */
    class KnownFieldHolder {
        var knownField: String? = null
    }
}
