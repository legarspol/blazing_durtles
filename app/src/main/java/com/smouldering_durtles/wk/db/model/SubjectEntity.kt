package com.smouldering_durtles.wk.db.model

import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.smouldering_durtles.wk.api.model.AuxiliaryMeaning
import com.smouldering_durtles.wk.api.model.ContextSentence
import com.smouldering_durtles.wk.api.model.Meaning
import com.smouldering_durtles.wk.api.model.PronunciationAudio
import com.smouldering_durtles.wk.api.model.Reading
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.model.PitchInfo

/**
 * Room entity for the subject table. This class combines all information about a subject in
 * a single entity for convenience - subject, assignment, review statistics, study materials
 * and reference data.
 *
 * This is a query result holder, not the `@Entity` — the schema is defined by
 * [SubjectEntityDefinition]. That split is what lets the timestamp fields here be primitive
 * `Long` (0 meaning "not set") while the columns themselves stay nullable.
 */
class SubjectEntity {
    // From base subject

    /**
     * The unique ID.
     */
    @JvmField var id: Long = 0L

    /**
     * The type of subject, one of "radical", "kanji", "vocabulary", "kana_vocabulary".
     */
    @JvmField
    @ColumnInfo(name = "object")
    var type: SubjectType? = null

    /**
     * The star rating (0-5) given to the subject by the user.
     */
    @JvmField var numStars: Int = 0

    /**
     * Timestamp when this subject was hidden, or null if it isn't hidden.
     * A hidden subject is still maintained, but is treated as non-existent almost everywhere.
     */
    @JvmField var hiddenAt: Long = 0L

    /**
     * The ordinal position within the level of this subject. Affects ordering but nothing else.
     */
    @JvmField var lessonPosition: Int = 0

    /**
     * The ID of the SRS system that applies to this subject.
     */
    @JvmField var srsSystemId: Long = 0L

    /**
     * The level this subject belongs to.
     */
    @JvmField @ColumnInfo(index = true) var level: Int = 0

    /**
     * The characters representing this subject, or null for radicals that have no characters.
     */
    @JvmField @ColumnInfo(index = true) var characters: String? = null

    /**
     * The slug, which can be used as an alternative to the characters for when it's null.
     */
    @JvmField var slug: String? = null

    /**
     * The URL for the weg page for this subject.
     */
    @JvmField var documentUrl: String? = null

    /**
     * The registered meanings for this subject. Encoded as a JSON string.
     */
    @JvmField var meanings: String? = null

    /**
     * The meaning mnemonic for this subject.
     */
    @JvmField var meaningMnemonic: String? = null

    /**
     * The meaning hint for this subject.
     */
    @JvmField var meaningHint: String? = null

    /**
     * The registered auxiliary meanings for this subject. Encoded as a JSON string.
     */
    @JvmField var auxiliaryMeanings: String? = null

    /**
     * The registered readings for this subject. Encoded as a JSON string.
     */
    @JvmField var readings: String? = null

    /**
     * The reading mnemonic for this subject.
     */
    @JvmField var readingMnemonic: String? = null

    /**
     * The reading hint for this subject.
     */
    @JvmField var readingHint: String? = null

    /**
     * The IDs of subjects that are components of this subject. For kanji, these are the used radicals.
     * For vocab, these are the used kanji. Encoded as a JSON string.
     */
    @JvmField var componentSubjectIds: String? = null

    /**
     * The IDs of subjects that this subject is a component of. For radicals, these are the kanji it's used in.
     * For kanji, these are the vocab it's used in. Encoded as a JSON string.
     */
    @JvmField var amalgamationSubjectIds: String? = null

    /**
     * The IDs of kanji that are visually similar to this kanji. Empty for radicals and vocab. Encoded as a JSON string.
     */
    @JvmField var visuallySimilarSubjectIds: String? = null

    /**
     * This subject's parts of speech. Encoded as a JSON string.
     */
    @JvmField var partsOfSpeech: String? = null

    /**
     * The context sentences for this subject. Encoded as a JSON string.
     */
    @JvmField var contextSentences: String? = null

    /**
     * The audio for this vocab, empty for radicals and kanji. Encoded as a JSON string.
     */
    @JvmField var pronunciationAudios: String? = null

    /**
     * A concatenation of all searchable text in this subject. Used to speed up searches.
     */
    @JvmField var searchTarget: String? = null

    /**
     * A concatenation of the most important searchable text in this subject. Used to speed up searches.
     */
    @JvmField var smallSearchTarget: String? = null

    /**
     * The unique ID of this subject's assignment, or 0 if it doesn't exist.
     */
    @JvmField var assignmentId: Long = 0L

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or null if no review is scheduled yet.
     */
    @JvmField @ColumnInfo(index = true) var availableAt: Long = 0L

    /**
     * The timestamp when this subject was burned, or null if it hasn't been burned yet.
     */
    @JvmField @ColumnInfo(index = true) var burnedAt: Long = 0L

    /**
     * The timestamp when this subject was passed, i.e. reached Guru I for the first time.
     * Note: for older assignments, this field used to be empty. They have been backfilled since then.
     */
    @JvmField var passedAt: Long = 0L

    /**
     * The timestamp when this subject was resurrected from burned status, or null if it hasn't been resurrected.
     */
    @JvmField var resurrectedAt: Long = 0L

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or null if it hasn't been started yet.
     */
    @JvmField var startedAt: Long = 0L

