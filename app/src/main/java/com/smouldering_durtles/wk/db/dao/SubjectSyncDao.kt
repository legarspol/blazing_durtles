package com.smouldering_durtles.wk.db.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Dao
import androidx.room.Query
import com.fasterxml.jackson.core.JsonProcessingException
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.api.model.ApiAssignment
import com.smouldering_durtles.wk.api.model.ApiReviewStatistic
import com.smouldering_durtles.wk.api.model.ApiStudyMaterial
import com.smouldering_durtles.wk.api.model.ApiSubject
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.db.model.Subject
import com.smouldering_durtles.wk.db.model.SubjectEntity
import com.smouldering_durtles.wk.livedata.SubjectChangeWatcher
import com.smouldering_durtles.wk.util.Logger
import com.smouldering_durtles.wk.util.ReferenceDataUtil
import com.smouldering_durtles.wk.util.SearchUtil
import kotlin.math.max
import kotlin.math.pow

/**
 * DAO for subjects.
 */
@Dao
abstract class SubjectSyncDao {
    /**
     * Room-generated method: get a single subject by ID.
     *
     * @param id the subject's ID
     * @return the subject or null if not found
     */
    @Query("SELECT * FROM subject WHERE id = :id")
    protected abstract fun getByIdHelper(id: Long): SubjectEntity?

    /**
     * Get a single subject by ID.
     *
     * @param id the subject's ID
     * @return the subject or null if not found
     */
    private fun getById(id: Long): Subject? {
        val entity = getByIdHelper(id)
        return if (entity == null) null else Subject(entity)
    }

    // Note: the following are a bunch of methods that offer a very convoluted way to insert and update
    // subjects in the database. It's ugly and could be a lot cleaner, but this approach makes the
    // first time setup (somewhat) acceptably fast.

    // Don't start on refactoring this until you're very sure you can do it, and you're ready for weird
    // and subtle bugs and have a plan for performance problems.

    /**
     * Room-generated method: try to update a subject record. This covers the core subject data
     * from the API, and the static reference data that is not user-specific.
     *
     * @param subjectId subject ID
     * @param object subject field
     * @param characters subject field
     * @param slug subject field
     * @param documentUrl subject field
     * @param meaningMnemonic subject field
     * @param meaningHint subject field
     * @param readingMnemonic subject field
     * @param readingHint subject field
     * @param searchTarget subject field
     * @param smallSearchTarget subject field
     * @param meanings subject field
     * @param auxiliaryMeanings subject field
     * @param readings subject field
     * @param componentSubjectIds subject field
     * @param amalgamationSubjectIds subject field
     * @param visuallySimilarSubjectIds subject field
     * @param partsOfSpeech subject field
     * @param contextSentences subject field
     * @param pronunciationAudios subject field
     * @param lessonPosition subject field
     * @param level subject field
     * @param hiddenAt subject field
     * @param frequency subject field
     * @param joyoGrade subject field
     * @param jlptLevel subject field
     * @param pitchInfo subject field
     * @param strokeData subject field
     * @param srsSystemId subject field
     * @return true if there was a record to update
     */
    @Query(
        "UPDATE subject SET" +
            " object = :object," +
            " characters = :characters," +
            " slug = :slug," +
            " documentUrl = :documentUrl," +
            " meaningMnemonic = :meaningMnemonic," +
            " meaningHint = :meaningHint," +
            " readingMnemonic = :readingMnemonic," +
            " readingHint = :readingHint," +
            " searchTarget = :searchTarget," +
            " smallSearchTarget = :smallSearchTarget," +
            " meanings = :meanings," +
            " auxiliaryMeanings = :auxiliaryMeanings," +
            " readings = :readings," +
            " componentSubjectIds = :componentSubjectIds," +
            " amalgamationSubjectIds = :amalgamationSubjectIds," +
            " visuallySimilarSubjectIds = :visuallySimilarSubjectIds," +
            " partsOfSpeech = :partsOfSpeech," +
            " contextSentences = :contextSentences," +
            " pronunciationAudios = :pronunciationAudios," +
            " lessonPosition = :lessonPosition," +
            " level = :level," +
            " hiddenAt = :hiddenAt," +
            " frequency = :frequency," +
            " joyoGrade = :joyoGrade," +
            " jlptLevel = :jlptLevel," +
            " pitchInfo = :pitchInfo," +
            " strokeData = :strokeData," +
            " srsSystemId = :srsSystemId" +
            " WHERE id = :subjectId"
    )
    protected abstract fun tryUpdateHelper(
        subjectId: Long,
        `object`: String?,
        characters: String?,
        slug: String?,
        documentUrl: String?,
        meaningMnemonic: String?,
        meaningHint: String?,
        readingMnemonic: String?,
        readingHint: String?,
        searchTarget: String,
        smallSearchTarget: String,
        meanings: String,
        auxiliaryMeanings: String,
        readings: String,
        componentSubjectIds: String,
        amalgamationSubjectIds: String,
        visuallySimilarSubjectIds: String,
        partsOfSpeech: String,
        contextSentences: String,
        pronunciationAudios: String,
        lessonPosition: Int,
        level: Int,
        hiddenAt: Long,
        frequency: Int,
        joyoGrade: Int,
        jlptLevel: Int,
        pitchInfo: String?,
        strokeData: String?,
        srsSystemId: Long
    ): Int

