package com.smouldering_durtles.wk.test.db;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smouldering_durtles.wk.api.model.ApiAssignment;
import com.smouldering_durtles.wk.api.model.ApiReviewStatistic;
import com.smouldering_durtles.wk.api.model.ApiStudyMaterial;
import com.smouldering_durtles.wk.api.model.ApiSubject;
import com.smouldering_durtles.wk.db.AppDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for {@code SubjectSyncDao}'s write paths.
 *
 * <p>Written for #69, which made 17 columns {@code NOT NULL}. Every timestamp that changed lives on
 * {@code subject}, and the statements that write those timestamps are all in this DAO — so this is
 * where a leftover null write would surface, as a {@code SQLiteConstraintException} at sync time.
 * Nothing else covered it: the #54 suites exercise the subject-facing <i>queries</i>, not the sync
 * writes, which is the largest untested surface in {@code db/} and stays that way until #57
 * decomposes this class.</p>
 *
 * <p>The DAO is self-contained — no {@code WkApplication}, no {@code GlobalSettings} — so these run
 * against a plain in-memory database.</p>
 *
 * <p>Two things are being asserted throughout. First, that each path completes at all: a
 * {@code NOT NULL} violation throws rather than corrupting silently, so reaching the assertions is
 * itself the constraint check. Second, that the values land in the columns they claim to — this
 * DAO's two long positional INSERTs list their {@code VALUES} as literals, so a miscount shifts
 * every subsequent value one column across and still compiles and runs.</p>
 */
@RunWith(AndroidJUnit4.class)
public class SubjectSyncDaoCharacterizationTest {
    /**
     * The subject columns #69 flipped from nullable to {@code NOT NULL}. Every write path below is
     * checked against this list rather than against a couple of spot values, so a path that starts
     * writing a null to any of them fails here instead of on a user's device mid-sync.
     */
    private static final List<String> NOT_NULL_TIMESTAMPS = Arrays.asList(
            "hiddenAt", "availableAt", "burnedAt", "passedAt",
            "resurrectedAt", "startedAt", "unlockedAt", "lastIncorrectAnswer");

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

