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

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.arch.core.util.Function;
import androidx.core.content.ContextCompat;

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.WkApplication;
import com.smouldering_durtles.wk.api.model.PronunciationAudio;
import com.smouldering_durtles.wk.db.AppDatabase;
import com.smouldering_durtles.wk.db.model.PronunciationAudioOwner;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.db.model.SubjectPronunciationAudio;
import com.smouldering_durtles.wk.enums.VoicePreference;
import com.smouldering_durtles.wk.livedata.LiveAudioDownloadStatus;
import com.smouldering_durtles.wk.model.GenderedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.Constants.AUDIO_DIRECTORY_NAME;
import static com.smouldering_durtles.wk.enums.VoicePreference.ALTERNATE;
import static com.smouldering_durtles.wk.enums.VoicePreference.FEMALE;
import static com.smouldering_durtles.wk.enums.VoicePreference.MALE;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEmpty;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEqual;
import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.ObjectSupport.shuffle;
import static java.util.Objects.requireNonNull;

/**
 * Utility methods related to pronunciation audio files.
 */
public final class AudioUtil {
    private static final Logger LOGGER = Logger.get(AudioUtil.class);

    /**
     * The storage backend for locating, resolving and deleting audio files. AudioUtil's
     * still-internal callers hold this instance until their own tickets extract them.
     */
    private static final AudioStorage AUDIO_STORAGE = new AudioStorage();

    /**
     * An unused reference to the most recent MediaPlayer instance. This is kept around so it
     * doesn't get recycled before it is done playing.
     */
    @SuppressWarnings({"unused", "FieldCanBeLocal", "RedundantSuppression"})
    private static @Nullable MediaPlayer savedMediaPlayer = null;

    private static boolean lastWasMale = false;

    /**
     * The audio focus grant currently held on API 26+, if any.
     */
    private static @Nullable AudioFocusRequest currentFocusRequest = null;

    /**
     * The audio focus grant currently held on API 21-25, if any.
     */
    private static @Nullable AudioManager.OnAudioFocusChangeListener currentFocusListener = null;

    private AudioUtil() {
        //
    }

    /**
     * Get a random existing audio file, taking into account the user's preference.
     * Prefer the primary reading.
     *
     * @param subject the subject to get audio for
     * @return the file, or null if none exists
     */
    private static @Nullable GenderedFile getOneAudioFile(final Subject subject) {
        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final VoicePreference voicePreference = GlobalSettings.Audio.getVoicePreference();
        final boolean malePreferred = voicePreference == MALE || voicePreference == ALTERNATE && !lastWasMale;
        final boolean femalePreferred = voicePreference == FEMALE || voicePreference == ALTERNATE && lastWasMale;

        final int level = subject.getLevel();
        final Iterable<String> locationValues = AUDIO_STORAGE.getLocationValues();

        final Comparator<PronunciationAudio> comparator = (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (AUDIO_STORAGE.hasAudioFileFor(level, o1, locationValues) && !AUDIO_STORAGE.hasAudioFileFor(level, o2, locationValues)) {
                return -1;
            }
            if (AUDIO_STORAGE.hasAudioFileFor(level, o2, locationValues) && !AUDIO_STORAGE.hasAudioFileFor(level, o1, locationValues)) {
                return 1;
            }
            if (subject.isPrimaryReading(o1.getMetadata().getPronunciation())
                    && !subject.isPrimaryReading(o2.getMetadata().getPronunciation())) {
                return -1;
            }
            if (subject.isPrimaryReading(o2.getMetadata().getPronunciation())
                    && !subject.isPrimaryReading(o1.getMetadata().getPronunciation())) {
                return 1;
            }
            if (malePreferred) {
                if (o1.getMetadata().isMale() && !o2.getMetadata().isMale()) {
                    return -1;
                }
                if (o2.getMetadata().isMale() && !o1.getMetadata().isMale()) {
                    return 1;
                }
            }
            if (femalePreferred) {
                if (o1.getMetadata().isFemale() && !o2.getMetadata().isFemale()) {
                    return -1;
                }
                if (o2.getMetadata().isFemale() && !o1.getMetadata().isFemale()) {
                    return 1;
                }
            }
            return 0;
        };

        Collections.sort(shuffled, comparator);

        return AUDIO_STORAGE.getExistingFileForAudio(level, shuffled.get(0), locationValues);
    }