    /**
     * Try to update a subject record from an API subject instance.
     *
     * @param apiSubject the API subject to pull data from
     * @return true if there was a record to update
     */
    private fun tryUpdate(apiSubject: ApiSubject): Boolean {
        val type = Converters.stringToSubjectType(apiSubject.getObject())
        val count = tryUpdateHelper(
            apiSubject.id,
            apiSubject.getObject(),
            apiSubject.characters,
            apiSubject.slug,
            apiSubject.documentUrl,
            apiSubject.meaningMnemonic,
            apiSubject.meaningHint,
            apiSubject.readingMnemonic,
            apiSubject.readingHint,
            SearchUtil.findSearchTarget(apiSubject),
            SearchUtil.findSmallSearchTarget(apiSubject),
            serializeToJsonString(apiSubject.meanings),
            serializeToJsonString(apiSubject.auxiliaryMeanings),
            serializeToJsonString(apiSubject.readings),
            serializeToJsonString(apiSubject.componentSubjectIds),
            serializeToJsonString(apiSubject.amalgamationSubjectIds),
            serializeToJsonString(apiSubject.visuallySimilarSubjectIds),
            serializeToJsonString(apiSubject.partsOfSpeech),
            serializeToJsonString(apiSubject.contextSentences),
            serializeToJsonString(apiSubject.pronunciationAudios),
            apiSubject.lessonPosition,
            apiSubject.level,
            apiSubject.hiddenAt,
            ReferenceDataUtil.getFrequency(type, apiSubject.characters),
            ReferenceDataUtil.getJoyoGrade(type, apiSubject.characters),
            ReferenceDataUtil.getJlptLevel(type, apiSubject.characters),
            ReferenceDataUtil.getPitchInfo(type, apiSubject.characters),
            ReferenceDataUtil.getStrokeData(type, apiSubject.id, apiSubject.characters),
            apiSubject.srsSystemId
        )
        return count > 0
    }

