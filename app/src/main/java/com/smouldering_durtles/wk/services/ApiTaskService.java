/*
 * Copyright 2019-2020 Ernst Jan Plugge <rmc@dds.nl>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smouldering_durtles.wk.services;

import android.content.Intent;
import android.database.Cursor;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.dao.TaskDefinitionDao;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.jobs.TickJob;
import com.smouldering_durtles.wk.livedata.LiveFirstTimeSetup;
import com.smouldering_durtles.wk.model.Session;
import com.smouldering_durtles.wk.services.JobIntentService;
import com.smouldering_durtles.wk.tasks.ApiTask;
import com.smouldering_durtles.wk.util.Logger;

import java.util.Collection;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.StableIds.API_TASK_SERVICE_JOB_ID;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;

/**
 * An intent service for running tasks. Tasks are actions that need to run
 * in the background, don't have to run immediately, may take a long time to
 * complete (usually because they are network calls), and must be persisted
 * so they will be executed even across restarts and when errors occur.
 *
 * <p>
 *     Tasks are recorded in the database. This service will loop over them
 *     one by one in priority order, taking into account the current online
 *     status.
 * </p>
 */
public final class ApiTaskService extends JobIntentService {
    /**
     * A single dummy object to synchronize on, to make sure the background sync doesn't
     * overlap with this.
     */
    private static final Object TASK_MONITOR = new Object();

    private static final Logger LOGGER = Logger.get(ApiTaskService.class);

    /**
     * Schedule a run of the service to be executed on a background thread.
     * This is regularly called from job housekeeping.
     */
    public static void schedule() {
        final Intent intent = new Intent(WkApplication.getInstance(), ApiTaskService.class);
        enqueueWork(WkApplication.getInstance(), ApiTaskService.class, API_TASK_SERVICE_JOB_ID, intent);
    }

    private static void runTasksImpl() throws Exception {
        final AppDatabase db = WkApplication.getDatabase();
        drainApiTasks(db.taskDefinitionDao(),
                () -> db.query("SELECT id, taskClass FROM task_definition ORDER BY priority, id LIMIT 1", null),
                (id, taskClassName, cause) -> {
                    // TODO(#11): recordException
                    LOGGER.error(cause, "Deleting task definition id=%d with unresolvable task class %s", id, taskClassName);
                });
        if (db.taskDefinitionDao().getApiCount() == 0) {
            if (GlobalSettings.getFirstTimeSetup() == 0) {
                GlobalSettings.setFirstTimeSetup(1);
                LiveFirstTimeSetup.getInstance().forceUpdate();
            }
            if (Session.getInstance().isInactive()) {
                final Collection<Long> assignmentSubjectIds = db.subjectViewsDao().getPatchedAssignments();
                if (!assignmentSubjectIds.isEmpty()) {
                    db.assertGetPatchedAssignmentsTask(assignmentSubjectIds);
                }
                final Collection<Long> reviewStatisticsSubjectIds = db.subjectViewsDao().getPatchedReviewStatistics();
                if (!reviewStatisticsSubjectIds.isEmpty()) {
                    db.assertGetPatchedReviewStatisticsTask(reviewStatisticsSubjectIds);
                }
                final Collection<Long> studyMaterialsSubjectIds = db.subjectViewsDao().getPatchedStudyMaterials();
                if (!studyMaterialsSubjectIds.isEmpty()) {
                    db.assertGetPatchedStudyMaterialsTask(studyMaterialsSubjectIds);
                }
                if (db.propertiesDao().getForceLateRefresh()) {
                    db.propertiesDao().setForceLateRefresh(false);
                    db.assertRefreshForAllModels();
                    db.assertGetLevelProgressionTask();
                    JobRunnerService.schedule(TickJob.class, "");
                }
            }
        }
    }

    /**
     * Reports a task definition whose {@code taskClass} column names a class that no longer exists,
     * before it is deleted. Kept as a seam (rather than calling {@link Logger} directly) so the
     * drain/terminate behavior below is unit-testable without an Android logging stack.
     */
    @FunctionalInterface
    interface UnresolvableTaskReporter {
        void report(int id, String taskClassName, RuntimeException cause);
    }

    /**
     * Loop through the pending task definitions and execute them one by one, in priority order.
     *
     * @param dao the DAO to fetch and delete task definitions with
     * @param unresolvableTaskLookup a raw lookup for the id/taskClass of the next task definition,
     *                               used when {@code dao.getNextTaskDefinition()} cannot materialise
     *                               its task class; bypasses the {@code taskClass} type converter,
     *                               which is exactly what throws in that case
     * @param unresolvableTaskReporter called with the offending id/class name before that row is deleted
     * @throws Exception if a task fails for a reason other than an unresolvable task class
     */
    static void drainApiTasks(final TaskDefinitionDao dao, final Supplier<Cursor> unresolvableTaskLookup,
                               final UnresolvableTaskReporter unresolvableTaskReporter) throws Exception {
        while (dao.getCount() > 0) {
            //noinspection SynchronizationOnStaticField
            synchronized (TASK_MONITOR) {
                final @Nullable TaskDefinition taskDefinition;
                try {
                    taskDefinition = dao.getNextTaskDefinition();
                } catch (final RuntimeException e) {
                    if (!(e.getCause() instanceof ClassNotFoundException)) {
                        throw e;
                    }
                    deleteUnresolvableTaskDefinition(dao, unresolvableTaskLookup, unresolvableTaskReporter, e);
                    continue;
                }
                if (taskDefinition == null) {
                    break;
                }

                final @Nullable Class<? extends ApiTask> clas = taskDefinition.getTaskClass();
                if (clas == null) {
                    dao.deleteTaskDefinition(taskDefinition);
                }
                else {
                    final ApiTask apiTask = clas.getConstructor(TaskDefinition.class).newInstance(taskDefinition);

                    if (!apiTask.canRun()) {
                        break;
                    }

                    apiTask.run();
                }
            }
        }
    }

    /**
     * Handle a task definition whose {@code taskClass} column names a class that no longer exists.
     * Reports the offending id and class name loudly, then deletes the row by id so the queue can drain.
     *
     * @param dao the DAO to delete the task definition with
     * @param unresolvableTaskLookup a raw lookup for the id/taskClass of the offending row
     * @param unresolvableTaskReporter called with the offending id/class name before that row is deleted
     * @param cause the exception thrown by {@code dao.getNextTaskDefinition()}, wrapping the {@code ClassNotFoundException}
     */
    private static void deleteUnresolvableTaskDefinition(final TaskDefinitionDao dao, final Supplier<Cursor> unresolvableTaskLookup,
                                                           final UnresolvableTaskReporter unresolvableTaskReporter,
                                                           final RuntimeException cause) {
        try (Cursor cursor = unresolvableTaskLookup.get()) {
            if (!cursor.moveToFirst()) {
                return;
            }
            final int id = cursor.getInt(0);
            final String taskClassName = cursor.getString(1);
            unresolvableTaskReporter.report(id, taskClassName, cause);
            final TaskDefinition stub = new TaskDefinition();
            stub.setId(id);
            dao.deleteTaskDefinition(stub);
        }
    }

    /**
     * Loop through all available tasks and execute them one by one, taking into
     * account the priority order and online status.
     *
     * <p>
     *     Each task is response for removing itself from the database when
     *     finished. Until then, the task will be retried indefinitely.
     * </p>
     */
    public static void runTasks() {
        safe(ApiTaskService::runTasksImpl);
    }

    @Override
    protected void onHandleWork(final @Nonnull Intent intent) {
        runTasks();
    }
}
