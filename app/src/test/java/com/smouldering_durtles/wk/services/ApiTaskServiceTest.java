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

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import androidx.lifecycle.LiveData;

import com.smouldering_durtles.wk.db.dao.TaskDefinitionDao;
import com.smouldering_durtles.wk.db.model.TaskDefinition;
import com.smouldering_durtles.wk.model.TaskCounts;
import com.smouldering_durtles.wk.tasks.ApiTask;

import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression tests for {@link ApiTaskService#drainApiTasks}: an unresolvable {@code taskClass}
 * (Room throwing while materialising the row, per {@link com.smouldering_durtles.wk.db.Converters})
 * must be logged, delete the offending row by id, and let the queue drain instead of looping
 * forever or being silently swallowed like every other failure.
 */
public final class ApiTaskServiceTest {
    @Test
    public void unresolvableTaskClassIsDeletedByIdAndTheQueueDrains() throws Exception {
        final FakeTaskDefinitionDao dao = new FakeTaskDefinitionDao();
        dao.nextTaskDefinitionFailsWithUnresolvableClass = true;
        dao.count = 1;
        final FakeCursor cursor = new FakeCursor(42, "com.smouldering_durtles.wk.tasks.NoSuchTaskEver");
        final int[] reportedId = {-1};
        final String[] reportedClassName = {null};
        final RuntimeException[] reportedCause = {null};

        ApiTaskService.drainApiTasks(dao, () -> cursor, (id, taskClassName, cause) -> {
            reportedId[0] = id;
            reportedClassName[0] = taskClassName;
            reportedCause[0] = cause;
        });

        assertEquals(0, dao.count);
        assertTrue("the offending row should have been deleted", dao.deleted);
        assertEquals(42, dao.deletedId);
        assertTrue("the raw lookup cursor should have been closed", cursor.closed);
        assertEquals("the offending row must be reported before it is deleted", 42, reportedId[0]);
        assertEquals("com.smouldering_durtles.wk.tasks.NoSuchTaskEver", reportedClassName[0]);
        assertEquals(ClassNotFoundException.class, reportedCause[0].getCause().getClass());
    }

    @Test
    public void otherRuntimeExceptionsFromGetNextTaskDefinitionAreNotSwallowed() {
        final FakeTaskDefinitionDao dao = new FakeTaskDefinitionDao();
        dao.nextTaskDefinitionFailsWithOtherError = true;
        dao.count = 1;

        try {
            ApiTaskService.drainApiTasks(dao,
                    () -> {
                        throw new AssertionError("the raw lookup must not run for non-ClassNotFoundException failures");
                    },
                    (id, taskClassName, cause) -> {
                        throw new AssertionError("the reporter must not run for non-ClassNotFoundException failures");
                    });
            fail("expected the original RuntimeException to propagate");
        } catch (final RuntimeException e) {
            assertEquals("boom", e.getMessage());
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
        assertFalse("the row must not be deleted when the failure is unrelated", dao.deleted);
    }

    /**
     * A hand-rolled fake: {@link TaskDefinitionDao} is a Room {@code @Dao} abstract class, and the
     * module has neither Robolectric nor Mockito, so a real {@code TaskDefinition} row is
     * simulated directly rather than going through Room/SQLite.
     */
    private static final class FakeTaskDefinitionDao extends TaskDefinitionDao {
        private int count;
        private boolean nextTaskDefinitionFailsWithUnresolvableClass;
        private boolean nextTaskDefinitionFailsWithOtherError;
        private boolean deleted;
        private int deletedId = -1;

        @Override
        public void deleteAll() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void deleteAudioDownloads() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getApiCount() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public LiveData<TaskCounts> getLiveCounts() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public @Nullable TaskDefinition getNextTaskDefinition() {
            if (nextTaskDefinitionFailsWithUnresolvableClass) {
                throw new RuntimeException(new ClassNotFoundException("com.smouldering_durtles.wk.tasks.NoSuchTaskEver"));
            }
            if (nextTaskDefinitionFailsWithOtherError) {
                throw new RuntimeException("boom");
            }
            return null;
        }

        @Override
        public int getCountByType(final Class<? extends ApiTask> taskClass) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void insertTaskDefinition(final TaskDefinition taskDefinition) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void deleteTaskDefinition(final TaskDefinition taskDefinition) {
            deleted = true;
            deletedId = taskDefinition.getId();
            count = 0;
        }
    }

    /**
     * A minimal, single-row {@link Cursor} fake standing in for the raw
     * {@code SELECT id, taskClass FROM task_definition ...} lookup. There is no Robolectric on
     * this module's test classpath, so the real {@code android.jar} stub methods throw if called;
     * only the methods {@code ApiTaskService} actually uses are given real behavior.
     */
    private static final class FakeCursor implements Cursor {
        private final int id;
        private final String taskClass;
        private boolean closed;

        FakeCursor(final int id, final String taskClass) {
            this.id = id;
            this.taskClass = taskClass;
        }

        @Override
        public boolean moveToFirst() {
            return true;
        }

        @Override
        public int getInt(final int columnIndex) {
            return columnIndex == 0 ? id : 0;
        }

        @Override
        public String getString(final int columnIndex) {
            return columnIndex == 1 ? taskClass : null;
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public int getCount() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int getPosition() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean move(final int offset) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean moveToPosition(final int position) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean moveToLast() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean moveToNext() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean moveToPrevious() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean isFirst() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean isLast() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean isBeforeFirst() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean isAfterLast() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int getColumnIndex(final String columnName) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int getColumnIndexOrThrow(final String columnName) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public String getColumnName(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public String[] getColumnNames() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int getColumnCount() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public byte[] getBlob(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void copyStringToBuffer(final int columnIndex, final CharArrayBuffer buffer) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public short getShort(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public long getLong(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public float getFloat(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public double getDouble(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public int getType(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean isNull(final int columnIndex) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void deactivate() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean requery() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void registerContentObserver(final ContentObserver observer) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void unregisterContentObserver(final ContentObserver observer) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void registerDataSetObserver(final DataSetObserver observer) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void unregisterDataSetObserver(final DataSetObserver observer) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void setNotificationUri(final ContentResolver cr, final Uri uri) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Uri getNotificationUri() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public boolean getWantsAllOnMoveCalls() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public void setExtras(final Bundle extras) {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Bundle getExtras() {
            throw new UnsupportedOperationException("not used by this test");
        }

        @Override
        public Bundle respond(final Bundle extras) {
            throw new UnsupportedOperationException("not used by this test");
        }
    }
}