    /**
     * Room-generated method: try to insert a new subject record. This covers the core subject data
     * from the API, and the static reference data that is not user-specific.
     *
     * @param subjectId the subject ID
     * @param object subject field
     * @param characters subject field
     * @param slug subject field
     * @param documentUrl subject field
     * @param meaningMnemonic subject field
     * @param meaningHint subject field
     * @param readingMnemonic subject field
     * @param readingHint subject field
     * @param searchTarget subject field
     * @param smallSearchTarget subject field
     * @param meanings subject field
     * @param auxiliaryMeanings subject field
     * @param readings subject field
     * @param componentSubjectIds subject field
     * @param amalgamationSubjectIds subject field
     * @param visuallySimilarSubjectIds subject field
     * @param partsOfSpeech subject field
     * @param contextSentences subject field
     * @param pronunciationAudios subject field
     * @param lessonPosition subject field
     * @param level subject field
     * @param hiddenAt subject field
     * @param frequency subject field
     * @param joyoGrade subject field
     * @param jlptLevel subject field
     * @param pitchInfo subject field
     * @param strokeData subject field
     * @param srsSystemId subject field
     */
    @Query(
        "INSERT INTO subject" +
            " (id, object, characters, slug, documentUrl, meaningMnemonic, meaningHint, readingMnemonic, readingHint, searchTarget, smallSearchTarget," +
            " meanings, auxiliaryMeanings, readings, componentSubjectIds, amalgamationSubjectIds, visuallySimilarSubjectIds," +
            " partsOfSpeech, contextSentences, pronunciationAudios," +
            " typeCode, lessonPosition, level, hiddenAt, frequency, joyoGrade, jlptLevel, pitchInfo, strokeData, srsSystemId," +
            " assignmentId, passed, resurrected, srsStage, assignmentPatched, studyMaterialId, studyMaterialPatched," +
            " reviewStatisticId, meaningCorrect, meaningIncorrect, meaningMaxStreak, meaningCurrentStreak," +
            " readingCorrect, readingIncorrect, readingMaxStreak, readingCurrentStreak, percentageCorrect," +
            " statisticPatched, leechScore, levelProgressScore, audioDownloadStatus," +
            " resurrectedAt, burnedAt, unlockedAt, startedAt, passedAt, availableAt, lastIncorrectAnswer" +
            " )" +
            " VALUES (:subjectId, :object, :characters, :slug, :documentUrl, :meaningMnemonic, :meaningHint, :readingMnemonic, :readingHint," +
            " :searchTarget, :smallSearchTarget," +
            " :meanings, :auxiliaryMeanings, :readings, :componentSubjectIds, :amalgamationSubjectIds, :visuallySimilarSubjectIds," +
            " :partsOfSpeech, :contextSentences, :pronunciationAudios," +
            " 0, :lessonPosition, :level, :hiddenAt," +
            " :frequency, :joyoGrade, :jlptLevel, :pitchInfo, :strokeData, :srsSystemId," +
            " 0, 0, 0, -999, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0," +
            " 0, 0, 0, 0, 0, 0, 0" +
            ")"
    )
    protected abstract fun tryInsertHelper(
        subjectId: Long,
        `object`: String?,
        characters: String?,
        slug: String?,
        documentUrl: String?,
        meaningMnemonic: String?,
        meaningHint: String?,
        readingMnemonic: String?,
        readingHint: String?,
        searchTarget: String,
        smallSearchTarget: String,
        meanings: String,
        auxiliaryMeanings: String,
        readings: String,
        componentSubjectIds: String,
        amalgamationSubjectIds: String,
        visuallySimilarSubjectIds: String,
        partsOfSpeech: String,
        contextSentences: String,
        pronunciationAudios: String,
        lessonPosition: Int,
        level: Int,
        hiddenAt: Long,
        frequency: Int,
        joyoGrade: Int,
        jlptLevel: Int,
        pitchInfo: String?,
        strokeData: String?,
        srsSystemId: Long
    )

    /**
     * Try to insert a subject record from an API subject instance.
     *
     * @param apiSubject the API subject to pull data from
     * @return true if the insert was successful (false if it already existed)
     */
    private fun tryInsert(apiSubject: ApiSubject): Boolean {
        try {
            val type = Converters.stringToSubjectType(apiSubject.getObject())
            tryInsertHelper(
                apiSubject.id,
                apiSubject.getObject(),
                apiSubject.characters,
                apiSubject.slug,
                apiSubject.documentUrl,
                apiSubject.meaningMnemonic,
                apiSubject.meaningHint,
                apiSubject.readingMnemonic,
                apiSubject.readingHint,
                SearchUtil.findSearchTarget(apiSubject),
                SearchUtil.findSmallSearchTarget(apiSubject),
                serializeToJsonString(apiSubject.meanings),
                serializeToJsonString(apiSubject.auxiliaryMeanings),
                serializeToJsonString(apiSubject.readings),
                serializeToJsonString(apiSubject.componentSubjectIds),
                serializeToJsonString(apiSubject.amalgamationSubjectIds),
                serializeToJsonString(apiSubject.visuallySimilarSubjectIds),
                serializeToJsonString(apiSubject.partsOfSpeech),
                serializeToJsonString(apiSubject.contextSentences),
                serializeToJsonString(apiSubject.pronunciationAudios),
                apiSubject.lessonPosition,
                apiSubject.level,
                apiSubject.hiddenAt,
                ReferenceDataUtil.getFrequency(type, apiSubject.characters),
                ReferenceDataUtil.getJoyoGrade(type, apiSubject.characters),
                ReferenceDataUtil.getJlptLevel(type, apiSubject.characters),
                ReferenceDataUtil.getPitchInfo(type, apiSubject.characters),
                ReferenceDataUtil.getStrokeData(type, apiSubject.id, apiSubject.characters),
                apiSubject.srsSystemId
            )
        } catch (e: SQLiteConstraintException) {
            return false
        }
        return true
    }