    /**
     * Get a random existing audio file that must match a specific reading,
     * taking into account the user's preference.
     *
     * @param subject the subject to get audio for
     * @param reading the reading to find audio for
     * @return the file, or null if none exists
     */
    public static @Nullable GenderedFile getOneAudioFileMustMatch(final PronunciationAudioOwner subject,
                                                                  final @Nullable String reading) {
        if (reading == null) {
            return null;
        }

        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final VoicePreference voicePreference = GlobalSettings.Audio.getVoicePreference();
        final boolean malePreferred = voicePreference == MALE || voicePreference == ALTERNATE && !lastWasMale;
        final boolean femalePreferred = voicePreference == FEMALE || voicePreference == ALTERNATE && lastWasMale;

        final int level = subject.getLevel();
        final Iterable<String> locationValues = AUDIO_STORAGE.getLocationValues();

        final Comparator<PronunciationAudio> comparator = (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (AUDIO_STORAGE.hasAudioFileFor(level, o1, locationValues) && !AUDIO_STORAGE.hasAudioFileFor(level, o2, locationValues)) {
                return -1;
            }
            if (AUDIO_STORAGE.hasAudioFileFor(level, o2, locationValues) && !AUDIO_STORAGE.hasAudioFileFor(level, o1, locationValues)) {
                return 1;
            }
            if (isEqual(o1.getMetadata().getPronunciation(), reading) && !isEqual(o2.getMetadata().getPronunciation(), reading)) {
                return -1;
            }
            if (isEqual(o2.getMetadata().getPronunciation(), reading) && !isEqual(o1.getMetadata().getPronunciation(), reading)) {
                return 1;
            }
            if (malePreferred) {
                if (o1.getMetadata().isMale() && !o2.getMetadata().isMale()) {
                    return -1;
                }
                if (o2.getMetadata().isMale() && !o1.getMetadata().isMale()) {
                    return 1;
                }
            }
            if (femalePreferred) {
                if (o1.getMetadata().isFemale() && !o2.getMetadata().isFemale()) {
                    return -1;
                }
                if (o2.getMetadata().isFemale() && !o1.getMetadata().isFemale()) {
                    return 1;
                }
            }
            return 0;
        };

        Collections.sort(shuffled, comparator);

        final PronunciationAudio audio = shuffled.get(0);
        if (isEqual(audio.getMetadata().getPronunciation(), reading)) {
            return AUDIO_STORAGE.getExistingFileForAudio(level, audio, locationValues);
        }

        return null;
    }

    /**
     * Get a random existing audio file that should match a specific reading,
     * taking into account the user's preference. If no match is available, return the best
     * possible option.
     *
     * @param subject the subject to get audio for
     * @param reading the reading to find audio for
     * @return the file, or null if none exists
     */
    public static @Nullable GenderedFile getOneAudioFileShouldMatch(final Subject subject, final @Nullable String reading) {
        final @Nullable GenderedFile file = getOneAudioFileMustMatch(subject, reading);
        return file == null ? getOneAudioFile(subject) : file;
    }

    /**
     * Look for audio files for a subject and assign a number to the overall status
     * of the availability of audio for that subject.
     *
     * <p>
     *     Values for audioDownloadStatus:
     * </p>
     *
     * <ul>
     *     <li>0 - No audio available for this subject</li>
     *     <li>1 - Audio is available, but none have been downloaded yet</li>
     *     <li>2 - Audio is available, and some has been downloaded, but not all of it</li>
     *     <li>3 - Audio is available, and all have been downloaded</li>
     * </ul>
     *
     * @param level The level of the subject
     * @param pronunciationAudios List of audio records for the subject
     * @param locationValues the available storage locations
     * @return The audio download status as defined above
     */
    public static int findAudioDownloadStatus(final int level, final Collection<PronunciationAudio> pronunciationAudios,
                                              final Iterable<String> locationValues) {
        if (pronunciationAudios.isEmpty()) {
            return 0;
        }
        int numPresent = 0;
        int numAbsent = 0;
        for (final PronunciationAudio audio: pronunciationAudios) {
            if (AUDIO_STORAGE.hasAudioFileFor(level, audio, locationValues)) {
                numPresent++;
            }
            else {
                numAbsent++;
            }
        }
        if (numPresent == 0) {
            return 1;
        }
        else if (numAbsent == 0) {
            return 3;
        }
        else {
            return 2;
        }
    }