    /**
     * The timestamp when this subject was unlocked, or null if it is still locked.
     */
    @JvmField var unlockedAt: Long = 0L

    /**
     * The current SRS stage for this subject.
     */
    @JvmField @ColumnInfo(index = true, name = "srsStage") var srsStageId: Long = 0L

    /**
     * The timestamp when the last incorrect answer was given for this subject.
     */
    @JvmField var lastIncorrectAnswer: Long = 0L

    /**
     * True if this subject's assignment has been patched locally but this hasn't been replaced with an API updated version yet.
     */
    @JvmField var assignmentPatched: Boolean = false

    // From study material

    /**
     * The unique ID of this subject's study material, or 0 if it doesn't exist.
     */
    @JvmField var studyMaterialId: Long = 0L

    /**
     * The user's meaning note.
     */
    @JvmField var meaningNote: String? = null

    /**
     * The user's meaning synonyms. Encoded as a JSON string.
     */
    @JvmField var meaningSynonyms: String? = null

    /**
     * The user's reading note.
     */
    @JvmField var readingNote: String? = null

    /**
     * True if this subject's study material has been patched locally but this hasn't been replaced with an API updated version yet.
     */
    @JvmField var studyMaterialPatched: Boolean = false

    // From review statistics

    /**
     * The unique ID of this subject's review statistics, or 0 if it doesn't exist.
     */
    @JvmField var reviewStatisticId: Long = 0L

    /**
     * Number of times the meaning has been answered correctly.
     */
    @JvmField var meaningCorrect: Int = 0

    /**
     * Number of times the meaning has been answered incorrectly.
     */
    @JvmField var meaningIncorrect: Int = 0

    /**
     * The longest streak of correct meaning answers for this subject.
     */
    @JvmField var meaningMaxStreak: Int = 0

    /**
     * The current streak of correct meaning answers for this subject.
     */
    @JvmField var meaningCurrentStreak: Int = 0

    /**
     * Number of times the reading has been answered correctly.
     */
    @JvmField var readingCorrect: Int = 0

    /**
     * Number of times the reading has been answered incorrectly.
     */
    @JvmField var readingIncorrect: Int = 0

    /**
     * The longest streak of correct reading answers for this subject.
     */
    @JvmField var readingMaxStreak: Int = 0

    /**
     * The current streak of correct reading answers for this subject.
     */
    @JvmField var readingCurrentStreak: Int = 0

    /**
     * The overall percentage of correct answers for this subject.
     */
    @JvmField var percentageCorrect: Int = 0

    /**
     * The leech score for this subject, precomputed for the self-study quiz filters.
     */
    @JvmField var leechScore: Int = 0

    /**
     * True if this subject's review statistic has been patched locally but this hasn't been replaced with an API updated version yet.
     */
    @JvmField var statisticPatched: Boolean = false

    // From reference data

    /**
     * The frequency (1-2500) in everyday use of a kanji.
     */
    @JvmField var frequency: Int = 0

    /**
     * The Joyo grade where this kanji is first taught. 0 = not in Joyo, 7 = middle school.
     */
    @JvmField var joyoGrade: Int = 0

    /**
     * The JLPT level where this kanji/vocab is thought to be required. 0 = not in JLPT, 1-5 = N1-N5.
     */
    @JvmField var jlptLevel: Int = 0

    /**
     * The pitch info for this subject.
     */
    @JvmField var pitchInfo: String? = null

    /**
     * The stroke order data for this subject.
     */
    @JvmField var strokeData: String? = null

    /**
     * Parsed version of meanings, inflated on demand.
     */
    @JvmField @Ignore var parsedMeanings: List<Meaning>? = null

    /**
     * Parsed version of auxiliaryMeanings, inflated on demand.
     */
    @JvmField @Ignore var parsedAuxiliaryMeanings: List<AuxiliaryMeaning>? = null

    /**
     * Parsed version of readings, inflated on demand.
     */
    @JvmField @Ignore var parsedReadings: List<Reading>? = null

    /**
     * Parsed version of componentSubjectIds, inflated on demand.
     */
    @JvmField @Ignore var parsedComponentSubjectIds: List<Long>? = null

    /**
     * Parsed version of amalgamationSubjectIds, inflated on demand.
     */
    @JvmField @Ignore var parsedAmalgamationSubjectIds: List<Long>? = null

    /**
     * Parsed version of visuallySimilarSubjectIds, inflated on demand.
     */
    @JvmField @Ignore var parsedVisuallySimilarSubjectIds: List<Long>? = null

    /**
     * Parsed version of partsOfSpeech, inflated on demand.
     */
    @JvmField @Ignore var parsedPartsOfSpeech: List<String>? = null

    /**
     * Parsed version of contextSentences, inflated on demand.
     */
    @JvmField @Ignore var parsedContextSentences: List<ContextSentence>? = null

    /**
     * Parsed version of pronunciationAudios, inflated on demand.
     */
    @JvmField @Ignore var parsedPronunciationAudios: List<PronunciationAudio>? = null

    /**
     * Parsed version of meaningSynonyms, inflated on demand.
     */
    @JvmField @Ignore var parsedMeaningSynonyms: List<String>? = null

    /**
     * Parsed version of meaningSynonyms, inflated on demand.
     */
    @JvmField @Ignore var parsedPitchInfo: List<PitchInfo>? = null

    /**
     * Parsed version of meaningSynonyms, inflated on demand.
     */
    @JvmField @Ignore var parsedStrokeData: List<String>? = null
}
