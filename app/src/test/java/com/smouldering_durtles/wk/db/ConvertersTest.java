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

package com.smouldering_durtles.wk.db;

import com.smouldering_durtles.wk.tasks.ApiTask;
import com.smouldering_durtles.wk.tasks.GetUserTask;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Characterization tests for {@link Converters#stringToTaskClass(String)}: it must keep resolving
 * valid class names and returning null for a null input, but it must no longer swallow an
 * unresolvable class name into a null return.
 */
public final class ConvertersTest {
    @Test
    public void nullInputReturnsNull() {
        assertNull(Converters.stringToTaskClass(null));
    }

    @Test
    public void resolvableClassNameReturnsTheClass() {
        final Class<? extends ApiTask> clas = Converters.stringToTaskClass(GetUserTask.class.getName());
        assertEquals(GetUserTask.class, clas);
    }

    @Test
    public void unresolvableClassNameThrowsWithClassNotFoundCause() {
        try {
            Converters.stringToTaskClass("com.smouldering_durtles.wk.tasks.NoSuchTaskEver");
            fail("expected a RuntimeException wrapping ClassNotFoundException");
        } catch (final RuntimeException e) {
            assertEquals(ClassNotFoundException.class, e.getCause().getClass());
        }
    }
}
