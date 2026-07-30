package com.smouldering_durtles.wk.test.db;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.dao.PropertiesDao;
import com.smouldering_durtles.wk.db.dao.SessionItemDao;
import com.smouldering_durtles.wk.db.dao.TaskDefinitionDao;
import com.smouldering_durtles.wk.db.model.SessionItem;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.enums.SessionItemState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for the Room DAOs, locking in current query behaviour before {@code db/}
 * is ported to Kotlin + KSP (#55).
 *
 * <p>Written against the current <b>blocking</b> DAO signatures on purpose. #55 freezes those
 * signatures precisely so this file compiles and passes unchanged after the port — if it needs
 * editing then, a signature moved and the port went out of scope. The {@code suspend}/{@code Flow}
 * variants arrive separately in #56.</p>
 *
 * <p>These tests complement rather than duplicate the exported schema hash. The hash proves the
 * database's <i>shape</i>; these prove the queries still select, order and filter the same rows,
 * and that values survive the type converters when they pass through real SQLite.</p>
 */
@RunWith(AndroidJUnit4.class)
public class DaoCharacterizationTest {
    private static final String AUDIO_TASK = "com.smouldering_durtles.wk.tasks.DownloadAudioTask";
    private static final String API_TASK = "com.smouldering_durtles.wk.tasks.GetSummaryTask";

    private AppDatabase db;

    @Before
    public void createDb() {
        final Context context = ApplicationProvider.getApplicationContext();
        // In-memory, bypassing AppDatabase.getInstance() so the test never touches the real
        // database file or the WkApplication singleton.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDb() {
        db.close();
    }

    // ------------------------------------------------------------------ SessionItemDao

    private static SessionItem sessionItem(final long id, final int order, final SessionItemState state) {
        final SessionItem item = new SessionItem();
        item.setId(id);
        item.setAssignmentId(id * 10);
        item.setOrder(order);
        item.setState(state);
        return item;
    }

    @Test
    public void sessionItemRoundTripsThroughTheDatabase() {
        final SessionItemDao dao = db.sessionItemDao();

        dao.insert(sessionItem(1L, 0, SessionItemState.PENDING));

        final SessionItem loaded = dao.getById(1L);
        assertNotNull(loaded);
        assertEquals(1L, loaded.getId());
        assertEquals(10L, loaded.getAssignmentId());
        assertEquals(SessionItemState.PENDING, loaded.getState());
    }

    @Test
    public void everySessionItemStateSurvivesTheConverterAndSqlite() {
        final SessionItemDao dao = db.sessionItemDao();

        long id = 1L;
        for (final SessionItemState state : SessionItemState.values()) {
            dao.insert(sessionItem(id, 0, state));
            final SessionItem loaded = dao.getById(id);
            assertNotNull(loaded);
            assertEquals("state " + state + " did not survive the round trip", state, loaded.getState());
            id++;
        }
    }

    @Test
    public void getAllOrdersByTheOrderColumnNotByInsertionOrInsertedId() {
        final SessionItemDao dao = db.sessionItemDao();

        dao.insert(sessionItem(1L, 30, SessionItemState.ACTIVE));
        dao.insert(sessionItem(2L, 10, SessionItemState.ACTIVE));
        dao.insert(sessionItem(3L, 20, SessionItemState.ACTIVE));

        final List<SessionItem> all = dao.getAll();

        assertEquals(3, all.size());
        assertEquals(2L, all.get(0).getId());
        assertEquals(3L, all.get(1).getId());
        assertEquals(1L, all.get(2).getId());
    }

    @Test
    public void legacyStartedStateStoredByAnOlderVersionReadsBackAsActive() {
        // The state column is a bare TEXT column, so a database written by an older app version can
        // legitimately contain "STARTED"/"NEW". Converters coerces those to ACTIVE; this asserts the
        // coercion holds when the value comes out of real SQLite rather than from a direct call.
        db.getOpenHelper().getWritableDatabase().execSQL(
                "INSERT INTO session_item (id, assignmentId, state, srsSystemId, srsStage, level,"
                        + " typeCode, bucket, `order`, meaningDone, meaningIncorrect, readingDone,"
                        + " readingIncorrect, onyomiDone, onyomiIncorrect, kunyomiDone,"
                        + " kunyomiIncorrect, numAnswers, lastAnswer)"
                        + " VALUES (99, 990, 'STARTED', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)");

        final SessionItem loaded = db.sessionItemDao().getById(99L);

        assertNotNull(loaded);
        assertEquals(SessionItemState.ACTIVE, loaded.getState());
    }

    @Test
    public void updatePersistsAChangedState() {
        final SessionItemDao dao = db.sessionItemDao();
        dao.insert(sessionItem(1L, 0, SessionItemState.ACTIVE));

        final SessionItem loaded = dao.getById(1L);
        assertNotNull(loaded);
        loaded.setState(SessionItemState.REPORTED);
        dao.update(loaded);

        final SessionItem reloaded = dao.getById(1L);
        assertNotNull(reloaded);
        assertEquals(SessionItemState.REPORTED, reloaded.getState());
    }

    @Test
    public void deleteAllEmptiesTheTableAndGetByIdReturnsNullForAMissingRow() {
        final SessionItemDao dao = db.sessionItemDao();
        dao.insert(sessionItem(1L, 0, SessionItemState.ACTIVE));

        dao.deleteAll();

        assertTrue(dao.getAll().isEmpty());
        assertNull(dao.getById(1L));
    }

    // --------------------------------------------------------------- TaskDefinitionDao

    private static TaskDefinition taskDefinition(final String taskClass, final int priority) {
        final TaskDefinition definition = new TaskDefinition();
        definition.setTaskClass(taskClass);
        definition.setPriority(priority);
        definition.setData("{}");
        return definition;
    }

    @Test
    public void taskDefinitionCountsDistinguishApiTasksFromAudioDownloads() {
        final TaskDefinitionDao dao = db.taskDefinitionDao();

        dao.insertTaskDefinition(taskDefinition(API_TASK, 0));
        dao.insertTaskDefinition(taskDefinition(AUDIO_TASK, 0));
        dao.insertTaskDefinition(taskDefinition(AUDIO_TASK, 0));

        assertEquals(3, dao.getCount());
        assertEquals(1, dao.getApiCount());
        assertEquals(2, dao.getCountByType(AUDIO_TASK));
        assertEquals(1, dao.getCountByType(API_TASK));
    }

    @Test
    public void nextTaskDefinitionIsOrderedByPriorityThenId() {
        final TaskDefinitionDao dao = db.taskDefinitionDao();

        dao.insertTaskDefinition(taskDefinition(API_TASK, 5));
        dao.insertTaskDefinition(taskDefinition(AUDIO_TASK, 1));
        dao.insertTaskDefinition(taskDefinition(API_TASK, 1));

        final TaskDefinition next = dao.getNextTaskDefinition();

        assertNotNull(next);
        // Priority 1 wins over 5; between the two priority-1 rows the lower autogenerated id wins,
        // which is the audio task inserted second.
        assertEquals(AUDIO_TASK, next.getTaskClass());
        assertEquals(1, next.getPriority());
    }

    @Test
    public void deleteAudioDownloadsRemovesOnlyAudioTasks() {
        final TaskDefinitionDao dao = db.taskDefinitionDao();
        dao.insertTaskDefinition(taskDefinition(API_TASK, 0));
        dao.insertTaskDefinition(taskDefinition(AUDIO_TASK, 0));

        dao.deleteAudioDownloads();

        assertEquals(1, dao.getCount());
        assertEquals(0, dao.getCountByType(AUDIO_TASK));
        assertEquals(1, dao.getCountByType(API_TASK));
    }

    @Test
    public void deletingATaskDefinitionRemovesThatRow() {
        final TaskDefinitionDao dao = db.taskDefinitionDao();
        dao.insertTaskDefinition(taskDefinition(API_TASK, 0));

        final TaskDefinition stored = dao.getNextTaskDefinition();
        assertNotNull(stored);
        dao.deleteTaskDefinition(stored);

        assertEquals(0, dao.getCount());
        assertNull(dao.getNextTaskDefinition());
    }

    @Test
    public void nextTaskDefinitionIsNullWhenTheQueueIsEmpty() {
        assertNull(db.taskDefinitionDao().getNextTaskDefinition());
        assertEquals(0, db.taskDefinitionDao().getCount());
    }

    // ------------------------------------------------------------------- PropertiesDao

    @Test
    public void absentPropertiesReadAsTheirZeroValues() {
        final PropertiesDao dao = db.propertiesDao();

        assertFalse(dao.isApiKeyRejected());
        assertFalse(dao.isApiInError());
        assertEquals(0L, dao.getLastApiSuccessDate());
        assertEquals(0L, dao.getLastUserSyncSuccessDate());
    }

    @Test
    public void propertySettersAreReadableImmediatelyThroughTheCache() {
        // setProperty writes the in-memory cache synchronously and the database asynchronously via
        // runAsync, so read-your-writes works without waiting. A port must keep that property; a
        // naive "await the DB write" rewrite would change the timing every caller relies on.
        final PropertiesDao dao = db.propertiesDao();

        dao.setApiKeyRejected(true);
        dao.setLastApiSuccessDate(1234L);

        assertTrue(dao.isApiKeyRejected());
        assertEquals(1234L, dao.getLastApiSuccessDate());
    }

    @Test
    public void booleanPropertiesRoundTripBothWays() {
        final PropertiesDao dao = db.propertiesDao();

        dao.setApiInError(true);
        assertTrue(dao.isApiInError());

        dao.setApiInError(false);
        assertFalse(dao.isApiInError());
    }

    @Test
    public void deltaBearingLongGettersSubtractTheDeltaWhenTheStoredValueIsAtLeastTheDelta() {
        final PropertiesDao dao = db.propertiesDao();

        dao.setLastSubjectSyncSuccessDate(1000L);

        assertEquals(900L, dao.getLastSubjectSyncSuccessDate(100L));
        assertEquals(1000L, dao.getLastSubjectSyncSuccessDate(0L));
        // Stored value below the delta is returned untouched rather than going negative.
        assertEquals(1000L, dao.getLastSubjectSyncSuccessDate(5000L));
    }

    @Test
    public void deletingAPropertyReturnsItToItsDefault() {
        final PropertiesDao dao = db.propertiesDao();
        dao.setApiKeyRejected(true);
        assertTrue(dao.isApiKeyRejected());

        dao.deleteProperty("api_key_rejected");

        assertFalse(dao.isApiKeyRejected());
    }
}
