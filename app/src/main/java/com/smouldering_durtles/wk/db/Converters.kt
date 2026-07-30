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
 * from the Kotlin type it converts to, and the enum columns these back are all nullable in the
 * v68 schema, so the three enum-to-String converters must keep accepting null and coercing it to
 * a default. [stringToSubjectType]/[subjectTypeToString] are genuinely null-in-null-out because
 * the `object` column can legitimately hold NULL.
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
     * Convert a String (may be null) to an enum value of type SessionItemState.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun stringToSessionItemState(value: String?): SessionItemState {
        if (value == null || value == "NEW" || value == "STARTED") {
            return SessionItemState.ACTIVE
        }
        return SessionItemState.valueOf(value)
    }

    /**
     * Convert an enum value of type SessionItemState (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun sessionItemStateToString(value: SessionItemState?): String {
        if (value == null) {
            return SessionItemState.ACTIVE.name
        }
        return value.name
    }

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
     * Convert a String (may be null) to an enum value of type KanjiAcceptedReadingType.
     *
     * @param value the String value or null
     * @return the enum instance or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun stringToKanjiAcceptedReadingType(value: String?): KanjiAcceptedReadingType {
        if (value == null) {
            return KanjiAcceptedReadingType.NEITHER
        }
        return KanjiAcceptedReadingType.valueOf(value)
    }

    /**
     * Convert an enum value of type KanjiAcceptedReadingType (may be null) to String.
     *
     * @param value the enum value or null
     * @return the name or a default if value is null
     */
    @JvmStatic
    @TypeConverter
    fun kanjiAcceptedReadingTypeToString(value: KanjiAcceptedReadingType?): String {
        if (value == null) {
            return KanjiAcceptedReadingType.NEITHER.name
        }
        return value.name
    }

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
