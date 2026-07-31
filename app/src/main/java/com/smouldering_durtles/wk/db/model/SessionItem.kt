package com.smouldering_durtles.wk.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.smouldering_durtles.wk.GlobalSettings
import com.smouldering_durtles.wk.enums.KanjiAcceptedReadingType
import com.smouldering_durtles.wk.enums.SessionItemState
import com.smouldering_durtles.wk.enums.SessionItemState.ABANDONED
import com.smouldering_durtles.wk.enums.SessionItemState.ACTIVE
import com.smouldering_durtles.wk.enums.SessionItemState.PENDING
import com.smouldering_durtles.wk.enums.SessionItemState.REPORTED
import com.smouldering_durtles.wk.jobs.ReportSessionItemJob
import com.smouldering_durtles.wk.jobs.UpdateSessionItemJob
import com.smouldering_durtles.wk.model.Question
import com.smouldering_durtles.wk.model.Session
import com.smouldering_durtles.wk.model.SrsSystem
import com.smouldering_durtles.wk.model.SrsSystemRepository
import com.smouldering_durtles.wk.model.TypefaceConfiguration
import com.smouldering_durtles.wk.services.JobRunnerService
import com.smouldering_durtles.wk.util.FontStorageUtil
import com.smouldering_durtles.wk.util.Logger
import com.smouldering_durtles.wk.util.ObjectSupport.nextRandomInt
import com.smouldering_durtles.wk.util.TextUtil.hasGlyphs
import com.smouldering_durtles.wk.util.TextUtil.isTildeCapable
import java.util.Locale
import java.util.regex.Pattern

/**
 * Room entity for the session_item table, which persists items in a session.
 *
 * [state] and [kanjiAcceptedReadingType] are nullable because their columns are: Room derives
 * `NOT NULL` from the Kotlin type, and a non-null type here would change the v68 schema. Neither
 * is ever actually null on a row read back from the database — [Converters] coerces a null to
 * `ACTIVE`/`NEITHER` on the way in.
 */
@Entity(tableName = "session_item")
class SessionItem {
    @PrimaryKey var id: Long = 0L
    var assignmentId: Long = 0L
    var state: SessionItemState? = ACTIVE
    var srsSystemId: Long = 0L
    @ColumnInfo(name = "srsStage") var srsStageId: Long = 0L
    var level: Int = 0
    @ColumnInfo(name = "typeCode") var unused: Int = 0
    var bucket: Int = 0
    var order: Int = 0

    @get:JvmName("isQuestion1Done")
    @ColumnInfo(name = "meaningDone") var question1Done: Boolean = false

    @ColumnInfo(name = "meaningIncorrect") var question1Incorrect: Int = 0

    @get:JvmName("isQuestion2Done")
    @ColumnInfo(name = "readingDone") var question2Done: Boolean = false

    @ColumnInfo(name = "readingIncorrect") var question2Incorrect: Int = 0

    @get:JvmName("isQuestion3Done")
    @ColumnInfo(name = "onyomiDone") var question3Done: Boolean = false

    @ColumnInfo(name = "onyomiIncorrect") var question3Incorrect: Int = 0

    @get:JvmName("isQuestion4Done")
    @ColumnInfo(name = "kunyomiDone") var question4Done: Boolean = false

    @ColumnInfo(name = "kunyomiIncorrect") var question4Incorrect: Int = 0
    var numAnswers: Int = 0
    var lastAnswer: Long = 0L
    var kanjiAcceptedReadingType: KanjiAcceptedReadingType? = KanjiAcceptedReadingType.NEITHER

    @Ignore private var typefaceConfiguration: TypefaceConfiguration? = null

    /**
     * Not stored in the database; the subject for this item.
     */
    @Ignore var subject: Subject? = null

    /**
     * The number of questions to wait before this item can come back as a choice.
     */
    @Ignore var choiceDelay: Int = 0

    @Ignore private val questions: MutableCollection<Question> = ArrayList()

    /**
     * Is this item active?.
     *
     * @return true if it is
     */
    fun isActive(): Boolean = state == ACTIVE