    /**
     * Room-generated method: insert an empty subject into the database. This is used to prepare an update with
     * an assignment or something like that for which the subject doesn't exist yet. The subject will be
     * effectively useless until the core subject data is included as well.
     *
     * @param id the subject ID
     */
    @Query(
        "INSERT INTO subject (id," +
            " assignmentId, passed, resurrected, srsStage, assignmentPatched, studyMaterialId, studyMaterialPatched," +
            " reviewStatisticId, meaningCorrect, meaningIncorrect, meaningMaxStreak, meaningCurrentStreak," +
            " readingCorrect, readingIncorrect, readingMaxStreak, readingCurrentStreak, percentageCorrect," +
            " statisticPatched, frequency, joyoGrade, jlptLevel, levelProgressScore, leechScore, srsSystemId," +
            " resurrectedAt, burnedAt, unlockedAt, startedAt, passedAt, availableAt, hiddenAt, lastIncorrectAnswer" +
            ") VALUES (:id," +
            " 0, 0, 0, -999, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0," +
            " 0, 0, 0, 0, 0, 0, 0, 0" +
            ")"
    )
    protected abstract fun tryInsertHelperIdOnly(id: Long)

    /**
     * Try to insert an empty subject into the database, see tryInsertHelperIdOnly. Ignore the exception if the
     * subject already exists.
     *
     * @param id the subject ID
     */
    private fun tryInsertIdOnly(id: Long) {
        try {
            tryInsertHelperIdOnly(id)
        } catch (e: SQLiteConstraintException) {
            //
        }
    }

    /**
     * Insert or update an API subject depending on whether it exists in the database already.
     *
     * @param apiSubject the API subject
     * @param existingSubjectIds the set of existing subject IDs, to predict the likely (non-)existence of the subject
     */
    fun insertOrUpdate(apiSubject: ApiSubject, existingSubjectIds: Collection<Long>) {
        if (existingSubjectIds.contains(apiSubject.id)) {
            val updated = tryUpdate(apiSubject)
            if (!updated) {
                tryInsert(apiSubject)
            }
        } else {
            val inserted = tryInsert(apiSubject)
            if (!inserted) {
                tryUpdate(apiSubject)
            }
        }
        SubjectChangeWatcher.getInstance().reportChange(apiSubject.id)
    }

    /**
     * Room-generated method: update a subject with data from an assignment.
     *
     * @param subjectId the subject ID
     * @param assignmentId the assignment ID
     * @param srsStageId the assignment field
     * @param availableAt the assignment field
     * @param burnedAt the assignment field
     * @param passedAt the assignment field
     * @param resurrectedAt the assignment field
     * @param startedAt the assignment field
     * @param unlockedAt the assignment field
     * @return 0 if the subject doesn't exist yet
     */
    @Query(
        "UPDATE subject SET" +
            " assignmentId = :assignmentId," +
            " srsStage = :srsStageId," +
            " availableAt = :availableAt," +
            " burnedAt = :burnedAt," +
            " passedAt = :passedAt," +
            " resurrectedAt = :resurrectedAt," +
            " startedAt = :startedAt," +
            " unlockedAt = :unlockedAt," +
            " assignmentPatched = 0" +
            " WHERE id = :subjectId"
    )
    protected abstract fun tryUpdateHelperAssignment(
        subjectId: Long,
        assignmentId: Long,
        srsStageId: Long,
        availableAt: Long,
        burnedAt: Long,
        passedAt: Long,
        resurrectedAt: Long,
        startedAt: Long,
        unlockedAt: Long
    ): Int

    /**
     * Try to update a subject record from an API assignment instance.
     *
     * @param apiAssignment the API assignment to pull data from
     * @return true if there was a record to update
     */
    private fun tryUpdateAssignment(apiAssignment: ApiAssignment): Boolean {
        val count = tryUpdateHelperAssignment(
            apiAssignment.subjectId,
            apiAssignment.id,
            if (apiAssignment.unlockedAt == 0L) -999 else apiAssignment.srsStageId,
            apiAssignment.availableAt,
            apiAssignment.burnedAt,
            apiAssignment.passedAt,
            apiAssignment.resurrectedAt,
            apiAssignment.startedAt,
            apiAssignment.unlockedAt
        )
        return count > 0
    }

    /**
     * Insert or update an API assignment depending on whether it exists in the database already.
     *
     * @param apiAssignment the API assignment
     */
    fun insertOrUpdateAssignment(apiAssignment: ApiAssignment) {
        val updated = tryUpdateAssignment(apiAssignment)
        if (!updated) {
            tryInsertIdOnly(apiAssignment.subjectId)
            tryUpdateAssignment(apiAssignment)
        }
        SubjectChangeWatcher.getInstance().reportChange(apiAssignment.subjectId)
    }

