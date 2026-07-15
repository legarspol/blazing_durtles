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

package com.smouldering_durtles.wk.util;

import android.os.Environment;

import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import com.smouldering_durtles.wk.BuildConfig;
import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.api.model.PronunciationAudio;
import com.smouldering_durtles.wk.model.GenderedFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.AUDIO_DIRECTORY_NAME;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;

/**
 * Storage-location, path-derivation, file-existence, temp-file and bulk-delete
 * logic for pronunciation audio files.
 */
public final class AudioStorage {
    /**
     * Get the array of external directories available to the app (scoped storage).
     *
     * @return the array of absolute path names, never null, could be empty
     */
    private File[] getExternalFilesDirs() {
        return WkApplication.getInstance().getExternalFilesDirs(null);
    }

    /**
     * Resolve the base directory for a storage location value. "Internal" resolves to
     * the app's no-backup files directory, falling back to the regular files directory.
     * Any other value is treated as an absolute filesystem path.
     *
     * @param location the location value, as returned by getLocationValues()
     * @return the base directory, or null if it could not be determined
     */
    private @Nullable File resolveBaseDirectory(final String location) {
        if (location.equals("Internal")) {
            @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
            if (dir == null) {
                dir = WkApplication.getInstance().getFilesDir();
            }
            return dir;
        }
        return new File(location);
    }

    /**
     * Get a list of possible audio download locations. The first entry is always "Internal",
     * the rest are filesystem paths for the various available external storage locations.
     *
     * @return the list
     */
    public List<String> getLocationValues() {
        final List<String> result = new ArrayList<>();

        for (final File file: getExternalFilesDirs()) {
            try {
                result.add(file.getCanonicalPath());
            }
            catch (final Exception e) {
                //
            }
        }

        final String currentLocation = GlobalSettings.Api.getAudioLocation();
        if (!isEmpty(currentLocation) && !result.contains(currentLocation)) {
            result.add(currentLocation);
        }

        result.remove("Internal");
        Collections.sort(result);
        result.add(0, "Internal");

        return result;
    }

    /**
     * Get a list of human-readable audio download locations. Based on the list retrieved
     * from getLocationValues(), these strings have the package-specific part removed for
     * display.
     *
     * @param locationValues the result of getLocationValues()
     * @return the list
     */
    public List<String> getLocations(final List<String> locationValues) {
        final List<String> result = new ArrayList<>(locationValues);

        for (int i=0; i<result.size(); i++) {
            String location = result.get(i);
            final int p = location.indexOf("/" + BuildConfig.APPLICATION_ID);
            if (p > 0) {
                location = location.substring(0, p);
            }
            result.set(i, location);
        }

        return result;
    }