    /**
     * Is this item pending to be reported at the end of the session?.
     *
     * @return true if it is
     */
    fun isPending(): Boolean = state == PENDING

    /**
     * Has this item been abandoned?.
     *
     * @return true if it has
     */
    fun isAbandoned(): Boolean = state == ABANDONED

    /**
     * Has this item been reported?.
     *
     * @return true if it has
     */
    fun isReported(): Boolean = state == REPORTED

    /**
     * Has this item been started, i.e. is it active and has it had at least one question answered correctly or incorrectly.
     *
     * @return true if it has
     */
    fun isStarted(): Boolean = isActive() && numAnswers > 0

    /**
     * Have all questions for this item been finished or is the item otherwise inactive?.
     *
     * @return true if they have
     */
    fun isFinished(): Boolean {
        if (state != ACTIVE) {
            return true
        }
        return question1Done && question2Done && question3Done && question4Done
    }

    /**
     * The nth question has been finished for this item.
     * @param slot the slot, 1..4
     * @return the value
     */
    fun isQuestionDone(slot: Int): Boolean = when (slot) {
        1 -> question1Done
        2 -> question2Done
        3 -> question3Done
        else -> question4Done
    }

    /**
     * The nth question has been finished for this item.
     * @param slot the slot, 1..4
     * @param questionDone the value
     */
    fun setQuestionDone(slot: Int, questionDone: Boolean) {
        when (slot) {
            1 -> question1Done = questionDone
            2 -> question2Done = questionDone
            3 -> question3Done = questionDone
            else -> question4Done = questionDone
        }
    }

    /**
     * Number of incorrect nth question answers for this item.
     * @param slot the slot, 1..4
     * @return the value
     */
    fun getQuestionIncorrect(slot: Int): Int = when (slot) {
        1 -> question1Incorrect
        2 -> question2Incorrect
        3 -> question3Incorrect
        else -> question4Incorrect
    }

    /**
     * Number of incorrect nth question answers for this item.
     * @param slot the slot, 1..4
     * @param questionIncorrect the value
     */
    fun setQuestionIncorrect(slot: Int, questionIncorrect: Int) {
        when (slot) {
            1 -> question1Incorrect = questionIncorrect
            2 -> question2Incorrect = questionIncorrect
            3 -> question3Incorrect = questionIncorrect
            else -> question4Incorrect = questionIncorrect
        }
    }

    /**
     * Get the starting SRS stage for this item, i.e. the stage it had before the session started.
     *
     * @return the stage
     */
    fun getSrsStage(): SrsSystem.Stage = SrsSystemRepository.getSrsSystem(srsSystemId).getStage(srsStageId)

    /**
     * Set the starting SRS stage for this item, i.e. the stage it had before the session started.
     *
     * @param srsStage the stage
     */
    fun setSrsStage(srsStage: SrsSystem.Stage) {
        srsSystemId = srsStage.system.id
        srsStageId = srsStage.id
    }

    override fun toString(): String = id.toString()

    /**
     * Is this item 'alive' in the session, meaning it's either active or pending a report.
     *
     * @return true if it is.
     */
    fun isAlive(): Boolean = state == ACTIVE || state == PENDING

    /**
     * Does this item has both meaning and reading-related questions to be answered?.
     *
     * @return true if it does
     */
    fun hasPendingReadingAndMeaning(): Boolean =
        !question1Done && !(question2Done && question3Done && question4Done)

    /**
     * Does this item have any incorrectly answered questions?.
     *
     * @return true if it does
     */
    fun hasIncorrectAnswers(): Boolean =
        question1Incorrect > 0 || question2Incorrect > 0 || question3Incorrect > 0 || question4Incorrect > 0

    /**
     * Assuming this is a review session, what would the new SRS stage for this item be if it were finished now?.
     *
     * @return the new stage
     */
    fun getNewSrsStage(): SrsSystem.Stage =
        getSrsStage().getNewStage(question1Incorrect + question2Incorrect + question3Incorrect + question4Incorrect)