    /**
     * Room-generated method: update a subject with data from a study material.
     *
     * @param subjectId the subject ID
     * @param studyMaterialId the study material ID
     * @param meaningNote study material field
     * @param meaningSynonyms study material field
     * @param readingNote study material field
     * @return 0 if the subject doesn't exist yet
     */
    @Query(
        "UPDATE subject SET" +
            " studyMaterialId = :studyMaterialId," +
            " meaningNote = :meaningNote," +
            " meaningSynonyms = :meaningSynonyms," +
            " readingNote = :readingNote," +
            " studyMaterialPatched = 0" +
            " WHERE id = :subjectId"
    )
    protected abstract fun tryUpdateHelperStudyMaterial(
        subjectId: Long,
        studyMaterialId: Long,
        meaningNote: String?,
        meaningSynonyms: String,
        readingNote: String?
    ): Int

    /**
     * Room-generated method: update a subject with data from a study material.
     *
     * @param subjectId the subject ID
     * @param meaningNote study material field
     * @param meaningSynonyms study material field
     * @param readingNote study material field
     * @return 0 if the subject doesn't exist yet
     */
    @Query(
        "UPDATE subject SET" +
            " meaningNote = :meaningNote," +
            " meaningSynonyms = :meaningSynonyms," +
            " readingNote = :readingNote," +
            " studyMaterialPatched = 1" +
            " WHERE id = :subjectId"
    )
    protected abstract fun tryUpdateHelperStudyMaterial(
        subjectId: Long,
        meaningNote: String?,
        meaningSynonyms: String,
        readingNote: String?
    ): Int

    /**
     * Try to update a subject record from an API study material instance.
     *
     * @param apiStudyMaterial the API study material to pull data from
     * @param patched if true, leave the study material ID in the subject alone
     * @return true if there was a record to update
     */
    private fun tryUpdateStudyMaterial(apiStudyMaterial: ApiStudyMaterial, patched: Boolean): Boolean {
        val count = if (patched) {
            tryUpdateHelperStudyMaterial(
                apiStudyMaterial.subjectId,
                apiStudyMaterial.meaningNote,
                serializeToJsonString(apiStudyMaterial.meaningSynonyms),
                apiStudyMaterial.readingNote
            )
        } else {
            tryUpdateHelperStudyMaterial(
                apiStudyMaterial.subjectId,
                apiStudyMaterial.id,
                apiStudyMaterial.meaningNote,
                serializeToJsonString(apiStudyMaterial.meaningSynonyms),
                apiStudyMaterial.readingNote
            )
        }
        return count > 0
    }

    /**
     * Insert or update an API study material depending on whether it exists in the database already.
     *
     * @param apiStudyMaterial the API study material
     * @param patched if true, leave the study material ID in the subject alone
     */
    fun insertOrUpdateStudyMaterial(apiStudyMaterial: ApiStudyMaterial, patched: Boolean) {
        val updated = tryUpdateStudyMaterial(apiStudyMaterial, patched)
        if (!updated) {
            tryInsertIdOnly(apiStudyMaterial.subjectId)
            tryUpdateStudyMaterial(apiStudyMaterial, patched)
        }
        SubjectChangeWatcher.getInstance().reportChange(apiStudyMaterial.subjectId)
    }

    /**
     * Room-generated method: update a subject with data from a study material.
     *
     * @param subjectId the subject ID
     * @param reviewStatisticId the review statistic ID
     * @param meaningCorrect review statistic field
     * @param meaningIncorrect review statistic field
     * @param meaningCurrentStreak review statistic field
     * @param meaningMaxStreak review statistic field
     * @param readingCorrect review statistic field
     * @param readingIncorrect review statistic field
     * @param readingCurrentStreak review statistic field
     * @param readingMaxStreak review statistic field
     * @param percentageCorrect review statistic field
     * @param leechScore review statistic field
     * @return 0 if the subject doesn't exist yet
     */
    @Query(
        "UPDATE subject SET" +
            " reviewStatisticId = :reviewStatisticId," +
            " meaningCorrect = :meaningCorrect," +
            " meaningIncorrect = :meaningIncorrect," +
            " meaningCurrentStreak = :meaningCurrentStreak," +
            " meaningMaxStreak = :meaningMaxStreak," +
            " readingCorrect = :readingCorrect," +
            " readingIncorrect = :readingIncorrect," +
            " readingCurrentStreak = :readingCurrentStreak," +
            " readingMaxStreak = :readingMaxStreak," +
            " percentageCorrect = :percentageCorrect," +
            " leechScore = :leechScore," +
            " statisticPatched = 0" +
            " WHERE id = :subjectId"
    )
    protected abstract fun tryUpdateHelperReviewStatistic(
        subjectId: Long,
        reviewStatisticId: Long,
        meaningCorrect: Int,
        meaningIncorrect: Int,
        meaningCurrentStreak: Int,
        meaningMaxStreak: Int,
        readingCorrect: Int,
        readingIncorrect: Int,
        readingCurrentStreak: Int,
        readingMaxStreak: Int,
        percentageCorrect: Int,
        leechScore: Int
    ): Int

