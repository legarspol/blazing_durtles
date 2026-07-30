package com.smouldering_durtles.wk.test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.dao.TaskDefinitionDao;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.services.ApiTaskService;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Covers what happens to a persisted task whose stored type key no longer maps to a task type,
 * which is what an app upgrade that removes a task class leaves behind.
 */
@RunWith(AndroidJUnit4.class)
public class ApiTaskDropTest {
    /** A key that is deliberately absent from ApiTaskType. */
    private static final String GONE_KEY = "com.smouldering_durtles.wk.tasks.GoneTask";

    /**
     * The task has to be removed - the drain loop is driven by the total row count, so leaving it
     * in place would spin forever - and it has to be reported on the way out. The timeout is part
     * of the assertion: if the row survives, this test hangs rather than fails.
     */
    @Test(timeout = 60_000)
    public void unresolvableTaskIsDroppedAndReported() {
        final AppDatabase db = WkApplication.getDatabase();
        final TaskDefinitionDao dao = db.taskDefinitionDao();

        // Start from an empty queue so the drain loop only has our row to deal with, and doesn't
        // spend the test making real API calls.
        dao.deleteAll();

        final TaskDefinition taskDefinition = new TaskDefinition();
        taskDefinition.setTaskClass(GONE_KEY);
        taskDefinition.setPriority(0);
        taskDefinition.setData("{\"test\": true}");
        dao.insertTaskDefinition(taskDefinition);
        assertEquals(1, dao.getCountByType(GONE_KEY));

        final int logSizeBefore = db.logRecordDao().getTotalSize();

        ApiTaskService.runTasks();

        assertEquals(0, dao.getCountByType(GONE_KEY));
        // The whole point of the ticket: the row is gone, but it did not go quietly.
        assertTrue("dropping the task should have been logged",
                db.logRecordDao().getTotalSize() > logSizeBefore);
    }
}