    /**
     * Update the audio download status for a level.
     *
     * @param level the level
     */
    public static void updateDownloadStatus(final int level) {
        final AppDatabase db = WkApplication.getDatabase();
        final Collection<SubjectPronunciationAudio> subjects = db.subjectViewsDao().getAudioByLevel(level);
        final Iterable<String> locationValues = AUDIO_STORAGE.getLocationValues();

        int numNoAudio = 0;
        int numMissingAudio = 0;
        int numPartialAudio = 0;
        int numFullAudio = 0;
        for (final PronunciationAudioOwner subject: subjects) {
            final int status = findAudioDownloadStatus(level, subject.getParsedPronunciationAudios(), locationValues);
            switch (status) {
                case 1:
                    numMissingAudio++;
                    break;
                case 2:
                    numPartialAudio++;
                    break;
                case 3:
                    numFullAudio++;
                    break;
                case 0:
                default:
                    numNoAudio++;
                    break;
            }
        }

        db.audioDownloadStatusDao().insertOrUpdate(level, subjects.size(), numNoAudio, numMissingAudio, numPartialAudio, numFullAudio);
        LiveAudioDownloadStatus.getInstance().update();
    }

    /**
     * Request transient audio focus that allows other apps to duck (not pause). Version-branched:
     * API 26+ uses {@link AudioFocusRequest}, API 21-25 uses the deprecated
     * {@code requestAudioFocus(listener, int, int)}.
     *
     * @param audioManager the audio manager to request focus from
     * @return true if focus was granted
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static boolean requestAudioFocus(final AudioManager audioManager) {
        final AudioManager.OnAudioFocusChangeListener listener = focusChange -> {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                stopCurrentPlayback();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final AudioFocusRequest request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setOnAudioFocusChangeListener(listener)
                    .build();
            if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return false;
            }
            currentFocusRequest = request;
            return true;
        }

        if (audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return false;
        }
        currentFocusListener = listener;
        return true;
    }

    /**
     * Abandon whichever audio focus grant (request or listener, depending on API level) is
     * currently held, if any.
     *
     * @param audioManager the audio manager to abandon focus with
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static void abandonAudioFocus(final AudioManager audioManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (currentFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(currentFocusRequest);
                    currentFocusRequest = null;
                }
            }
            else if (currentFocusListener != null) {
                audioManager.abandonAudioFocus(currentFocusListener);
                currentFocusListener = null;
            }
        }
        catch (final Exception e) {
            //
        }
    }

    /**
     * Stop and release the currently playing MediaPlayer, if any, and abandon its audio focus.
     * Called before starting new playback (so rapid taps don't overlap), and on
     * {@code AUDIOFOCUS_LOSS} (e.g. an incoming call).
     */
    private static void stopCurrentPlayback() {
        final @Nullable MediaPlayer player = savedMediaPlayer;
        savedMediaPlayer = null;
        if (player != null) {
            try {
                player.stop();
            }
            catch (final Exception e) {
                //
            }
            try {
                player.release();
            }
            catch (final Exception e) {
                //
            }
        }
        final @Nullable AudioManager audioManager = ContextCompat.getSystemService(WkApplication.getInstance(), AudioManager.class);
        if (audioManager != null) {
            abandonAudioFocus(audioManager);
        }
    }

    /**
     * Release a MediaPlayer that finished playing or errored out, and abandon its audio focus.
     *
     * @param player the player to release
     * @param audioManager the audio manager to abandon focus with
     */
    private static void releasePlayer(final MediaPlayer player, final AudioManager audioManager) {
        if (savedMediaPlayer == player) {
            savedMediaPlayer = null;
        }
        try {
            player.release();
        }
        catch (final Exception e) {
            //
        }
        abandonAudioFocus(audioManager);
    }

