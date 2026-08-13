package com.smouldering_durtles.wk.test.db;

import android.content.ContentValues;
import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.Subject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for the subject-facing DAOs, locking in current query behaviour before
 * {@code db/} is ported to Kotlin + KSP (#55). Companion to {@link DaoCharacterizationTest}.
 *
 * <p>Rows are inserted with raw SQL rather than through the entity. The {@code subject} table has 61
 * columns and its entity is the 138-method god-object that {@code AUDIT.md} §7 marks for
 * decomposition, so building instances through setters would make these tests hostage to that
 * refactor. Raw inserts pin what the <i>queries</i> do, which is exactly what the port must
 * preserve, and keep passing when the entity is eventually split.</p>
 *
 * <p>Written against the current blocking signatures, per #55's signature freeze.</p>
 */
@RunWith(AndroidJUnit4.class)
public class SubjectDaoCharacterizationTest {
    private AppDatabase db;

    @Before
    public void createDb() {
        final Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDb() {
        db.close();
    }

    /** Filler for {@code lastIncorrectAnswer}: distinct, and low enough that an update to a larger
     *  value still passes the monotonic guard on {@code updateLastIncorrectAnswer}. */
    private static final long LAST_INCORRECT_ANSWER_FILLER = 100L;

    /**
     * Distinct filler values for the NOT NULL columns.
     *
     * <p>Deliberately <b>not</b> all zero. If every column holds 0, a column/value misalignment —
     * the exact hazard in {@code SubjectSyncDao}'s long positional INSERTs, and the thing #64 has to
     * avoid when it removes columns — reads back as {@code 0 == 0} and the test passes anyway.
     * Distinct values make a shift show up as a wrong value.</p>
     *
     * <p>Numbering starts at 11, above the small numbers the tests themselves use for levels, star
     * ratings and lesson positions, so a filler value leaking into an assertion is obvious rather
     * than plausible.</p>
     *
     * <p>The three {@code *Patched} flags are pinned to 0 on purpose: {@code SubjectViewsDao} queries
     * them as bare truth tests ({@code WHERE assignmentPatched}), so any nonzero filler would make
     * every row look patched and those tests would assert nothing.</p>
     */
    private static final Map<String, Integer> FILLER = buildFiller();

    private static Map<String, Integer> buildFiller() {
        final Map<String, Integer> filler = new LinkedHashMap<>();
        int next = 11;
        for (final String column : Arrays.asList(
                "numStars", "lessonPosition", "srsSystemId", "level",
                "assignmentId", "srsStage",
                "studyMaterialId", "reviewStatisticId",
                "meaningCorrect", "meaningIncorrect", "meaningMaxStreak", "meaningCurrentStreak",
                "readingCorrect", "readingIncorrect", "readingMaxStreak", "readingCurrentStreak",
                "percentageCorrect", "leechScore", "frequency", "joyoGrade", "jlptLevel")) {
            filler.put(column, next++);
        }
        filler.put("assignmentPatched", 0);
        filler.put("studyMaterialPatched", 0);
        filler.put("statisticPatched", 0);
        return filler;
    }

    /**
     * Insert a subject row directly, filling every NOT NULL column with its distinct {@link #FILLER}
     * value and letting the caller override whatever the query under test actually looks at.
     */
    private void insertSubject(final long id, final ContentValues overrides) {
        final ContentValues values = new ContentValues();
        values.put("id", id);
        for (final Map.Entry<String, Integer> entry : FILLER.entrySet()) {
            values.put(entry.getKey(), entry.getValue());
        }
        // Not part of the filler above because the queries filter on them, so they need meaningful
        // values rather than arbitrary distinct ones: hiddenAt = 0 means visible, and
        // lastIncorrectAnswer carries its own filler so the guarded update has something to compare.
        values.put("object", "kanji");
        values.put("hiddenAt", 0L);
        values.put("lastIncorrectAnswer", LAST_INCORRECT_ANSWER_FILLER);
        // #69 made these NOT NULL, so they have to be written rather than left out. They get 0, not
        // a distinct filler: 0 is what the entity means by "not set", and several queries read them
        // as exactly that sentinel — getLevelReachedDate filters on "unlockedAt != 0" and
        // getLevelProgressLockedItems on "unlockedAt = 0". A distinct nonzero value here would
        // quietly change what those queries see. Tests that care override them.
        for (final String timestamp : Arrays.asList(
                "availableAt", "burnedAt", "passedAt", "resurrectedAt", "startedAt", "unlockedAt")) {
            values.put(timestamp, 0L);
        }
        values.putAll(overrides);
        db.getOpenHelper().getWritableDatabase().insert("subject", 0, values);
    }

    /** Every NOT NULL column still holds its filler, i.e. nothing shifted during the write. */
    private void assertFillerIntact(final long id, final String... exceptColumns) {
        final List<String> excluded = Arrays.asList(exceptColumns);
        for (final Map.Entry<String, Integer> entry : FILLER.entrySet()) {
            if (excluded.contains(entry.getKey())) {
                continue;
            }
            assertEquals(
                    "column " + entry.getKey() + " does not hold its filler value",
                    entry.getValue().longValue(),
                    readLongColumn(id, entry.getKey()));
        }
    }

    /**
     * Read one column back with raw SQL. Needed because {@code Subject} does not expose every column
     * it is loaded from — {@code lastIncorrectAnswer} is a public field on {@code SubjectEntity} with
     * no accessor on the wrapper — and because asserting on the column is the sharper test for an
     * {@code UPDATE} query anyway.
     */
    private long readLongColumn(final long id, final String column) {
        try (android.database.Cursor cursor = db.getOpenHelper().getReadableDatabase().query(
                "SELECT " + column + " FROM subject WHERE id = " + id)) {
            assertTrue("no subject row with id " + id, cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static ContentValues values(final Object... keyValuePairs) {
        final ContentValues values = new ContentValues();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            final String key = (String) keyValuePairs[i];
            final Object value = keyValuePairs[i + 1];
            if (value instanceof String) {
                values.put(key, (String) value);
            }
            else if (value instanceof Long) {
                values.put(key, (Long) value);
            }
            else {
                values.put(key, (Integer) value);
            }
        }
        return values;
    }

    // ---------------------------------------------------------------------- SubjectDao

    @Test
    public void getByIdReturnsTheRowAndNullForAMissingOne() {
        insertSubject(1L, values("level", 3));

        final Subject found = db.subjectDao().getById(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertNull(db.subjectDao().getById(999L));
    }

    @Test
    public void searchByCharactersMatchesOnlyVisibleKanji() {
        insertSubject(1L, values("object", "kanji", "characters", "水", "hiddenAt", 0L));
        // Hidden, so excluded despite matching characters.
        insertSubject(2L, values("object", "kanji", "characters", "水", "hiddenAt", 12345L));
        // Right characters, wrong type.
        insertSubject(3L, values("object", "vocabulary", "characters", "水", "hiddenAt", 0L));

        final Subject found = db.subjectDao().getKanjiByCharacters("水");

        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    public void updateLastIncorrectAnswerTouchesOnlyTheTargetRow() {
        insertSubject(1L, new ContentValues());
        insertSubject(2L, new ContentValues());

        db.subjectDao().updateLastIncorrectAnswer(1L, 555L);

        assertEquals(555L, readLongColumn(1L, "lastIncorrectAnswer"));
        assertEquals(LAST_INCORRECT_ANSWER_FILLER, readLongColumn(2L, "lastIncorrectAnswer"));
        // A targeted UPDATE must not disturb any other column on the row it does touch.
        assertFillerIntact(1L);
    }

    @Test
    public void updateLastIncorrectAnswerNeverMovesTheTimestampBackwards() {
        // The query guards with "AND lastIncorrectAnswer < :lastIncorrectAnswer", so it only ever
        // advances. Easy to drop when transliterating the query string, and the loss would be
        // invisible until a stale answer overwrote a newer one.
        insertSubject(1L, values("lastIncorrectAnswer", 1000L));

        db.subjectDao().updateLastIncorrectAnswer(1L, 500L);

        assertEquals(1000L, readLongColumn(1L, "lastIncorrectAnswer"));
    }

    @Test
    public void handWrittenPositionalInsertLandsInTheColumnsItNames() {
        // This test used to assert that the guarded update was a no-op against a NULL
        // lastIncorrectAnswer, because "NULL < 555" is NULL rather than true. #69 made that column
        // NOT NULL, so the case can no longer arise and the update now simply advances the value.
        // What is kept is the other half of what this test was doing, which is still worth having:
        // a hand-written positional INSERT whose alignment is checked at both ends.
        // Positional INSERT, so the values ascend consecutively with no zeros: if the column list and
        // the VALUES list ever drift out of step, the readbacks below land on the wrong column and
        // say so, instead of matching 0 against 0. Unlike the ContentValues helper above, the
        // *Patched flags need no special casing here — this test runs no patched-view query, so
        // they are free to carry distinct values like everything else. Same for hiddenAt: it is 0
        // ("visible") in the ContentValues helper because most queries filter on it, but nothing
        // here does, so it too carries a distinct value rather than being a blind spot.
        db.getOpenHelper().getWritableDatabase().execSQL(
                "INSERT INTO subject (id, object, hiddenAt, lastIncorrectAnswer, numStars,"
                        + " lessonPosition, srsSystemId, level, assignmentId,"
                        + " srsStage, assignmentPatched,"
                        + " studyMaterialId, studyMaterialPatched, reviewStatisticId, meaningCorrect,"
                        + " meaningIncorrect, meaningMaxStreak, meaningCurrentStreak, readingCorrect,"
                        + " readingIncorrect, readingMaxStreak, readingCurrentStreak,"
                        + " percentageCorrect, leechScore, statisticPatched, frequency, joyoGrade,"
                        + " jlptLevel,"
                        // NOT NULL since #69, so this row has to supply them. Distinct like the
                        // rest, since nothing in this test reads them as the 0 "not set" sentinel.
                        + " availableAt, burnedAt, passedAt, resurrectedAt, startedAt, unlockedAt)"
                        + " VALUES (77, 'kanji', 19, 20, 21, 22, 23, 24, 25, 26, 27,"
                        + " 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,"
                        + " 45, 46, 47, 48, 49, 50)");

        db.subjectDao().updateLastIncorrectAnswer(77L, 555L);

        // 20 < 555, so the guard passes and the timestamp advances.
        assertEquals(555L, readLongColumn(77L, "lastIncorrectAnswer"));
        // Check the alignment of the row this test just hand-wrote: both ends, and the three flag
        // columns that a one-place shift would most easily hide in.
        assertEquals(19L, readLongColumn(77L, "hiddenAt"));
        assertEquals(21L, readLongColumn(77L, "numStars"));
        assertEquals(27L, readLongColumn(77L, "assignmentPatched"));
        assertEquals(28L, readLongColumn(77L, "studyMaterialId"));
        assertEquals(29L, readLongColumn(77L, "studyMaterialPatched"));
        assertEquals(41L, readLongColumn(77L, "statisticPatched"));
        assertEquals(44L, readLongColumn(77L, "jlptLevel"));
        assertEquals(45L, readLongColumn(77L, "availableAt"));
        assertEquals(50L, readLongColumn(77L, "unlockedAt"));
    }

    @Test
    public void deleteAllEmptiesTheSubjectTable() {
        insertSubject(1L, new ContentValues());
        insertSubject(2L, new ContentValues());

        db.subjectDao().deleteAll();

        assertNull(db.subjectDao().getById(1L));
        assertNull(db.subjectDao().getById(2L));
    }

    // ----------------------------------------------------------------- SubjectViewsDao

    @Test
    public void patchedViewsSelectOnlyRowsWithTheCorrespondingFlagSet() {
        insertSubject(1L, values("assignmentPatched", 1));
        insertSubject(2L, values("statisticPatched", 1));
        insertSubject(3L, values("studyMaterialPatched", 1));
        insertSubject(4L, new ContentValues());

        assertEquals(Arrays.asList(1L), db.subjectViewsDao().getPatchedAssignments());
        assertEquals(Arrays.asList(2L), db.subjectViewsDao().getPatchedReviewStatistics());
        assertEquals(Arrays.asList(3L), db.subjectViewsDao().getPatchedStudyMaterials());
    }

    @Test
    public void resolvingPatchedAssignmentsClearsTheFlagForTheGivenIdsOnly() {
        // The query is "WHERE assignmentPatched AND id IN (:ids)" — a boolean column used as a bare
        // truth test alongside an IN clause. Both halves must survive the port.
        insertSubject(1L, values("assignmentPatched", 1));
        insertSubject(2L, values("assignmentPatched", 1));

        db.subjectDao().resolvePatchedAssignments(Arrays.asList(1L));

        final List<Long> stillPatched = db.subjectViewsDao().getPatchedAssignments();
        assertEquals(Arrays.asList(2L), stillPatched);
    }

    @Test
    public void resolvingPatchedFlagsIsScopedToItsOwnColumn() {
        insertSubject(1L, values("assignmentPatched", 1, "statisticPatched", 1));

        db.subjectDao().resolvePatchedAssignments(Arrays.asList(1L));

        assertTrue(db.subjectViewsDao().getPatchedAssignments().isEmpty());
        assertEquals(Arrays.asList(1L), db.subjectViewsDao().getPatchedReviewStatistics());
    }

    // ------------------------------------------------------------ SubjectAggregatesDao

    @Test
    public void maxLevelIgnoresHiddenRowsAndRowsWithNoObject() {
        insertSubject(1L, values("level", 5, "hiddenAt", 0L));
        insertSubject(2L, values("level", 42, "hiddenAt", 999L));

        assertEquals(5, db.subjectAggregatesDao().getMaxLevel());
    }

    // ----------------------------------------------------------- SubjectCollectionsDao

    @Test
    public void byLevelRangeIsInclusiveAtBothEndsAndExcludesHiddenRows() {
        insertSubject(1L, values("level", 2));
        insertSubject(2L, values("level", 3));
        insertSubject(3L, values("level", 4));
        insertSubject(4L, values("level", 5));
        insertSubject(5L, values("level", 3, "hiddenAt", 999L));

        final List<Subject> found = db.subjectCollectionsDao().getByLevelRange(3, 4);

        assertEquals(2, found.size());
        assertEquals(2L, found.get(0).getId());
        assertEquals(3L, found.get(1).getId());
    }

    @Test
    public void byLevelRangeOrdersByLevelThenLessonPositionThenId() {
        insertSubject(10L, values("level", 4, "lessonPosition", 1));
        insertSubject(11L, values("level", 3, "lessonPosition", 9));
        insertSubject(12L, values("level", 3, "lessonPosition", 2));
        insertSubject(13L, values("level", 3, "lessonPosition", 2));

        final List<Subject> found = db.subjectCollectionsDao().getByLevelRange(3, 4);

        // level 3 before 4; within level 3, lessonPosition 2 before 9; the lessonPosition tie breaks
        // on id.
        assertEquals(Arrays.asList(12L, 13L, 11L, 10L), ids(found));
    }

    @Test
    public void byIdsReturnsOnlyTheRequestedSubjects() {
        insertSubject(1L, new ContentValues());
        insertSubject(2L, new ContentValues());
        insertSubject(3L, new ContentValues());

        final List<Subject> found = db.subjectCollectionsDao().getByIds(Arrays.asList(1L, 3L));

        assertEquals(Arrays.asList(1L, 3L), ids(found));
    }

    @Test
    public void starRatingsRoundTripThroughTheirOwnColumn() {
        // updateStars writes "SET numStars = :numStars" and getStarredSubjectIds reads
        // "WHERE numStars = :numStars". Before #64 both said typeCode, a column retired as the
        // subject type code and quietly reused for star ratings; the rename gave stars a column
        // whose name matches its contents. SearchUtil builds the same filter as a raw SQL string,
        // so it cannot be checked here — see the star-search step in the ticket's device checks.
        insertSubject(1L, new ContentValues());
        insertSubject(2L, new ContentValues());

        db.subjectDao().updateStars(1L, 3);

        assertEquals(Arrays.asList(1L), db.subjectCollectionsDao().getStarredSubjectIds(3));
        assertTrue(db.subjectCollectionsDao().getStarredSubjectIds(5).isEmpty());
        assertEquals(3L, readLongColumn(1L, "numStars"));
        assertEquals(FILLER.get("numStars").longValue(), readLongColumn(2L, "numStars"));
        // Setting a star rating must not spill into any neighbouring column.
        assertFillerIntact(1L, "numStars");
    }

    private static List<Long> ids(final List<Subject> subjects) {
        final java.util.List<Long> result = new java.util.ArrayList<>();
        for (final Subject subject : subjects) {
            result.add(subject.getId());
        }
        return result;
    }

    @Test
    public void levelReachedDateIsTheEarliestUnlockForThatLevelIgnoringUnlockedAtZero() {
        insertSubject(1L, values("level", 7, "unlockedAt", 3000L));
        insertSubject(2L, values("level", 7, "unlockedAt", 1000L));
        // unlockedAt = 0 means "not unlocked" and is excluded rather than treated as the minimum.
        insertSubject(3L, values("level", 7, "unlockedAt", 0L));
        insertSubject(4L, values("level", 8, "unlockedAt", 500L));

        assertEquals(1000L, db.subjectAggregatesDao().getLevelReachedDate(7));
    }
}