    @Test
    public void insertingANewSubjectPopulatesEveryNotNullTimestamp() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        assertRowExists(1L);
        assertNoNullTimestamps(1L);
    }

    @Test
    public void insertingANewSubjectLandsItsValuesInTheColumnsItNames() {
        // The insert lists ~54 columns against positional literals. Distinct values rather than a
        // single spot check, so a one-column shift shows up as a wrong value somewhere.
        final ApiSubject apiSubject = subject(1L, 7);
        apiSubject.setHiddenAt(4444L);
        apiSubject.setLessonPosition(21);
        apiSubject.setSrsSystemId(22);

        db.subjectSyncDao().insertOrUpdate(apiSubject, Collections.emptyList());

        assertEquals(7L, readLong(1L, "level"));
        assertEquals(4444L, readLong(1L, "hiddenAt"));
        assertEquals(21L, readLong(1L, "lessonPosition"));
        assertEquals(22L, readLong(1L, "srsSystemId"));
        // A brand new subject starts unrated, and the insert hard-codes that 0.
        assertEquals(0L, readLong(1L, "numStars"));
        // The one non-zero literal in the statement, so it moves first if anything shifts.
        assertEquals(-999L, readLong(1L, "srsStage"));
    }

    @Test
    public void updatingAnExistingSubjectKeepsEveryNotNullTimestampPopulated() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        // Second pass with the id known to exist takes the UPDATE branch rather than the INSERT.
        final ApiSubject updated = subject(1L, 9);
        updated.setCharacters("犬");
        db.subjectSyncDao().insertOrUpdate(updated, Collections.singletonList(1L));

        assertEquals(9L, readLong(1L, "level"));
        assertEquals("犬", readString(1L, "characters"));
        assertNoNullTimestamps(1L);
    }

    @Test
    public void assignmentForAnUnknownSubjectIsSilentlyDropped() {
        // Documents a real defect rather than intended behaviour — see the follow-up ticket.
        //
        // With no subject row present, insertOrUpdateAssignment falls back to tryInsertIdOnly. That
        // statement's column list omits level, lessonPosition and numStars, all of which are
        // NOT NULL with no default, so the INSERT always raises SQLITE_CONSTRAINT_NOTNULL — and
        // tryInsertIdOnly catches SQLiteConstraintException and ignores it. The assignment is
        // therefore dropped without a trace.
        //
        // This predates #64 and #69: the column list has never carried those three, so it is
        // inherited from upstream rather than introduced by the Kotlin port. Pinned here so the
        // behaviour is visible, and so that whoever fixes the INSERT sees this test fail and
        // replaces it with the positive assertion it should have been.
        db.subjectSyncDao().insertOrUpdateAssignment(assignment(1L, 55L));

        assertRowAbsent(1L);
    }

    @Test
    public void assignmentTimestampsLandInTheColumnsTheyName() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        final ApiAssignment apiAssignment = assignment(1L, 55L);
        apiAssignment.setUnlockedAt(1001L);
        apiAssignment.setStartedAt(1002L);
        apiAssignment.setAvailableAt(1003L);
        apiAssignment.setPassedAt(1004L);
        apiAssignment.setBurnedAt(1005L);
        apiAssignment.setResurrectedAt(1006L);

        db.subjectSyncDao().insertOrUpdateAssignment(apiAssignment);

        assertEquals(1001L, readLong(1L, "unlockedAt"));
        assertEquals(1002L, readLong(1L, "startedAt"));
        assertEquals(1003L, readLong(1L, "availableAt"));
        assertEquals(1004L, readLong(1L, "passedAt"));
        assertEquals(1005L, readLong(1L, "burnedAt"));
        assertEquals(1006L, readLong(1L, "resurrectedAt"));
        assertEquals(55L, readLong(1L, "srsStage"));
        assertNoNullTimestamps(1L);
    }

    @Test
    public void patchingAnAssignmentPopulatesEveryNotNullTimestamp() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        db.subjectSyncDao().patchAssignment(1L, 4L, 2001L, 2002L, 2003L, 2004L, 2005L, 2006L);

        assertEquals(2001L, readLong(1L, "unlockedAt"));
        assertEquals(2002L, readLong(1L, "startedAt"));
        assertEquals(2003L, readLong(1L, "availableAt"));
        assertEquals(2004L, readLong(1L, "passedAt"));
        assertEquals(2005L, readLong(1L, "burnedAt"));
        assertEquals(2006L, readLong(1L, "resurrectedAt"));
        assertNoNullTimestamps(1L);
    }

    @Test
    public void patchingAnAssignmentWithNoUnlockDateParksTheSrsStage() {
        // patchAssignment substitutes -999 for the stage when unlockedAt is 0, which is how a locked
        // subject is represented. Easy to lose, and it decides whether the subject is presented at
        // all, so it is worth pinning next to the constraint checks.
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        db.subjectSyncDao().patchAssignment(1L, 4L, 0L, 0L, 0L, 0L, 0L, 0L);

        assertEquals(-999L, readLong(1L, "srsStage"));
        assertNoNullTimestamps(1L);
    }

    @Test
    public void reviewStatisticAndStudyMaterialWritesKeepTimestampsPopulated() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        final ApiReviewStatistic statistic = new ApiReviewStatistic();
        statistic.setId(10L);
        statistic.setSubjectId(1L);
        statistic.setMeaningCorrect(11);
        statistic.setMeaningIncorrect(12);
        statistic.setReadingCorrect(13);
        statistic.setPercentageCorrect(14);
        db.subjectSyncDao().insertOrUpdateReviewStatistic(statistic);

        final ApiStudyMaterial material = new ApiStudyMaterial();
        material.setId(20L);
        material.setSubjectId(1L);
        material.setMeaningNote("note");
        material.setMeaningSynonyms(Collections.singletonList("synonym"));
        db.subjectSyncDao().insertOrUpdateStudyMaterial(material, false);

        assertEquals(11L, readLong(1L, "meaningCorrect"));
        assertEquals(12L, readLong(1L, "meaningIncorrect"));
        assertEquals(13L, readLong(1L, "readingCorrect"));
        assertEquals(14L, readLong(1L, "percentageCorrect"));
        assertEquals("note", readString(1L, "meaningNote"));
        assertNoNullTimestamps(1L);
    }

    @Test
    public void patchingAReviewStatisticPopulatesEveryNotNullTimestamp() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());

        db.subjectSyncDao().patchReviewStatistic(1L, 11, 12, 13, 14, 15, 16, 17, 18, 19);

        assertEquals(11L, readLong(1L, "meaningCorrect"));
        assertEquals(19L, readLong(1L, "percentageCorrect"));
        assertNoNullTimestamps(1L);
    }

    @Test
    public void forcingLessonAndReviewAvailabilityKeepsTimestampsPopulated() {
        db.subjectSyncDao().insertOrUpdate(subject(1L, 3), Collections.emptyList());
        db.subjectSyncDao().insertOrUpdateAssignment(assignment(1L, 2L));

        db.subjectSyncDao().forceLessonAvailable(1L, 3001L, 60);
        assertNoNullTimestamps(1L);

        db.subjectSyncDao().forceReviewAvailable(1L, 3002L, 60);
        assertNoNullTimestamps(1L);
    }

    /** An API subject with the fields the insert path actually reads. */
    private static ApiSubject subject(final long id, final int level) {
        final ApiSubject apiSubject = new ApiSubject();
        apiSubject.setId(id);
        apiSubject.setObject("kanji");
        apiSubject.setLevel(level);
        apiSubject.setCharacters("一");
        apiSubject.setSlug("one");
        apiSubject.setDocumentUrl("https://example.invalid/one");
        apiSubject.setMeaningMnemonic("mnemonic");
        return apiSubject;
    }

    private static ApiAssignment assignment(final long subjectId, final long srsStageId) {
        final ApiAssignment apiAssignment = new ApiAssignment();
        apiAssignment.setId(subjectId + 500L);
        apiAssignment.setSubjectId(subjectId);
        apiAssignment.setSrsStageId(srsStageId);
        return apiAssignment;
    }

    /** Fail with the column name rather than a bare "expected true" if any of them came back NULL. */
    private void assertNoNullTimestamps(final long id) {
        final List<String> nulls = new ArrayList<>();
        for (final String column : NOT_NULL_TIMESTAMPS) {
            try (Cursor cursor = db.getOpenHelper().getReadableDatabase().query(
                    "SELECT " + column + " IS NULL FROM subject WHERE id = " + id)) {
                assertTrue("no subject row with id " + id, cursor.moveToFirst());
                if (cursor.getInt(0) == 1) {
                    nulls.add(column);
                }
            }
        }
        assertTrue("NOT NULL columns came back NULL: " + nulls, nulls.isEmpty());
    }

    private void assertRowExists(final long id) {
        assertEquals("expected a subject row with id " + id, 1, countRows(id));
    }

    private void assertRowAbsent(final long id) {
        assertEquals("expected no subject row with id " + id, 0, countRows(id));
    }

    private int countRows(final long id) {
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase().query(
                "SELECT COUNT(*) FROM subject WHERE id = " + id)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private long readLong(final long id, final String column) {
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase().query(
                "SELECT " + column + " FROM subject WHERE id = " + id)) {
            assertTrue("no subject row with id " + id, cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private String readString(final long id, final String column) {
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase().query(
                "SELECT " + column + " FROM subject WHERE id = " + id)) {
            assertTrue("no subject row with id " + id, cursor.moveToFirst());
            return cursor.getString(0);
        }
    }
}