    /**
     * Schedule a job to update this item in the database one last time and then
     * report it to the API (if applicable for this session type).
     */
    fun report() {
        val data = String.format(
            Locale.ROOT, "%d %d %s %d %d %d",
            id,
            assignmentId,
            Session.getInstance().type,
            question1Incorrect,
            question2Incorrect + question3Incorrect + question4Incorrect,
            lastAnswer
        )
        JobRunnerService.schedule(ReportSessionItemJob::class.java, data)
        state = REPORTED
    }

    /**
     * Schedule a job to update this item in the database.
     */
    fun update() {
        val data = String.format(
            Locale.ROOT, "%d %s %s %d %s %d %s %d %s %d %d %d",
            id,
            state,
            question1Done,
            question1Incorrect,
            question2Done,
            question2Incorrect,
            question3Done,
            question3Incorrect,
            question4Done,
            question4Incorrect,
            numAnswers,
            lastAnswer
        )
        JobRunnerService.schedule(UpdateSessionItemJob::class.java, data)
    }

    /**
     * Get the typeface configuration chosen for this item in question text display. Choose one at random
     * from the configured options if none has been chosen yet.
     *
     * @param text The question text to show, used to test compatibility of a typeface with the question text
     * @return a typeface config, which could be the Android default if no other suitable typeface is found,
     *         even if the Android default typeface is not one of the configured options
     */
    fun getTypefaceConfiguration(text: String): TypefaceConfiguration {
        typefaceConfiguration?.let { return it }

        try {
            val typefaceConfigs: MutableList<TypefaceConfiguration> = ArrayList()
            for (name in GlobalSettings.Font.getSelectedFonts()) {
                tryAddTypefaceConfiguration(typefaceConfigs, FontStorageUtil.getTypefaceConfiguration(name), text)
            }

            typefaceConfiguration = when {
                typefaceConfigs.isEmpty() -> TypefaceConfiguration.DEFAULT
                typefaceConfigs.size == 1 -> typefaceConfigs[0]
                else -> typefaceConfigs[nextRandomInt(typefaceConfigs.size)]
            }
        } catch (e: Exception) {
            LOGGER.error(e, "Error fetching a typeface")
            typefaceConfiguration = TypefaceConfiguration.DEFAULT
        }

        return typefaceConfiguration!!
    }

    /**
     * Add a question for this item.
     *
     * @param question the question
     */
    fun addQuestion(question: Question) {
        questions.add(question)
    }

    /**
     * Get a question for this item by its type's name() if it exists.
     *
     * @param questionType the type as a string
     * @return the question
     */
    fun getQuestionByTypeStr(questionType: String?): Question? {
        if (questionType == null) {
            return null
        }
        for (question in questions) {
            if (question.type.name == questionType) {
                return question
            }
        }
        return null
    }

    /**
     * Get a question for this item by its slot if it exists.
     *
     * @param slot the slot the question belongs in
     * @return the question
     */
    fun getQuestionBySlot(slot: Int): Question? {
        for (question in questions) {
            if (question.type.slot == slot) {
                return question
            }
        }
        return null
    }

    companion object {
        private val LOGGER = Logger.get(SessionItem::class.java)
        private val TILDE_PATTERN: Pattern = Pattern.compile("〜")

        /**
         * Test if a typeface config is usable for the given question text. If it is, add it to the supplied collection.
         *
         * @param typefaces the collection to update
         * @param typefaceConfiguration the typeface config to test for suitability
         * @param text the question text to test against
         */
        private fun tryAddTypefaceConfiguration(
            typefaces: MutableCollection<in TypefaceConfiguration>,
            typefaceConfiguration: TypefaceConfiguration?,
            text: String
        ) {
            if (typefaceConfiguration == null) {
                return
            }
            if (hasGlyphs(typefaceConfiguration.typeface, text)) {
                typefaces.add(typefaceConfiguration)
            } else if (text.contains("〜") && !isTildeCapable(typefaceConfiguration.typeface) &&
                hasGlyphs(typefaceConfiguration.typeface, TILDE_PATTERN.matcher(text).replaceAll("~"))
            ) {
                typefaces.add(typefaceConfiguration)
            }
        }
    }
}
