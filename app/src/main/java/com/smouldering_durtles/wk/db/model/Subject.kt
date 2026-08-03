package com.smouldering_durtles.wk.db.model

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.LegacyRadicals
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.api.model.AuxiliaryMeaning
import com.smouldering_durtles.wk.api.model.ContextSentence
import com.smouldering_durtles.wk.api.model.Meaning
import com.smouldering_durtles.wk.api.model.PronunciationAudio
import com.smouldering_durtles.wk.api.model.Reading
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.enums.KanjiAcceptedReadingType
import com.smouldering_durtles.wk.enums.SessionType
import com.smouldering_durtles.wk.enums.SubjectSource
import com.smouldering_durtles.wk.enums.SubjectSource.WANIKANI
import com.smouldering_durtles.wk.enums.SubjectType
import com.smouldering_durtles.wk.model.PitchInfo
import com.smouldering_durtles.wk.model.SrsSystem
import com.smouldering_durtles.wk.model.SrsSystemRepository
import com.smouldering_durtles.wk.util.ObjectSupport.getShortWaitTimeAsInformalString
import com.smouldering_durtles.wk.util.ObjectSupport.isEmpty
import com.smouldering_durtles.wk.util.ObjectSupport.isEqual
import com.smouldering_durtles.wk.util.ObjectSupport.join
import com.smouldering_durtles.wk.util.ObjectSupport.removeDuplicates
import com.smouldering_durtles.wk.util.PseudoIme
import com.smouldering_durtles.wk.util.TextUtil.escapeHtml
import com.smouldering_durtles.wk.util.TextUtil.formatTimestampForDisplay
import com.smouldering_durtles.wk.util.TextUtil.renderHtml
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern

/**
 * Data class for the subject table that encapsulates the entity data more cleanly than the raw entity does.
 *
 * Deliberately kept as `getX()` functions rather than Kotlin properties: this is a 1:1 port, and
 * `AUDIT.md` §7 has this class down for decomposition, so the accessor shapes are left alone until
 * that happens. The three interface members from [PronunciationAudioOwner] are the exception —
 * they have to be properties to override it, and compile to the same `getId()`/`getLevel()`/
 * `getParsedPronunciationAudios()` that Java callers already use.
 */