    /**
     * Try to update a subject record from an API review statistic instance.
     *
     * @param apiReviewStatistic the API review statistic to pull data from
     * @return true if there was a record to update
     */
    private fun tryUpdateReviewStatistic(apiReviewStatistic: ApiReviewStatistic): Boolean {
        var meaningStreak = apiReviewStatistic.meaningCurrentStreak.toDouble().pow(1.5)
        if (meaningStreak == 0.0) {
            meaningStreak = 0.5
        }
        var readingStreak = apiReviewStatistic.readingCurrentStreak.toDouble().pow(1.5)
        if (readingStreak == 0.0) {
            readingStreak = 0.5
        }
        val meaningScore = apiReviewStatistic.meaningIncorrect / meaningStreak
        val readingScore = apiReviewStatistic.readingIncorrect / readingStreak
        val leechScore = (1000 * max(meaningScore, readingScore)).toInt()
        val count = tryUpdateHelperReviewStatistic(
            apiReviewStatistic.subjectId,
            apiReviewStatistic.id,
            apiReviewStatistic.meaningCorrect,
            apiReviewStatistic.meaningIncorrect,
            apiReviewStatistic.meaningCurrentStreak,
            apiReviewStatistic.meaningMaxStreak,
            apiReviewStatistic.readingCorrect,
            apiReviewStatistic.readingIncorrect,
            apiReviewStatistic.readingCurrentStreak,
            apiReviewStatistic.readingMaxStreak,
            apiReviewStatistic.percentageCorrect,
            leechScore
        )
        return count > 0
    }

    /**
     * Insert or update an API review statistic depending on whether it exists in the database already.
     *
     * @param apiReviewStatistic the API review statistic
     */
    fun insertOrUpdateReviewStatistic(apiReviewStatistic: ApiReviewStatistic) {
        val updated = tryUpdateReviewStatistic(apiReviewStatistic)
        if (!updated) {
            tryInsertIdOnly(apiReviewStatistic.subjectId)
            tryUpdateReviewStatistic(apiReviewStatistic)
        }
        SubjectChangeWatcher.getInstance().reportChange(apiReviewStatistic.subjectId)
    }

    /**
     * Room-generated method: locally patch the assignment data for a record.
     *
     * @param subjectId the subject ID
     * @param srsStageId assignment field
     * @param unlockedAt assignment field
     * @param startedAt assignment field
     * @param availableAt assignment field
     * @param passedAt assignment field
     * @param burnedAt assignment field
     * @param resurrectedAt assignment field
     */
    @Query(
        "UPDATE subject SET" +
            " srsStage = :srsStageId," +
            " unlockedAt = :unlockedAt," +
            " startedAt = :startedAt," +
            " availableAt = :availableAt," +
            " passedAt = :passedAt," +
            " burnedAt = :burnedAt," +
            " resurrectedAt = :resurrectedAt," +
            " assignmentPatched = 1" +
            " WHERE id = :subjectId"
    )
    protected abstract fun patchAssignmentHelper(
        subjectId: Long,
        srsStageId: Long,
        unlockedAt: Long,
        startedAt: Long,
        availableAt: Long,
        passedAt: Long,
        burnedAt: Long,
        resurrectedAt: Long
    )

    /**
     * Locally patch the assignment data for a record.
     *
     * @param subjectId the subject ID
     * @param srsStageId assignment field
     * @param unlockedAt assignment field
     * @param startedAt assignment field
     * @param availableAt assignment field
     * @param passedAt assignment field
     * @param burnedAt assignment field
     * @param resurrectedAt assignment field
     */
    fun patchAssignment(
        subjectId: Long,
        srsStageId: Long,
        unlockedAt: Long,
        startedAt: Long,
        availableAt: Long,
        passedAt: Long,
        burnedAt: Long,
        resurrectedAt: Long
    ) {
        LOGGER.info(
            "Patch assignment: id:%d stage:%d unlockedAt:%s startedAt:%s availableAt:%s passedAt:%s burnedAt:%s resurrectedAt:%s",
            subjectId, srsStageId, unlockedAt, startedAt, availableAt, passedAt, burnedAt, resurrectedAt
        )
        patchAssignmentHelper(
            subjectId,
            if (unlockedAt == 0L) -999 else srsStageId,
            unlockedAt,
            startedAt,
            availableAt,
            passedAt,
            burnedAt,
            resurrectedAt
        )
        SubjectChangeWatcher.getInstance().reportChange(subjectId)
    }