    /**
     * Get the File instance corresponding to an audio file for the specified audio record.
     * If no such file exists, return null. If the return value is not null, that file is
     * guaranteed to exist, but there are no guarantees it will be readable.
     *
     * @param level the subject's level
     * @param audio the audio record
     * @param locationValues the available storage locations
     * @return the file if it exists
     */
    public @Nullable GenderedFile getExistingFileForAudio(final int level, final PronunciationAudio audio,
                                                            final Iterable<String> locationValues) {
        for (final String location: locationValues) {
            final @Nullable File baseDirectory = resolveBaseDirectory(location);
            if (baseDirectory == null) {
                continue;
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            final File levelDir = new File(audioDir, Integer.toString(level));
            final GenderedFile mp3File = new GenderedFile(levelDir, String.format(Locale.ROOT, "%d.mp3", audio.getMetadata().getSourceId()),
                    audio.getMetadata().isMale());
            if (mp3File.exists()) {
                return mp3File;
            }
            final GenderedFile oggFile = new GenderedFile(levelDir, String.format(Locale.ROOT, "%d.ogg", audio.getMetadata().getSourceId()),
                    audio.getMetadata().isMale());
            if (oggFile.exists()) {
                return oggFile;
            }
        }
        return null;
    }

    /**
     * Check if we have any audio file for a subject's audio record.
     *
     * @param level the subject's level
     * @param audio the audio record from the subject
     * @param locationValues the available storage locations
     * @return true if any audio file exists
     */
    public boolean hasAudioFileFor(final int level, final PronunciationAudio audio, final Iterable<String> locationValues) {
        return getExistingFileForAudio(level, audio, locationValues) != null;
    }

    /**
     * Get the file where an audio file should be stored if it will be stored on internal storage.
     * The result should never be null, but will be null if anything goes wrong finding this location.
     * If this returns a non-null value, the returned file may or may not exist, but its parent
     * directory is guaranteed to exist.
     *
     * @param level the subject's level
     * @param audio the audio record
     * @return the file or null is something went wrong
     */
    private @Nullable GenderedFile getNewFileForAudioOnInternal(final int level, final PronunciationAudio audio) {
        if (isEmpty(audio.getUrl()) || isEmpty(audio.getContentType()) || audio.getMetadata().getSourceId() <= 0) {
            return null;
        }

        try {
            File baseDirectory = resolveBaseDirectory("Internal");
            if (baseDirectory == null) {
                return null;
            }
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }
            baseDirectory = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }

            final File levelDir = new File(baseDirectory, Integer.toString(level));
            if (!levelDir.exists() && !levelDir.mkdirs()) {
                return null;
            }

            if (!levelDir.canWrite()) {
                return null;
            }

            final String extension = audio.getContentType().equals("audio/ogg") ? ".ogg" : ".mp3";
            final String fileName = audio.getMetadata().getSourceId() + extension;
            return new GenderedFile(levelDir, fileName, audio.getMetadata().isMale());
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Get the file where an audio file should be stored if it will be stored on external storage.
     * The result can be null if the storage isn't mounted or is mounted read-only, or if anything
     * goes wrong finding this location. If this returns a non-null value, the returned file may
     * or may not exist, but its parent directory is guaranteed to exist.
     *
     * @param location the location to store under
     * @param level the subject's level
     * @param audio the audio record
     * @return the file or null is something went wrong
     */
    private @Nullable GenderedFile getNewFileForAudioOnExternal(final String location, final int level, final PronunciationAudio audio) {
        if (isEmpty(audio.getUrl()) || isEmpty(audio.getContentType()) || audio.getMetadata().getSourceId() <= 0) {
            return null;
        }

        try {
            File baseDirectory = resolveBaseDirectory(location);
            if (baseDirectory == null) {
                return null;
            }
            final @Nullable String status = EnvironmentCompat.getStorageState(baseDirectory);
            if (!Environment.MEDIA_MOUNTED.equals(status)) {
                return null;
            }
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }
            baseDirectory = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            if (!baseDirectory.exists() && !baseDirectory.mkdirs()) {
                return null;
            }

            final File levelDir = new File(baseDirectory, Integer.toString(level));
            if (!levelDir.exists() && !levelDir.mkdirs()) {
                return null;
            }

            if (!levelDir.canWrite()) {
                return null;
            }

            final String extension = audio.getContentType().equals("audio/ogg") ? ".ogg" : ".mp3";
            final String fileName = audio.getMetadata().getSourceId() + extension;
            return new GenderedFile(levelDir, fileName, audio.getMetadata().isMale());
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Get the file where an audio file should be stored, taking into account user storage preferences.
     * The result should never be null, but will be null if anything goes wrong finding this location.
     * If this returns a non-null value, the returned file may or may not exist, but its parent
     * directory is guaranteed to exist. If the user wants audio to be stored on external storage,
     * but that storage is not available or writable, the result file will point to internal storage
     * instead.
     *
     * @param level the subject's level
     * @param audio the audio record
     * @return the file or null is something went wrong
     */
    public @Nullable GenderedFile getNewFileForAudio(final int level, final PronunciationAudio audio) {
        final String location = GlobalSettings.Api.getAudioLocation();
        if (location.equals("Internal")) {
            return getNewFileForAudioOnInternal(level, audio);
        }
        final @Nullable GenderedFile externalFile = getNewFileForAudioOnExternal(location, level, audio);
        if (externalFile != null) {
            return externalFile;
        }
        return getNewFileForAudioOnInternal(level, audio);
    }

    /**
     * Get the temporary file for downloading a new audio file.
     *
     * @param targetFile the eventual target file this temp file is for
     * @return the file, which may or may not exist
     */
    public @Nullable File getTempFile(final File targetFile) {
        try {
            final @Nullable File dir = targetFile.getParentFile();
            if (dir == null) {
                return null;
            }
            return new File(dir, "downloading.tmp");
        }
        catch (final Exception e) {
            return null;
        }
    }

    /**
     * Delete a directory and all of its contents.
     *
     * @param directory the directory to delete
     */
    private void deleteDirectory(final File directory) {
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] files = directory.listFiles();
            if (files != null) {
                for (final File file : files) {
                    deleteDirectory(file);
                }
            }
            //noinspection ResultOfMethodCallIgnored
            directory.delete();
        }
        catch (final Exception e) {
            //
        }
    }

    /**
     * Delete all audio files.
     */
    public void deleteAllAudio() {
        final @Nullable File baseDirectory = resolveBaseDirectory("Internal");
        deleteDirectory(new File(baseDirectory, AUDIO_DIRECTORY_NAME));

        for (final String location: getLocationValues()) {
            if (location.equals("Internal")) {
                continue;
            }
            final @Nullable File dir = resolveBaseDirectory(location);
            deleteDirectory(new File(dir, AUDIO_DIRECTORY_NAME));
        }
    }
}