class Subject(
    /**
     * The backing entity for this subject.
     */
    private val entity: SubjectEntity
) : PronunciationAudioOwner {
    /**
     * Temporary storage for search result ranking.
     */
    var ranking: Int = 0

    init {
        entity.characters = internalize(entity.characters)
        entity.slug = internalize(entity.slug)
    }

    /*
     *************************************************************************************************************************************************
     * Identification
     *************************************************************************************************************************************************
     */

    /**
     * The subject's source, i.e. where does its definition come from?.
     *
     * @return the source
     */
    @Suppress("unused")
    fun getSource(): SubjectSource = WANIKANI

    /**
     * The subject's type (radical, kanji, ...).
     *
     * @return the type
     */
    fun getType(): SubjectType = entity.type ?: SubjectType.WANIKANI_RADICAL

    /**
     * The subject's type order.
     *
     * @return the order
     */
    fun getTypeOrder(): Int = getType().order

    override val id: Long
        get() = entity.id

    override val level: Int
        get() = entity.level

    /**
     * Does this subject need a question in slot 1?.
     *
     * @return true if it does
     */
    fun needsQuestion1(): Boolean = getType().supportsQuestion1() && hasAcceptedMeanings()

    /**
     * Does this subject need a question in slot 2?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    fun needsQuestion2(onkun: Boolean): Boolean = getType().supportsQuestion2(onkun) && hasAcceptedReadings()

    /**
     * Does this subject need a question in slot 3?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    fun needsQuestion3(onkun: Boolean): Boolean = getType().supportsQuestion3(onkun) && hasOnYomi()

    /**
     * Does this subject need a question in slot 4?.
     *
     * @param onkun value of the setting to quiz on'yomi and kun'yomi for kanji separately
     * @return true if it does
     */
    fun needsQuestion4(onkun: Boolean): Boolean = getType().supportsQuestion4(onkun) && hasKunYomi()

    /*
     *************************************************************************************************************************************************
     * The core of the subject's definition. Meanings, readings, audio, etc.
     *************************************************************************************************************************************************
     */

    /**
     * The ordinal position within the level of this subject. Affects ordering but nothing else.
     * @return the value
     */
    fun getLessonPosition(): Int = entity.lessonPosition

    /**
     * The ID of the SRS system that applies to this subject.
     * @return the value
     */
    fun getSrsSystem(): SrsSystem = SrsSystemRepository.getSrsSystem(entity.srsSystemId)

    /**
     * The characters representing this subject, or null for radicals that have no characters.
     * @return the value
     */
    fun getCharacters(): String? = entity.characters

    /**
     * Get the characters for this subject as HTML, tagged as Japanese or containing an image link if needed.
     *
     * @return the characters
     */
    fun getCharactersHtml(): String {
        if (needsTitleImage()) {
            val imageId = getTitleImageId()
            return String.format(Locale.ROOT, "\u200C<title-image-%d>.</title-image-%d>", imageId, imageId)
        }
        val characters = entity.characters ?: return entity.id.toString()
        return "<ja>$characters</ja>"
    }

    /**
     * The slug, which can be used as an alternative to the characters for when it's null.
     * @return the value
     */
    fun getSlug(): String? = entity.slug

    /**
     * The URL for the web site document for this subject.
     * @return the URL
     */
    fun getDocumentUrl(): String? = entity.documentUrl

    /**
     * The meaning mnemonic for this subject.
     * @return the value
     */
    fun getMeaningMnemonic(): String? = entity.meaningMnemonic

    /**
     * The meaning hint for this subject.
     * @return the value
     */
    fun getMeaningHint(): String? = entity.meaningHint

    /**
     * The legacy name for this subject.
     * @return the value
     */
    private fun getLegacyName(): String? = LegacyRadicals.getLegacyName(id)

    /**
     * The legacy mnemonic for this subject.
     * @return the value
     */
    private fun getLegacyMnemonic(): String? = LegacyRadicals.getLegacyMnemonic(id)

    /**
     * Is this a radical with legacy name and mnemonic?.
     * @return true if it is
     */
    fun hasLegacy(): Boolean = LegacyRadicals.isLegacyRadical(id)

    /**
     * The reading mnemonic for this subject.
     * @return the value
     */
    fun getReadingMnemonic(): String? = entity.readingMnemonic

    /**
     * The reading hint for this subject.
     * @return the value
     */
    fun getReadingHint(): String? = entity.readingHint

    /**
     * The meanings for this subject, lazily parsed from JSON.
     * @return the value
     */
    fun getMeanings(): List<Meaning> {
        if (entity.parsedMeanings == null) {
            entity.parsedMeanings = parseJsonList(entity.meanings, object : TypeReference<MutableList<Meaning>>() {})
        }
        return entity.parsedMeanings ?: emptyList()
    }

    /**
     * The auxiliary meanings for this subject, lazily parsed from JSON.
     * @return the value
     */
    fun getAuxiliaryMeanings(): List<AuxiliaryMeaning> {
        if (entity.parsedAuxiliaryMeanings == null) {
            entity.parsedAuxiliaryMeanings =
                parseJsonList(entity.auxiliaryMeanings, object : TypeReference<MutableList<AuxiliaryMeaning>>() {})
        }
        return entity.parsedAuxiliaryMeanings ?: emptyList()
    }

    /**
     * The readings for this subject, lazily parsed from JSON.
     * @return the value
     */
    fun getReadings(): List<Reading> {
        if (entity.parsedReadings == null) {
            entity.parsedReadings = parseJsonList(entity.readings, object : TypeReference<MutableList<Reading>>() {})
        }
        return entity.parsedReadings ?: emptyList()
    }

    /**
     * The IDs of subjects that are components of this subject. For kanji, these are the used radicals.
     * For vocab, these are the used kanji. Lazily parsed from JSON.
     * @return the value
     */
    fun getComponentSubjectIds(): List<Long> {
        if (entity.parsedComponentSubjectIds == null) {
            entity.parsedComponentSubjectIds = parseIdList(entity.componentSubjectIds)
        }
        return entity.parsedComponentSubjectIds ?: emptyList()
    }

    /**
     * The IDs of subjects that this subject is a component of. For radicals, these are the kanji it's used in.
     * For kanji, these are the vocab it's used in. Lazily parsed from JSON.
     * @return the value
     */
    fun getAmalgamationSubjectIds(): List<Long> {
        if (entity.parsedAmalgamationSubjectIds == null) {
            entity.parsedAmalgamationSubjectIds = parseIdList(entity.amalgamationSubjectIds)
        }
        return entity.parsedAmalgamationSubjectIds ?: emptyList()
    }

    /**
     * The IDs of kanji that are visually similar to this kanji. Empty for radicals and vocab. Lazily parsed from JSON.
     * @return the value
     */
    fun getVisuallySimilarSubjectIds(): List<Long> {
        if (entity.parsedVisuallySimilarSubjectIds == null) {
            entity.parsedVisuallySimilarSubjectIds = parseIdList(entity.visuallySimilarSubjectIds)
        }
        return entity.parsedVisuallySimilarSubjectIds ?: emptyList()
    }

    /**
     * This subject's parts of speech. Lazily parsed from JSON.
     * @return the value
     */
    fun getPartsOfSpeech(): List<String> {
        if (entity.parsedPartsOfSpeech == null) {
            entity.parsedPartsOfSpeech =
                parseJsonList(entity.partsOfSpeech, object : TypeReference<MutableList<String>>() {})
        }
        return entity.parsedPartsOfSpeech ?: emptyList()
    }

    /**
     * The context sentences for this subject. Lazily parsed from JSON.
     * @return the value
     */
    fun getContextSentences(): List<ContextSentence> {
        if (entity.parsedContextSentences == null) {
            entity.parsedContextSentences =
                parseJsonList(entity.contextSentences, object : TypeReference<MutableList<ContextSentence>>() {})
        }
        return entity.parsedContextSentences ?: emptyList()
    }

    /**
     * The audio for this vocab, empty for radicals and kanji. Lazily parsed from JSON.
     */
    override val parsedPronunciationAudios: List<PronunciationAudio>
        get() {
            if (entity.parsedPronunciationAudios == null) {
                entity.parsedPronunciationAudios = parseJsonList(
                    entity.pronunciationAudios, object : TypeReference<MutableList<PronunciationAudio>>() {}
                )
            }
            return entity.parsedPronunciationAudios ?: emptyList()
        }

    /**
     * Is this a vocab that forms a prefix?.
     *
     * @return true if it is
     */
    fun isPrefix(): Boolean = entity.characters?.endsWith("〜") == true

    /**
     * Is this a vocab that forms a suffix?.
     *
     * @return true if it is
     */
    fun isSuffix(): Boolean {
        val characters = entity.characters
        return characters != null && characters[0] == '〜'
    }

    /**
     * Does this subject have a non-empty meaning mnemonic?.
     *
     * @return true if it does
     */
    fun hasMeaningMnemonic(): Boolean = !isEmpty(entity.meaningMnemonic)

    /**
     * Does this subject have a non-empty meaning hint?.
     *
     * @return true if it does
     */
    fun hasMeaningHint(): Boolean = !isEmpty(entity.meaningHint)

    /**
     * Get the accepted meanings for this subject (excluding auxiliary and user synonyms).
     *
     * @return the list
     */
    private fun getAcceptedMeanings(): List<Meaning> = getMeanings().filter { it.isAcceptedAnswer }

    /**
     * Does this subject have any meanings?.
     *
     * @return true if it does
     */
    fun hasMeanings(): Boolean = getMeanings().isNotEmpty()

    /**
     * Does this subject have any meanings that are accepted as meaning answers?.
     *
     * @return true if it does
     */
    private fun hasAcceptedMeanings(): Boolean = getMeanings().any { it.isAcceptedAnswer }

    /**
     * Get the number of accepted meaning answers for this subject.
     *
     * @return the number
     */
    private fun getNumAcceptedMeanings(): Int = getMeanings().count { it.isAcceptedAnswer }

    /**
     * Get the 'best' meaning for this subject. Basically, this is the first primary meaning listed.
     *
     * @return the meaning
     */
    fun getOneMeaning(): String = getMeanings().reduceOrNull { accumulator, newValue ->
        when {
            accumulator.isPrimary -> accumulator
            newValue.isPrimary -> newValue
            accumulator.isAcceptedAnswer -> accumulator
            else -> newValue
        }
    }?.meaning ?: ""

    /**
     * Get the accepted readings for this subject.
     *
     * @return the list
     */
    fun getAcceptedReadings(): List<Reading> = getReadings().filter { it.isAcceptedAnswer }

    /**
     * Does this subject have any readings?.
     *
     * @return true if it does
     */
    fun hasReadings(): Boolean = getReadings().isNotEmpty()

    /**
     * Does this subject have any readings that are accepted as reading answers?.
     *
     * @return true if it does
     */
    private fun hasAcceptedReadings(): Boolean = getReadings().any { it.isAcceptedAnswer }

    /**
     * Get the number of accepted reading answers for this subject.
     *
     * @return the number
     */
    private fun getNumAcceptedReadings(): Int = getReadings().count { it.isAcceptedAnswer }

    /**
     * Does this subject have a non-empty reading mnemonic?.
     *
     * @return true if it does
     */
    fun hasReadingMnemonic(): Boolean = !isEmpty(entity.readingMnemonic)

    /**
     * Get the 'best' reading for this subject. Basically, this is the first primary reading listed.
     *
     * @return the meaning
     */
    fun getOneReading(): String = getReadings().reduceOrNull { t, u ->
        when {
            t.isPrimary -> t
            u.isPrimary -> u
            t.isAcceptedAnswer -> t
            else -> u
        }
    }?.getValue(GlobalSettings.Other.getShowOnInKatakana()) ?: ""

    /**
     * Does this subject have a non-empty reading hint?.
     *
     * @return true if it does
     */
    fun hasReadingHint(): Boolean = !isEmpty(entity.readingHint)

    /**
     * Get the on'yomi readings for this subject.
     *
     * @return the list
     */
    fun getOnYomiReadings(): List<Reading> = getReadings().filter { it.isOnYomi }

    /**
     * Get the kun'yomi readings for this subject.
     *
     * @return the list
     */
    fun getKunYomiReadings(): List<Reading> = getReadings().filter { it.isKunYomi }

    /**
     * Does this subject have any on'yomi readings?.
     *
     * @return true if it does
     */
    fun hasOnYomi(): Boolean = getReadings().any { it.isOnYomi }

    /**
     * Does this subject have any kun'yomi readings?.
     *
     * @return true if it does
     */
    fun hasKunYomi(): Boolean = getReadings().any { it.isKunYomi }

    /**
     * Does this subject have any nanori readings?.
     *
     * @return true if it does
     */
    fun hasNanori(): Boolean = getReadings().any { it.isNanori }

    /**
     * Does this subject have any accepted on'yomi readings?.
     *
     * @return true if it does
     */
    fun hasAcceptedOnYomi(): Boolean = getReadings().filter { it.isAcceptedAnswer }.any { it.isOnYomi }

    /**
     * Does this subject have any accepted kun'yomi readings?.
     *
     * @return true if it does
     */
    fun hasAcceptedKunYomi(): Boolean = getReadings().filter { it.isAcceptedAnswer }.any { it.isKunYomi }

    /**
     * Is this string a primary accepted reading for this subject?.
     *
     * @param value the reading
     * @return true if it is
     */
    fun isPrimaryReading(value: String?): Boolean =
        getReadings().filter { it.isPrimary }.any { isEqual(it.reading, value) }

    /**
     * Does this subject have any component subjects?.
     *
     * @return true if it does
     */
    fun hasComponents(): Boolean = getComponentSubjectIds().isNotEmpty()

    /**
     * Does this subject have any amalgamation subjects?.
     *
     * @return true if it does
     */
    fun hasAmalgamations(): Boolean = getAmalgamationSubjectIds().isNotEmpty()

    /**
     * Does this subject have any visually similar subjects?.
     *
     * @return true if it does
     */
    fun hasVisuallySimilar(): Boolean = getVisuallySimilarSubjectIds().isNotEmpty()

    /**
     * Does this subject have a non-empty list of parts of speech?.
     *
     * @return true if it does
     */
    fun hasPartsOfSpeech(): Boolean = getPartsOfSpeech().isNotEmpty()

    /**
     * Does this subject have any context sentences?.
     *
     * @return true if it does
     */
    fun hasContextSentences(): Boolean = getContextSentences().isNotEmpty()

    /**
     * Get the type of reading (on'yomi or kun'yomi) that is required to
     * answer a reading question for this subject.
     *
     * @return the type of reading
     */
    fun getKanjiAcceptedReadingType(): KanjiAcceptedReadingType {
        if (!getType().isKanji) {
            return KanjiAcceptedReadingType.NEITHER
        }

        val onYomi = hasAcceptedOnYomi()
        val kunYomi = hasAcceptedKunYomi()

        if (onYomi && kunYomi) {
            return KanjiAcceptedReadingType.BOTH
        }

        if (onYomi) {
            return KanjiAcceptedReadingType.ONYOMI
        }

        if (kunYomi) {
            return KanjiAcceptedReadingType.KUNYOMI
        }

        return KanjiAcceptedReadingType.NEITHER
    }

    /*
     *************************************************************************************************************************************************
     * Reference data for the subject that is not part of the core.
     *************************************************************************************************************************************************
     */

    /**
     * The frequency (1-2500) in everyday use of a kanji.
     * @return the value
     */
    fun getFrequency(): Int = entity.frequency

    /**
     * The Joyo grade where this kanji is first taught. 0 = not in Joyo, 7 = middle school.
     * @return the value
     */
    fun getJoyoGrade(): Int = entity.joyoGrade

    /**
     * The JLPT level where this kanji/vocab is thought to be required. 0 = not in JLPT, 1-5 = N1-N5.
     * @return the value
     */
    fun getJlptLevel(): Int = entity.jlptLevel

    private fun getPitchInfo(): List<PitchInfo> {
        if (entity.parsedPitchInfo == null) {
            entity.parsedPitchInfo = parseJsonList(entity.pitchInfo, object : TypeReference<MutableList<PitchInfo>>() {})
        }
        return entity.parsedPitchInfo ?: emptyList()
    }

    private fun hasPitchInfoFor(reading: CharSequence): Boolean {
        val kana = PseudoIme.toKatakana(reading)!!
        return getPitchInfo().any { isEqual(kana, it.reading) }
    }

    private fun hasFallbackPitchInfo(): Boolean = getPitchInfo().any { it.reading == null }

    /**
     * Get the pitch info records that apply to a specific reading.
     *
     * @param reading the reading to check
     * @return the list
     */
    fun getPitchInfoFor(reading: CharSequence): List<PitchInfo> {
        val kana = PseudoIme.toKatakana(reading)!!

        val normalMatches = getPitchInfo().filter { isEqual(kana, it.reading) }

        if (normalMatches.isNotEmpty()) {
            return normalMatches
        }

        return getPitchInfo().filter { it.reading == null }
    }

    /**
     * Does this subject need an attempt to download pitch info data?.
     *
     * @param delta the min time in ms between attempts
     * @return true if it does
     */
    fun needsPitchInfoDownload(delta: Long): Boolean {
        val characters = entity.characters
        if (!getType().canHavePitchInfo() || isEmpty(characters)
            || characters!![0] == '〜' || characters.endsWith("〜")
        ) {
            return false
        }
        val pitchInfo = entity.pitchInfo
        if (isEmpty(pitchInfo)) {
            return true
        }
        if (pitchInfo!![0] != '@') {
            return false
        }
        val ts = pitchInfo.substring(1).toLong()
        return (System.currentTimeMillis() - ts) > delta
    }

    /**
     * Get the raw pitch info string stored in the database for this subject. Only used
     * for managing the pitch info reference data.
     *
     * @return the raw data as a JSON encoded string
     */
    fun getRawPitchInfo(): String? = entity.pitchInfo

    /**
     * Does this subject have any pitch info records?.
     *
     * @return true if it does
     */
    fun hasPitchInfo(): Boolean {
        if (!getType().canHavePitchInfo()) {
            return false
        }

        for (reading in getReadings()) {
            val value = reading.reading
            if (isEmpty(value)) {
                continue
            }
            if (hasPitchInfoFor(value!!)) {
                return true
            }
        }

        return hasFallbackPitchInfo()
    }

    /**
     * Does this subject have stroke data?.
     *
     * @return true if it does
     */
    fun hasStrokeData(): Boolean = getType().canHaveStrokeData() && getParsedStrokeData().isNotEmpty()

    /**
     * The stroke data for this radical/kanji, empty for vocab. Lazily parsed from JSON.
     * @return the value
     */
    fun getParsedStrokeData(): List<String> {
        if (entity.parsedStrokeData == null) {
            entity.parsedStrokeData =
                parseJsonList(entity.strokeData, object : TypeReference<MutableList<String>>() {})
        }
        return entity.parsedStrokeData ?: emptyList()
    }

    /**
     * Get the raw stroke data, encoded as a JSON string.
     *
     * @return the stroke data or null if none is available
     */
    fun getStrokeData(): String? = entity.strokeData

    /*
     *************************************************************************************************************************************************
     * The user's assignment data for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * The unique ID of this subject's assignment, or 0 if it doesn't exist.
     * @return the value
     */
    fun getAssignmentId(): Long = entity.assignmentId

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or null if no review is scheduled yet.
     * @return the value
     */
    fun getAvailableAt(): Long = entity.availableAt

    /**
     * The timestamp when the next available review becomes available for this subject,
     * or null if no review is scheduled yet.
     * @param availableAt the value
     */
    fun setAvailableAt(availableAt: Long) {
        entity.availableAt = availableAt
    }

    /**
     * The timestamp when this subject was burned, or 0 if it hasn't been burned yet.
     * @return the value
     */
    fun getBurnedAt(): Long = entity.burnedAt

    /**
     * The timestamp when this subject was passed, i.e. reached Guru I for the first time.
     * Note: for older assignments, this field is not filled in, but the passed boolean is
     * always reliable.
     * @return the value
     */
    fun getPassedAt(): Long = entity.passedAt

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or null if it hasn't been started yet.
     * @return the value
     */
    fun getStartedAt(): Long = entity.startedAt

    /**
     * The timestamp when this subject was started, i.e. when the lesson for this subject was completed,
     * or null if it hasn't been started yet.
     * @param startedAt the value
     */
    fun setStartedAt(startedAt: Long) {
        entity.startedAt = startedAt
    }

    /**
     * The timestamp when this subject was unlocked, or 0 if it is still locked.
     * @return the value
     */
    fun getUnlockedAt(): Long = entity.unlockedAt

    /**
     * The timestamp when this subject was unlocked, or 0 if it is still locked.
     * @param unlockedAt the value
     */
    fun setUnlockedAt(unlockedAt: Long) {
        entity.unlockedAt = unlockedAt
    }

    /**
     * The timestamp when this subject was resurrected from burned status, or 0 if it hasn't been resurrected.
     * @return the value
     */
    fun getResurrectedAt(): Long = entity.resurrectedAt

    /**
     * The SRS stage for this subject.
     * @return the value
     */
    fun getSrsStage(): SrsSystem.Stage =
        SrsSystemRepository.getSrsSystem(entity.srsSystemId).getStage(entity.srsStageId)

    /**
     * The SRS stage for this subject.
     * @param srsStage the value
     */
    fun setSrsStage(srsStage: SrsSystem.Stage) {
        entity.srsStageId = srsStage.id
    }

    /**
     * True if this subject has passed, i.e. has reached Guru I at some point.
     * @return the value
     */
    fun isPassed(): Boolean = entity.passedAt != 0L

    /**
     * True if this subject's assignment has been patched locally but this hasn't been replaced with an API updated version yet.
     * @return the value
     */
    fun isAssignmentPatched(): Boolean = entity.assignmentPatched

    /**
     * Is this subject overdue, i.e. has at least [threshold] percent of the SRS interval
     * elapsed since the latest review became available?.
     *
     * @return true if it is
     */
    fun isOverdue(): Boolean {
        if (entity.availableAt == 0L || isLocked()) {
            return false
        }
        val stage = getSrsStage()
        if (stage.isInitial) {
            return true
        }
        if (stage.isCompleted) {
            return false
        }
        val since = (System.currentTimeMillis() - entity.availableAt).toDouble()
        if (since <= 0) {
            return false
        }
        val interval = stage.interval.toDouble()
        return (since / interval) >= GlobalSettings.AdvancedOther.getOverdueThreshold()
    }

    /**
     * Is this subject eligible to be resurrected?.
     *
     * @return true if it is
     */
    fun isResurrectable(): Boolean {
        if (isEmpty(GlobalSettings.Api.getWebPassword())) {
            return false
        }
        return getSrsStage().isCompleted
    }

    /**
     * Is this subject eligible to be burned?.
     *
     * @return true if it is
     */
    fun isBurnable(): Boolean {
        if (isEmpty(GlobalSettings.Api.getWebPassword())) {
            return false
        }
        return !getSrsStage().isCompleted && getResurrectedAt() != 0L
    }

    /**
     * Is this subject locked?.
     *
     * @return true if it is
     */
    fun isLocked(): Boolean = entity.unlockedAt == 0L

    /**
     * Is this subject eligible for a session of the given type right now?. Ignores level restrictions.
     *
     * @param sessionType type of session to check
     * @return true if it is
     */
    fun isEligibleForSessionType(sessionType: SessionType): Boolean = when (sessionType) {
        SessionType.LESSON ->
            entity.unlockedAt != 0L && entity.startedAt == 0L && (entity.resurrectedAt != 0L || entity.burnedAt == 0L)
        SessionType.REVIEW ->
            entity.availableAt != 0L && entity.availableAt <= System.currentTimeMillis()
        else -> true
    }

    /*
     *************************************************************************************************************************************************
     * The user's review statistics for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * Number of times the meaning has been answered correctly.
     * @return the value
     */
    fun getMeaningCorrect(): Int = entity.meaningCorrect

    /**
     * Number of times the meaning has been answered incorrectly.
     * @return the value
     */
    fun getMeaningIncorrect(): Int = entity.meaningIncorrect

    /**
     * The current streak of correct meaning answers for this subject.
     * @return the value
     */
    fun getMeaningCurrentStreak(): Int = entity.meaningCurrentStreak

    /**
     * The longest streak of correct meaning answers for this subject.
     * @return the value
     */
    fun getMeaningMaxStreak(): Int = entity.meaningMaxStreak

    /**
     * Number of times the reading has been answered correctly.
     * @return the value
     */
    fun getReadingCorrect(): Int = entity.readingCorrect

    /**
     * Number of times the reading has been answered incorrectly.
     * @return the value
     */
    fun getReadingIncorrect(): Int = entity.readingIncorrect

    /**
     * The current streak of correct reading answers for this subject.
     * @return the value
     */
    fun getReadingCurrentStreak(): Int = entity.readingCurrentStreak

    /**
     * The longest streak of correct reading answers for this subject.
     * @return the value
     */
    fun getReadingMaxStreak(): Int = entity.readingMaxStreak

    /**
     * The overall percentage of correct answers for this subject.
     * @return the value
     */
    fun getPercentageCorrect(): Int = entity.percentageCorrect

    /*
     *************************************************************************************************************************************************
     * The user's study materials for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * The unique ID of this subject's study material, or 0 if it doesn't exist.
     * @return the value
     */
    fun getStudyMaterialId(): Long = entity.studyMaterialId

    /**
     * The user's meaning note.
     * @return the value
     */
    fun getMeaningNote(): String? = entity.meaningNote

    /**
     * The user's reading note.
     * @return the value
     */
    fun getReadingNote(): String? = entity.readingNote

    /**
     * The user's meaning synonyms, lazily parsed from JSON.
     * @return the value
     */
    fun getMeaningSynonyms(): List<String> {
        if (entity.parsedMeaningSynonyms == null) {
            entity.parsedMeaningSynonyms =
                parseJsonList(entity.meaningSynonyms, object : TypeReference<MutableList<String>>() {})
        }
        return entity.parsedMeaningSynonyms ?: emptyList()
    }

    /**
     * The user's meaning synonyms, lazily parsed from JSON.
     * @param meaningSynonyms the value
     */
    fun setMeaningSynonyms(meaningSynonyms: List<String>) {
        try {
            entity.meaningSynonyms = Converters.getObjectMapper().writeValueAsString(meaningSynonyms)
            entity.parsedMeaningSynonyms = ArrayList(meaningSynonyms)
        } catch (e: JsonProcessingException) {
            // This can't realistically happen.
        }
    }

    /**
     * Does this subject have a non-empty meaning note?.
     *
     * @return true if it does
     */
    fun hasMeaningNote(): Boolean = !isEmpty(entity.meaningNote)

    /**
     * Does this subject have a non-empty list of user synonyms?.
     *
     * @return true if it does
     */
    fun hasMeaningSynonyms(): Boolean = getMeaningSynonyms().isNotEmpty()

    /**
     * Does this subject have a non-empty reading note?.
     *
     * @return true if it does
     */
    fun hasReadingNote(): Boolean = !isEmpty(entity.readingNote)

    /**
     * Get the star rating the user gave to this item. 0 = no stars, the default. 1-5 are the non-default options.
     *
     * @return the number of start, 0-5
     */
    fun getNumStars(): Int = entity.numStars

    /*
     *************************************************************************************************************************************************
     * Some presentation-related methods for the subject.
     *************************************************************************************************************************************************
     */

    /**
     * Get the type of this subject for the search suggestion box.
     *
     * @return the type
     */
    fun getSearchSuggestionType(): String = getType().shortDescription

    /**
     * Get the type-specific background color for this subject.
     *
     * @return the color
     */
    fun getBackgroundColor(): Int = getType().backgroundColor

    /**
     * Get the type-specific button background color for this subject.
     *
     * @return the color
     */
    fun getButtonBackgroundColor(): Int = getType().buttonBackgroundColor

    /**
     * Get the type-specific text color for this subject.
     *
     * @return the color
     */
    fun getTextColor(): Int = getType().textColor

    /**
     * Get the drawable resource ID for this radical if this is a radical
     * and it should be shown with an image rather than text.
     *
     * @return the ID or 0 if not needed
     */
    fun getTitleImageId(): Int {
        if (!getType().canHaveTitleImage()) {
            return 0
        }
        return when ((entity.id and 0x7FFFFFFFL).toInt()) {
            56 -> R.drawable.radical_56
            114 -> R.drawable.radical_114
            8766 -> R.drawable.radical_8766
            8770 -> R.drawable.radical_8770
            8771 -> R.drawable.radical_8771
            8773 -> R.drawable.radical_8773
            8774 -> R.drawable.radical_8774
            8778 -> R.drawable.radical_8778
            8781 -> R.drawable.radical_8781
            8787 -> R.drawable.radical_8787
            8788 -> R.drawable.radical_8788
            8790 -> R.drawable.radical_8790
            8792 -> R.drawable.radical_8792
            8796 -> R.drawable.radical_8796
            8797 -> R.drawable.radical_8797
            8798 -> R.drawable.radical_8798
            8799 -> R.drawable.radical_8799
            9452 -> R.drawable.radical_9452
            else -> 0
        }
    }

    /**
     * Is this a radical that needs an image instead of characters?.
     *
     * @return true if it is
     */
    fun needsTitleImage(): Boolean {
        if (!getType().canHaveTitleImage()) {
            return false
        }
        return getTitleImageId() != 0
    }

    /**
     * Get the title for the subject info dump for this subject, with the subject title tagged as Japanese.
     *
     * @param prefix A prefix to add to the title
     * @param suffix A suffix to add to the title
     * @return the title
     */
    fun getInfoTitle(prefix: String, suffix: String): CharSequence {
        val charactersHtml = getCharactersHtml()
        val html = String.format(
            Locale.ROOT, "%s%s (level %d %s)%s",
            prefix, charactersHtml, entity.level, getType().infoTitleLabel, suffix
        )
        return renderHtml(html)
    }

    /**
     * Get a short subject info title for use in the info dump itself.
     *
     * @return the title
     */
    fun getSimpleInfoTitle(): String = getType().getSimpleInfoTitle(entity.level)

    /**
     * The availableAt value, formatted for display.
     * @return the formatted value
     */
    fun getFormattedAvailableAt(): String = formatTimestampForDisplay(getAvailableAt())

    /**
     * The burnedAt value, formatted for display.
     * @return the formatted value
     */
    fun getFormattedBurnedAt(): String = formatTimestampForDisplay(getBurnedAt())

    /**
     * The passedAt value, formatted for display.
     * @return the formatted value
     */
    fun getFormattedPassedAt(): String = formatTimestampForDisplay(getPassedAt())

    /**
     * The resurrectedAt value, formatted for display.
     * @return the formatted value
     */
    fun getFormattedResurrectedAt(): String = formatTimestampForDisplay(getResurrectedAt())

    /**
     * The startedAt value, formatted for display.
     * @return the formatted value
     */
    fun getFormattedStartedAt(): String = formatTimestampForDisplay(getStartedAt())

    /**
     * The unlockedAt value, formatted for display.
     * @return the formatted value
     */
    fun getFormattedUnlockedAt(): String = formatTimestampForDisplay(getUnlockedAt())

    /**
     * Get the accepted meanings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    fun getMeaningRichText(prefix: CharSequence): CharSequence {
        val html = getAcceptedMeanings().joinToString(", ", prefix, "") { meaning ->
            if (meaning.isPrimary && getNumAcceptedMeanings() > 1) {
                String.format(Locale.ROOT, "<b>%s</b>", meaning.meaning)
            } else {
                meaning.meaning.toString()
            }
        }
        return renderHtml(html)
    }

    /**
     * Get the meaning mnemonic formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getMeaningMnemonicRichText(): CharSequence {
        val s = getMeaningMnemonic() ?: ""
        return renderHtml("<b>Meaning mnemonic</b>: " + NL_PATTERN.matcher(s).replaceAll("<br>"))
    }

    /**
     * Get the meaning hint formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getMeaningHintRichText(): CharSequence = renderHtml(getMeaningHint() ?: "")

    /**
     * Get the legacy name formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getLegacyNameRichText(): CharSequence = renderHtml("<b>Old name</b>: " + (getLegacyName() ?: ""))

    /**
     * Get the legacy mnemonic formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getLegacyMnemonicRichText(): CharSequence = renderHtml("<b>Old mnemonic</b>: " + (getLegacyMnemonic() ?: ""))

    /**
     * Get the meaning note formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getMeaningNoteRichText(): CharSequence {
        val s = getMeaningNote() ?: ""
        return renderHtml("<b>My meaning note</b>: " + NL_PATTERN.matcher(escapeHtml(s)).replaceAll("<br/>"))
    }

    /**
     * Get the user synonyms formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getMeaningSynonymsRichText(): CharSequence =
        renderHtml("<b>My synonyms</b>: " + escapeHtml(join(", ", "", "", getMeaningSynonyms())))

    /**
     * Get the accepted readings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    fun getRegularReadingRichText(prefix: CharSequence): CharSequence {
        val showOnInKatakana = GlobalSettings.Other.getShowOnInKatakana()
        val html = getAcceptedReadings().joinToString(", ", prefix, "") { reading ->
            if (reading.isPrimary && getNumAcceptedReadings() > 1) {
                String.format(Locale.ROOT, "<b>%s</b>", reading.getValue(showOnInKatakana))
            } else {
                reading.getValue(showOnInKatakana).toString()
            }
        }
        return renderHtml(html)
    }

    /**
     * Get the reading mnemonic formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getReadingMnemonicRichText(): CharSequence {
        val s = getReadingMnemonic() ?: ""
        return renderHtml("<b>Reading mnemonic</b>: " + NL_PATTERN.matcher(s).replaceAll("<br>"))
    }

    /**
     * Get the reading hint formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getReadingHintRichText(): CharSequence = renderHtml(getReadingHint() ?: "")

    /**
     * Get the reading note formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getReadingNoteRichText(): CharSequence {
        val s = getReadingNote() ?: ""
        return renderHtml("<b>My reading note</b>: " + NL_PATTERN.matcher(escapeHtml(s)).replaceAll("<br/>"))
    }

    /**
     * Get the accepted on'yomi readings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    fun getAcceptedOnYomiRichText(prefix: CharSequence): CharSequence {
        val showOnInKatakana = GlobalSettings.Other.getShowOnInKatakana()
        val html = getReadings()
            .filter { it.isOnYomi }
            .filter { !hasAcceptedOnYomi() || it.isAcceptedAnswer }
            .joinToString(", ", prefix, "") { it.getValue(showOnInKatakana).toString() }
        return renderHtml(html)
    }

    /**
     * Get the accepted kun'yomi readings formatted as a piece of rich text.
     *
     * @param prefix prefix to add to the produced text
     * @return the text
     */
    fun getAcceptedKunYomiRichText(prefix: CharSequence): CharSequence {
        val html = getReadings()
            .filter { it.isKunYomi }
            .filter { !hasAcceptedKunYomi() || it.isAcceptedAnswer }
            .joinToString(", ", prefix, "") { it.getValue(false).toString() }
        return renderHtml(html)
    }

    /**
     * Get the on'yomi readings formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getOnYomiRichText(): CharSequence {
        val showOnInKatakana = GlobalSettings.Other.getShowOnInKatakana()
        val html = getReadings()
            .filter { it.isOnYomi }
            .joinToString(", ", "<b>On'yomi:</b> ", "") { it.getValue(showOnInKatakana).toString() }
        return renderHtml(html)
    }

    /**
     * Get the kun'yomi readings formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getKunYomiRichText(): CharSequence {
        val html = getReadings()
            .filter { it.isKunYomi }
            .joinToString(", ", "<b>Kun'yomi:</b> ", "") { it.getValue(false).toString() }
        return renderHtml(html)
    }

    /**
     * Get the nanori readings formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getNanoriRichText(): CharSequence {
        val html = getReadings()
            .filter { it.isNanori }
            .joinToString(", ", "<b>Nanori:</b> ", "") { it.getValue(false).toString() }
        return renderHtml(html)
    }

    /**
     * Get the parts of speech formatted as a piece of rich text.
     *
     * @return the text
     */
    fun getPartsOfSpeechRichText(): CharSequence =
        renderHtml(join(", ", "<b>Part of speech</b>: ", "", getPartsOfSpeech()))

    /**
     * Get the Joyo grade for this subject as a string.
     *
     * @return the grade
     */
    fun getJoyoGradeAsString(): String = when (getJoyoGrade()) {
        1 -> "1"
        2 -> "2"
        3 -> "3"
        4 -> "4"
        5 -> "5"
        6 -> "6"
        7 -> "Middle school"
        else -> "None"
    }

    /**
     * Get the JLPT level for this subject as a string.
     *
     * @return the level
     */
    fun getJlptLevelAsString(): String = when (getJlptLevel()) {
        1 -> "N1"
        2 -> "N2"
        3 -> "N3"
        4 -> "N4"
        5 -> "N5"
        else -> "None"
    }

    /**
     * Get a short informal string describing when the next review will become available.
     *
     * @return the wait time
     */
    fun getShortNextReviewWaitTime(): String {
        if (getAvailableAt() == 0L) {
            return "locked"
        }
        val now = System.currentTimeMillis()
        val next = getAvailableAt()
        return getShortWaitTimeAsInformalString(next - now)
    }

    companion object {
        private val NL_PATTERN: Pattern = Pattern.compile("\n")

        private fun internalize(value: String?): String? = value?.intern()

        /**
         * Parse a JSON array column into a list, falling back to an empty list on unparseable data.
         */
        private fun <T> parseJsonList(json: String?, typeReference: TypeReference<MutableList<T>>): List<T> {
            if (isEmpty(json)) {
                return emptyList()
            }
            return try {
                Converters.getObjectMapper().readValue(json, typeReference)
            } catch (e: IOException) {
                emptyList()
            }
        }

        /**
         * As [parseJsonList], but also strips duplicate IDs, which the subject ID columns can contain.
         */
        private fun parseIdList(json: String?): List<Long> {
            if (isEmpty(json)) {
                return emptyList()
            }
            return try {
                val parsed: MutableList<Long> =
                    Converters.getObjectMapper().readValue(json, object : TypeReference<MutableList<Long>>() {})
                removeDuplicates(parsed)
                parsed
            } catch (e: IOException) {
                emptyList()
            }
        }
    }

    override fun toString(): String {
        return entity.id.toString() + getType().name + " " + entity.characters + " " + getOneMeaning()
    }
}