    /**
     * Room-generated method: locally patch the review statistic data for a record.
     *
     * @param subjectId the subject ID
     * @param meaningCorrect review statistic field
     * @param meaningIncorrect review statistic field
     * @param meaningCurrentStreak review statistic field
     * @param meaningMaxStreak review statistic field
     * @param readingCorrect review statistic field
     * @param readingIncorrect review statistic field
     * @param readingCurrentStreak review statistic field
     * @param readingMaxStreak review statistic field
     * @param percentageCorrect review statistic field
     * @param leechScore review statistic field
     */
    @Query(
        "UPDATE subject SET" +
            " meaningCorrect = :meaningCorrect," +
            " meaningIncorrect = :meaningIncorrect," +
            " meaningCurrentStreak = :meaningCurrentStreak," +
            " meaningMaxStreak = :meaningMaxStreak," +
            " readingCorrect = :readingCorrect," +
            " readingIncorrect = :readingIncorrect," +
            " readingCurrentStreak = :readingCurrentStreak," +
            " readingMaxStreak = :readingMaxStreak," +
            " percentageCorrect = :percentageCorrect," +
            " leechScore = :leechScore," +
            " statisticPatched = 1" +
            " WHERE id = :subjectId"
    )
    protected abstract fun patchReviewStatisticHelper(
        subjectId: Long,
        meaningCorrect: Int,
        meaningIncorrect: Int,
        meaningCurrentStreak: Int,
        meaningMaxStreak: Int,
        readingCorrect: Int,
        readingIncorrect: Int,
        readingCurrentStreak: Int,
        readingMaxStreak: Int,
        percentageCorrect: Int,
        leechScore: Int
    )

    /**
     * Locally patch the review statistic data for a record.
     *
     * @param subjectId the subject ID
     * @param meaningCorrect review statistic field
     * @param meaningIncorrect review statistic field
     * @param meaningCurrentStreak review statistic field
     * @param meaningMaxStreak review statistic field
     * @param readingCorrect review statistic field
     * @param readingIncorrect review statistic field
     * @param readingCurrentStreak review statistic field
     * @param readingMaxStreak review statistic field
     * @param percentageCorrect review statistic field
     */
    fun patchReviewStatistic(
        subjectId: Long,
        meaningCorrect: Int,
        meaningIncorrect: Int,
        meaningCurrentStreak: Int,
        meaningMaxStreak: Int,
        readingCorrect: Int,
        readingIncorrect: Int,
        readingCurrentStreak: Int,
        readingMaxStreak: Int,
        percentageCorrect: Int
    ) {
        var meaningStreak = meaningCurrentStreak.toDouble().pow(1.5)
        if (meaningStreak == 0.0) {
            meaningStreak = 0.5
        }
        var readingStreak = readingCurrentStreak.toDouble().pow(1.5)
        if (readingStreak == 0.0) {
            readingStreak = 0.5
        }
        val meaningScore = meaningIncorrect / meaningStreak
        val readingScore = readingIncorrect / readingStreak
        val leechScore = (1000 * max(meaningScore, readingScore)).toInt()
        patchReviewStatisticHelper(
            subjectId,
            meaningCorrect,
            meaningIncorrect,
            meaningCurrentStreak,
            meaningMaxStreak,
            readingCorrect,
            readingIncorrect,
            readingCurrentStreak,
            readingMaxStreak,
            percentageCorrect,
            leechScore
        )
        SubjectChangeWatcher.getInstance().reportChange(subjectId)
    }

    /**
     * Forcibly patch a subject to have an available lesson right now, if it doesn't have one.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param id the subject ID
     * @param unlockedAt the date to simulate for the unlock of this subject, if not set
     * @param maxLevel the max level granted by the user's subscription
     */
    fun forceLessonAvailable(id: Long, unlockedAt: Long, maxLevel: Int) {
        val subject = getById(id)
        if (subject == null || unlockedAt == 0L || subject.level > maxLevel) {
            return
        }

        var changed = false
        if (subject.getUnlockedAt() == 0L) {
            subject.setUnlockedAt(unlockedAt)
            changed = true
        }
        if (subject.getStartedAt() != 0L) {
            subject.setStartedAt(0)
            changed = true
        }
        var stage = subject.getSrsStage()
        if (!stage.isInitial) {
            stage = stage.system.initialStage
            subject.setSrsStage(stage)
            changed = true
        }
        if (changed) {
            patchAssignment(
                id, stage.id, subject.getUnlockedAt(), subject.getStartedAt(),
                subject.getAvailableAt(), subject.getPassedAt(), subject.getBurnedAt(), subject.getResurrectedAt()
            )
        }
    }

