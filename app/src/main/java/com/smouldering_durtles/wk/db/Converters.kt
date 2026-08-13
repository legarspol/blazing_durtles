package com.smouldering_durtles.wk.db

import androidx.room.TypeConverter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.smouldering_durtles.wk.enums.KanjiAcceptedReadingType
import com.smouldering_durtles.wk.enums.SessionItemState
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.enums.SubjectType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Various data conversion tools.
 *
 * The nullability here is load-bearing rather than cosmetic. Room derives a column's `NOT NULL`
 * from the Kotlin type it converts to, so a converter's signature and its column's nullability
 * have to agree:
 *
 * - [stringToSessionItemState]/[sessionItemStateToString] and
 *   [stringToKanjiAcceptedReadingType]/[kanjiAcceptedReadingTypeToString] are non-null both ways.
 *   Their columns are `NOT NULL` as of the v70 schema; they were nullable only to keep the
 *   Flaming Durtles schema unchanged, and nothing ever wrote a null.
 * - [stringToSessionType] still accepts null, for a different reason: session type is not a
 *   column at all, it is a row in the key/value `property` table, and it is genuinely absent
 *   until a session starts. `null` there means "no property yet", not "nullable column".
 * - [stringToSubjectType]/[subjectTypeToString] are null-in-null-out because the `object` column
 *   can legitimately hold NULL.
 */
object Converters {
    /**
     * A singleton instance of the object mapper, preconfigured to handle timestamps as needed for the API.
     */
    private var cachedObjectMapper: ObjectMapper? = null

    /**
     * Get a singleton instance of the object mapper, preconfigured to handle timestamps as needed for the API.
     *
     * @return the instance
     */
    @JvmStatic
    fun getObjectMapper(): ObjectMapper {
        var mapper = cachedObjectMapper
        if (mapper == null) {
            mapper = ObjectMapper()
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            mapper.dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.ENGLISH)
            mapper.dateFormat.timeZone = TimeZone.getTimeZone("Z")
            cachedObjectMapper = mapper
        }
        return mapper
    }

    /**
     * Convert a String to an enum value of type SessionItemState.
     *
     * @param value the String value
     * @return the enum instance
     */
    @JvmStatic
    @TypeConverter
    fun stringToSessionItemState(value: String): SessionItemState = SessionItemState.valueOf(value)

    /**
     * Convert an enum value of type SessionItemState to String.
     *
     * @param value the enum value
     * @return the name
     */
    @JvmStatic
    @TypeConverter
    fun sessionItemStateToString(value: SessionItemState): String = value.name

    /**
     * Convert a String (may be null) to an enum value of type SessionType.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun stringToSessionType(value: String?): SessionType {
        if (value == null) {
            return SessionType.NONE
        }
        return SessionType.valueOf(value)
    }

    /**
     * Convert an enum value of type SessionType (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun sessionTypeToString(value: SessionType?): String {
        if (value == null) {
            return SessionType.NONE.name
        }
        return value.name
    }

    /**
     * Convert a String to an enum value of type KanjiAcceptedReadingType.
     *
     * @param value the String value
     * @return the enum instance
     */
    @JvmStatic
    @TypeConverter
    fun stringToKanjiAcceptedReadingType(value: String): KanjiAcceptedReadingType =
        KanjiAcceptedReadingType.valueOf(value)

    /**
     * Convert an enum value of type KanjiAcceptedReadingType to String.
     *
     * @param value the enum value
     * @return the name
     */
    @JvmStatic
    @TypeConverter
    fun kanjiAcceptedReadingTypeToString(value: KanjiAcceptedReadingType): String = value.name

    /**
     * Convert a String (may be null) to an enum value of type SubjectType.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun stringToSubjectType(value: String?): SubjectType? {
        if (value == null) {
            return null
        }
        return SubjectType.from(value)
    }

    /**
     * Convert an enum value of type SubjectType (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun subjectTypeToString(value: SubjectType?): String? {
        if (value == null) {
            return null
        }
        return value.dbTypeName
    }
}