    /**
     * Play an audio file for a subject. Take into account the user's preferences,
     * and try to get a match for the given reading.
     *
     * @param subject the subject
     * @param lastMatchedAnswer the reading to match if possible
     */
    public static void playAudio(final Subject subject, final @Nullable String lastMatchedAnswer) {
        if (WkApplication.getDatabase().propertiesDao().getIsMuted()) {
            return;
        }

        final @Nullable GenderedFile audioFile = getOneAudioFileShouldMatch(subject, lastMatchedAnswer);
        if (audioFile != null) {
            playLocalAudio(audioFile);
        } else {
            // Fallback to streaming
            final @Nullable PronunciationAudio streamingAudio = getStreamingAudio(subject, lastMatchedAnswer);
            if (streamingAudio != null) {
                playStreamingAudio(streamingAudio);
            }
        }
    }

    private static void playLocalAudio(final GenderedFile audioFile) {
        safe(() -> {
            stopCurrentPlayback();

            final @Nullable AudioManager audioManager = ContextCompat.getSystemService(WkApplication.getInstance(), AudioManager.class);
            if (audioManager == null || !requestAudioFocus(audioManager)) {
                return;
            }

            final MediaPlayer player = new MediaPlayer();
            savedMediaPlayer = player;
            lastWasMale = audioFile.isMale();

            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setOnCompletionListener(mp -> releasePlayer(mp, audioManager));
            player.setOnErrorListener((mp, what, extra) -> {
                releasePlayer(mp, audioManager);
                return true;
            });

            try {
                // Use audioFile directly
                player.setDataSource(audioFile.getAbsolutePath());
                player.prepare();
                player.start();
            }
            catch (final Exception e) {
                releasePlayer(player, audioManager);
            }
        });
    }

    private static void playStreamingAudio(final PronunciationAudio audio) {
        safe(() -> {
            stopCurrentPlayback();

            final @Nullable AudioManager audioManager = ContextCompat.getSystemService(WkApplication.getInstance(), AudioManager.class);
            if (audioManager == null || !requestAudioFocus(audioManager)) {
                return;
            }

            final MediaPlayer player = new MediaPlayer();
            savedMediaPlayer = player;
            lastWasMale = audio.getMetadata().isMale();

            player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            player.setOnCompletionListener(mp -> releasePlayer(mp, audioManager));
            player.setOnErrorListener((mp, what, extra) -> {
                releasePlayer(mp, audioManager);
                return true;
            });
            player.setOnPreparedListener(MediaPlayer::start);

            try {
                player.setDataSource(audio.getUrl());
                player.prepareAsync();
            }
            catch (final Exception e) {
                releasePlayer(player, audioManager);
            }
        });
    }

    /**
     * Stream an audio file for a subject. Take into account the user's preferences,
     * and try to get a match for the given reading.
     *
     * @param subject the subject
     * @param lastMatchedAnswer the reading to match if possible
     */
    public static @Nullable PronunciationAudio getStreamingAudio(Subject subject, @Nullable String lastMatchedAnswer) {
        // Shuffle so pronunciation gender is random unless preferences alter it via the comparator.
        List<PronunciationAudio> audioList = shuffle(subject.getParsedPronunciationAudios());
        if (audioList.isEmpty()) {
            return null;
        }

        final VoicePreference voicePreference = GlobalSettings.Audio.getVoicePreference();
        // Prefer male if that is the preference or if the user chose alternate then prefer it if the last pronunciation was female.
        final boolean malePreferred = voicePreference == MALE || voicePreference == ALTERNATE && !lastWasMale;
        // Same as above for female preference. Same functionality for alternating.
        final boolean femalePreferred = voicePreference == FEMALE || voicePreference == ALTERNATE && lastWasMale;

        // Sorted based on pronunciation matching last matched answer and gender, taking into account their preference.
        final Comparator<PronunciationAudio> comparator = (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (isEqual(o1.getMetadata().getPronunciation(), lastMatchedAnswer) && !isEqual(o2.getMetadata().getPronunciation(), lastMatchedAnswer)) {
                return -1;
            }
            if (isEqual(o2.getMetadata().getPronunciation(), lastMatchedAnswer) && !isEqual(o1.getMetadata().getPronunciation(), lastMatchedAnswer)) {
                return 1;
            }
            if (malePreferred) {
                if (o1.getMetadata().isMale() && !o2.getMetadata().isMale()) {
                    return -1;
                }
                if (o2.getMetadata().isMale() && !o1.getMetadata().isMale()) {
                    return 1;
                }
            }
            if (femalePreferred) {
                if (o1.getMetadata().isFemale() && !o2.getMetadata().isFemale()) {
                    return -1;
                }
                if (o2.getMetadata().isFemale() && !o1.getMetadata().isFemale()) {
                    return 1;
                }
            }
            return 0;
        };

        Collections.sort(audioList, comparator);

        return audioList.get(0); // First audio should align with preferences.
    }