    /**
     * Room-generated method: get a list of all subjects available for lesson.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @return the list
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND level <= :maxLevel" +
            " AND (resurrectedAt != 0 OR burnedAt = 0)" +
            " AND unlockedAt != 0 AND startedAt = 0" +
            " ORDER BY level, lessonPosition, id"
    )
    protected abstract fun getAvailableLessonItemsHelper(maxLevel: Int): List<SubjectEntity>

    /**
     * For selected subjects, forcibly patch them so no lesson is available for them.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param maxLevel the max level granted by the user's subscription
     * @param subjectIds the subject IDs to remove from the lesson pool
     */
    fun forceLessonUnavailableExcept(maxLevel: Int, subjectIds: Collection<Long>) {
        for (subject in getAvailableLessonItemsHelper(maxLevel)) {
            if (!subjectIds.contains(subject.id)) {
                patchAssignment(
                    subject.id, subject.srsStageId, subject.unlockedAt, subject.unlockedAt,
                    subject.availableAt, subject.passedAt, subject.burnedAt, subject.resurrectedAt
                )
            }
        }
    }

    /**
     * Forcibly patch a subject to have an available review at the specified timestamp, if it doesn't have one.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param id the subject ID
     * @param availableAt the date to force for the availability of the review
     * @param maxLevel the max level granted by the user's subscription
     */
    fun forceReviewAvailable(id: Long, availableAt: Long, maxLevel: Int) {
        val subject = getById(id)
        if (subject == null || availableAt == 0L || subject.level > maxLevel) {
            return
        }

        var changed = false
        if (subject.getAvailableAt() == 0L || subject.getAvailableAt() > availableAt) {
            subject.setAvailableAt(availableAt)
            changed = true
        }
        var stage = subject.getSrsStage()
        if (subject.getUnlockedAt() == 0L) {
            subject.setUnlockedAt(availableAt)
            stage = stage.system.firstStartedStage
            subject.setSrsStage(stage)
            changed = true
        }
        if (subject.getStartedAt() == 0L) {
            subject.setStartedAt(availableAt)
            changed = true
        }
        if (stage.isCompleted) {
            stage = stage.system.firstStartedStage
            subject.setSrsStage(stage)
            changed = true
        }
        if (changed) {
            patchAssignment(
                id, stage.id, subject.getUnlockedAt(), subject.getStartedAt(),
                subject.getAvailableAt(), subject.getPassedAt(), subject.getBurnedAt(), subject.getResurrectedAt()
            )
        }
    }

    /**
     * Room-generated method: get a list of all subjects available for review, where the review
     * becomes/became available before the given cutoff date.
     *
     * @param maxLevel the maximum level available on the user's subscription
     * @param cutoff the cutoff date
     * @return the list
     */
    @Query(
        "SELECT * FROM subject" +
            " WHERE hiddenAt = 0 AND object IS NOT NULL" +
            " AND level <= :maxLevel" +
            " AND availableAt != 0 AND availableAt < :cutoff"
    )
    protected abstract fun getPendingReviewItemsHelper(maxLevel: Int, cutoff: Long): List<SubjectEntity>

    /**
     * For selected subjects, forcibly patch them so no review is available for them in the next hour.
     * This is part of fixing up sync problems from the summary API endpoint.
     *
     * @param maxLevel the max level granted by the user's subscription
     * @param subjectIds the subject IDs to remove from the review pool
     */
    fun forceUpcomingReviewUnavailableExcept(maxLevel: Int, subjectIds: Collection<Long>) {
        val cutoff = System.currentTimeMillis() + Constants.HOUR
        for (subject in getPendingReviewItemsHelper(maxLevel, cutoff)) {
            if (!subjectIds.contains(subject.id)) {
                patchAssignment(
                    subject.id, subject.srsStageId, subject.unlockedAt, subject.startedAt,
                    0, subject.passedAt, subject.burnedAt, subject.resurrectedAt
                )
            }
        }
    }

    companion object {
        private val LOGGER = Logger.get(SubjectSyncDao::class.java)

        /**
         * Helper method: serialize a generic collection to a JSON array expression. This is to more
         * efficiently store complex fields in the database: each complex field becomes just
         * a string and is de-serialized on demand.
         *
         * @param value the collection to serialize
         * @return the resulting String
         */
        private fun serializeToJsonString(value: Collection<*>?): String {
            if (value == null || value.isEmpty()) {
                return "[]"
            }
            try {
                return Converters.getObjectMapper().writeValueAsString(value)
            } catch (e: JsonProcessingException) {
                throw IllegalArgumentException(e)
            }
        }
    }
}
