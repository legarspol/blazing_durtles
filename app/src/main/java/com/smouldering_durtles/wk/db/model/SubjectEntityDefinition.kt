package com.smouldering_durtles.wk.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smouldering_durtles.wk.enums.SubjectType

/**
 * Room entity for the subject table. See LogRecordEntityDefinition for an explanation of why this class exists.
 *
 * Every nullable property here maps to a nullable column in the v68 schema. A non-null Kotlin type
 * would silently make the column `NOT NULL` and change the schema's identityHash, which existing
 * installs verify on every open.
 */
@Suppress("unused")
@Entity(tableName = "subject")
class SubjectEntityDefinition {
    @PrimaryKey var id: Long = 0L

    @ColumnInfo(name = "object")
    var type: SubjectType? = null

    var numStars: Int = 0
    var hiddenAt: Long? = null
    var lessonPosition: Int = 0
    var srsSystemId: Long = 0L

    @ColumnInfo(index = true) var level: Int = 0

    @ColumnInfo(index = true) var characters: String? = null
    var slug: String? = null
    var documentUrl: String? = null
    var meanings: String? = null
    var meaningMnemonic: String? = null
    var meaningHint: String? = null
    var auxiliaryMeanings: String? = null
    var readings: String? = null
    var readingMnemonic: String? = null
    var readingHint: String? = null
    var componentSubjectIds: String? = null
    var amalgamationSubjectIds: String? = null
    var visuallySimilarSubjectIds: String? = null
    var partsOfSpeech: String? = null
    var contextSentences: String? = null
    var pronunciationAudios: String? = null

    var searchTarget: String? = null
    var smallSearchTarget: String? = null
    var assignmentId: Long = 0L

    @ColumnInfo(index = true) var availableAt: Long? = null

    @ColumnInfo(index = true) var burnedAt: Long? = null
    var passedAt: Long? = null
    var resurrectedAt: Long? = null

    @ColumnInfo(index = true) var startedAt: Long? = null

    @ColumnInfo(index = true) var unlockedAt: Long? = null

    @ColumnInfo(index = true, name = "srsStage") var srsStageId: Long = 0L
    var lastIncorrectAnswer: Long? = null
    var assignmentPatched: Boolean = false
    var studyMaterialId: Long = 0L
    var meaningNote: String? = null
    var meaningSynonyms: String? = null
    var readingNote: String? = null
    var studyMaterialPatched: Boolean = false
    var reviewStatisticId: Long = 0L
    var meaningCorrect: Int = 0
    var meaningIncorrect: Int = 0
    var meaningMaxStreak: Int = 0
    var meaningCurrentStreak: Int = 0
    var readingCorrect: Int = 0
    var readingIncorrect: Int = 0
    var readingMaxStreak: Int = 0
    var readingCurrentStreak: Int = 0
    var percentageCorrect: Int = 0
    var leechScore: Int = 0
    var statisticPatched: Boolean = false
    var frequency: Int = 0
    var joyoGrade: Int = 0
    var jlptLevel: Int = 0
    var pitchInfo: String? = null
    var strokeData: String? = null
}