    /**
     * Schedule audio downloads for a set of subjects where needed, with a cap on the number of tasks scheduled.
     *
     * @param subjects the subjects to download for
     * @param maxCount the maximum number of downloads to schedule
     */
    public static void scheduleDownloadTasks(final Iterable<? extends PronunciationAudioOwner> subjects,
                                             final int maxCount) {
        if (GlobalSettings.getFirstTimeSetup() != 0) {
            final AppDatabase db = WkApplication.getDatabase();
            final Iterable<String> locationValues = AUDIO_STORAGE.getLocationValues();

            int count = 0;
            for (final PronunciationAudioOwner subject: subjects) {
                final int status = findAudioDownloadStatus(subject.getLevel(), subject.getParsedPronunciationAudios(), locationValues);
                if (status == 1 || status == 2) {
                    db.assertDownloadAudioTask(subject);
                    count++;
                    if (count >= maxCount) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Helper method: does the argument directory contain any regular files, recursively checked?.
     *
     * @param dir directory to scan
     * @return true if it does
     */
    private static boolean hasFiles(final File dir) {
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] children = dir.listFiles();
            if (children != null) {
                for (final File child: children) {
                    if (child.isFile()) {
                        return true;
                    }
                    if (child.isDirectory() && hasFiles(child)) {
                        return true;
                    }
                }
            }
            return false;
        }
        catch (final Exception e) {
            return false;
        }
    }

    /**
     * Are there any audio files that are not in the preferred location?.
     *
     * @return true if there are
     */
    public static boolean hasAnyMisplacedAudioFiles() {
        for (final String location: AUDIO_STORAGE.getLocationValues()) {
            if (location.equals(GlobalSettings.Api.getAudioLocation())) {
                continue;
            }
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            if (hasFiles(audioDir)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method: get the number of regular files in the argument directory.
     *
     * @param dir directory to scan
     * @return the number of regular files
     */
    private static int numFiles(final File dir) {
        int count = 0;
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] children = dir.listFiles();
            if (children != null) {
                for (final File child: children) {
                    if (child.isFile()) {
                        count++;
                    }
                    if (child.isDirectory()) {
                        count += numFiles(child);
                    }
                }
            }
        }
        catch (final Exception e) {
            //
        }
        return count;
    }

    /**
     * How many audio files are there that are not in the preferred location?.
     *
     * @return the number of regular files
     */
    public static int getNumMisplacedAudioFiles() {
        int count = 0;
        for (final String location: AUDIO_STORAGE.getLocationValues()) {
            if (location.equals(GlobalSettings.Api.getAudioLocation())) {
                continue;
            }
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            count += numFiles(audioDir);
        }
        return count;
    }

    /**
     * Helper method: iterate over all files in the argument directory, recursively scanned.
     * The consumer is called for every regular file found, and it should return true if the
     * file system walk should be aborted.
     *
     * @param dir the directory to walk
     * @param consumer the consumer for encountered regular files
     * @return true if the walk should be aborted
     */
    private static boolean iterateMisplacedAudioFilesHelper(final File dir, final Function<? super File, Boolean> consumer) {
        try {
            @androidx.annotation.Nullable
            final @Nullable File[] children = dir.listFiles();
            if (children != null) {
                for (final File child: children) {
                    if (child.isFile() && consumer.apply(child)) {
                        return true;
                    }
                    if (child.isDirectory() && iterateMisplacedAudioFilesHelper(child, consumer)) {
                        return true;
                    }
                }
            }
        }
        catch (final Exception e) {
            //
        }
        return false;
    }

    /**
     * Helper method: iterate over all misplaced audio files, recursively scanned.
     * The consumer is called for every regular file found, and it should return true if the
     * file system walk should be aborted.
     *
     * @param consumer the consumer for encountered regular files
     */
    public static void iterateMisplacedAudioFiles(final Function<? super File, Boolean> consumer) {
        for (final String location: AUDIO_STORAGE.getLocationValues()) {
            if (location.equals(GlobalSettings.Api.getAudioLocation())) {
                continue;
            }
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    continue;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final File audioDir = new File(baseDirectory, AUDIO_DIRECTORY_NAME);
            try {
                iterateMisplacedAudioFilesHelper(audioDir, consumer);
            }
            catch (final Exception e) {
                //
            }
        }
    }

    /**
     * Get the location of the argument file, mapped to the location specified by the argument
     * base directory. The part of the file's path before the AUDIO_DIRECTORY_NAME segment is
     * replaced with the base directory, so that the file's path is mapped to the equivalent
     * path on another storage.
     *
     * @param file the file to map
     * @param baseDirectory the base directory of the storage location to map to
     * @return the mapped file or null if something went wrong
     */
    private static @Nullable File findDestinationFile(final File file, final File baseDirectory) {
        if (file.getName().equals(AUDIO_DIRECTORY_NAME)) {
            return new File(baseDirectory, AUDIO_DIRECTORY_NAME);
        }

        final @Nullable File parent = file.getParentFile();
        if (parent == null) {
            return null;
        }

        final @Nullable File destinationParent = findDestinationFile(parent, baseDirectory);
        if (destinationParent == null) {
            return null;
        }

        return new File(destinationParent, file.getName());
    }

    /**
    * Simple check specifically for kana only vocab audio file checking
    * @param subject the subject to check
    */
    public static boolean hasAudio(final Subject subject) {
        final Iterable<String> locationValues = AUDIO_STORAGE.getLocationValues(); // Assuming you have this method as it's used elsewhere
        final List<PronunciationAudio> audios = subject.getParsedPronunciationAudios(); // Get all audio files related to the subject

        for (PronunciationAudio audio : audios) {
            if (AUDIO_STORAGE.hasAudioFileFor(subject.getLevel(), audio, locationValues)) {
                return true;
            }
        }

        return false;
    }
    /**
     * Move a misplaced audio file to its preferred location.
     *
     * @param file the file to move
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void moveToPreferredLocation(final File file) {
        @Nullable InputStream is = null;
        @Nullable OutputStream os = null;
        try {
            final String location = GlobalSettings.Api.getAudioLocation();
            final File baseDirectory;
            if (location.equals("Internal")) {
                @Nullable File dir = ContextCompat.getNoBackupFilesDir(WkApplication.getInstance());
                if (dir == null) {
                    dir = WkApplication.getInstance().getFilesDir();
                }
                if (dir == null) {
                    return;
                }
                baseDirectory = dir;
            }
            else {
                baseDirectory = new File(location);
            }
            final @Nullable File destinationFile = findDestinationFile(file, baseDirectory);
            if (destinationFile == null) {
                return;
            }
            final @Nullable File parent = destinationFile.getParentFile();
            if (parent == null) {
                return;
            }
            if (!parent.exists() && !parent.mkdirs()) {
                return;
            }
            if (destinationFile.exists() && file.length() <= destinationFile.length()) {
                file.delete();
                return;
            }

            LOGGER.info("Moving file %s to %s...", file, destinationFile);

            final File tempFile = requireNonNull(AUDIO_STORAGE.getTempFile(destinationFile));
            is = new FileInputStream(file);
            os = new FileOutputStream(tempFile);
            StreamUtil.pump(is, os);
            is.close();
            is = null;
            os.close();
            os = null;
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            tempFile.renameTo(destinationFile);
            file.delete();
        }
        catch (final Exception e) {
            //
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (final Exception e) {
                    //
                }
            }
            if (os != null) {
                try {
                    os.close();
                }
                catch (final Exception e) {
                    //
                }
            }
        }
    }
}
