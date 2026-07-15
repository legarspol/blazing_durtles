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

import com.smouldering_durtles.wk.GlobalSettings;
import com.smouldering_durtles.wk.api.model.PronunciationAudio;
import com.smouldering_durtles.wk.db.model.PronunciationAudioOwner;
import com.smouldering_durtles.wk.db.model.Subject;
import com.smouldering_durtles.wk.enums.VoicePreference;
import com.smouldering_durtles.wk.model.GenderedFile;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.enums.VoicePreference.ALTERNATE;
import static com.smouldering_durtles.wk.enums.VoicePreference.FEMALE;
import static com.smouldering_durtles.wk.enums.VoicePreference.MALE;
import static com.smouldering_durtles.wk.util.ObjectSupport.isEqual;
import static com.smouldering_durtles.wk.util.ObjectSupport.shuffle;

/**
 * Selects pronunciation audio for a subject or reading, taking into account the user's voice
 * preference and, where relevant, which audio files are already downloaded.
 */
public final class AudioSelector {
    private final AudioStorage audioStorage;

    /**
     * Whether the most recently played audio was male, used to alternate voices when the
     * user's preference is {@link VoicePreference#ALTERNATE}.
     */
    private boolean lastWasMale = false;

    public AudioSelector(final AudioStorage audioStorage) {
        this.audioStorage = audioStorage;
    }

    /**
     * Record whether the audio that was actually played was male, for the benefit of the next
     * {@link VoicePreference#ALTERNATE} selection.
     *
     * @param male true if the played audio was male
     */
    public void setLastWasMale(final boolean male) {
        lastWasMale = male;
    }

    /**
     * Build a comparator that ranks candidate audio records by: presence of a downloaded file
     * (if requested), whether the reading predicate matches, and the user's gender preference.
     *
     * @param readingMatches predicate that decides whether an audio record's reading is the preferred one
     * @param level the subject's level, only used when filePresenceTiebreaker is true
     * @param locationValues the available storage locations, or null if file presence should not be a tiebreaker
     * @return the comparator
     */
    private Comparator<PronunciationAudio> buildComparator(final Predicate<PronunciationAudio> readingMatches,
                                                            final int level,
                                                            final @Nullable Iterable<String> locationValues) {
        final VoicePreference voicePreference = GlobalSettings.Audio.getVoicePreference();
        final boolean malePreferred = voicePreference == MALE || voicePreference == ALTERNATE && !lastWasMale;
        final boolean femalePreferred = voicePreference == FEMALE || voicePreference == ALTERNATE && lastWasMale;

        return (o1, o2) -> {
            if (o1 == o2) {
                return 0;
            }
            if (locationValues != null) {
                if (audioStorage.hasAudioFileFor(level, o1, locationValues) && !audioStorage.hasAudioFileFor(level, o2, locationValues)) {
                    return -1;
                }
                if (audioStorage.hasAudioFileFor(level, o2, locationValues) && !audioStorage.hasAudioFileFor(level, o1, locationValues)) {
                    return 1;
                }
            }
            if (readingMatches.test(o1) && !readingMatches.test(o2)) {
                return -1;
            }
            if (readingMatches.test(o2) && !readingMatches.test(o1)) {
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
    }

    /**
     * Get a random existing audio file, taking into account the user's preference.
     * Prefer the primary reading.
     *
     * @param subject the subject to get audio for
     * @return the file, or null if none exists
     */
    public @Nullable GenderedFile getOneAudioFile(final Subject subject) {
        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final int level = subject.getLevel();
        final Iterable<String> locationValues = audioStorage.getLocationValues();

        final Comparator<PronunciationAudio> comparator = buildComparator(
                audio -> subject.isPrimaryReading(audio.getMetadata().getPronunciation()), level, locationValues);

        Collections.sort(shuffled, comparator);

        return audioStorage.getExistingFileForAudio(level, shuffled.get(0), locationValues);
    }

    /**
     * Get a random existing audio file that must match a specific reading,
     * taking into account the user's preference.
     *
     * @param subject the subject to get audio for
     * @param reading the reading to find audio for
     * @return the file, or null if none exists
     */
    public @Nullable GenderedFile getOneAudioFileMustMatch(final PronunciationAudioOwner subject, final @Nullable String reading) {
        if (reading == null) {
            return null;
        }

        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final int level = subject.getLevel();
        final Iterable<String> locationValues = audioStorage.getLocationValues();

        final Comparator<PronunciationAudio> comparator = buildComparator(
                audio -> isEqual(audio.getMetadata().getPronunciation(), reading), level, locationValues);

        Collections.sort(shuffled, comparator);

        final PronunciationAudio audio = shuffled.get(0);
        if (isEqual(audio.getMetadata().getPronunciation(), reading)) {
            return audioStorage.getExistingFileForAudio(level, audio, locationValues);
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
    public @Nullable GenderedFile getOneAudioFileShouldMatch(final Subject subject, final @Nullable String reading) {
        final @Nullable GenderedFile file = getOneAudioFileMustMatch(subject, reading);
        return file == null ? getOneAudioFile(subject) : file;
    }

    /**
     * Select an audio record for streaming a subject's audio. Take into account the user's
     * preferences, and try to get a match for the given reading.
     *
     * @param subject the subject
     * @param lastMatchedAnswer the reading to match if possible
     * @return the audio record, or null if none exists
     */
    public @Nullable PronunciationAudio getStreamingAudio(final Subject subject, final @Nullable String lastMatchedAnswer) {
        final List<PronunciationAudio> shuffled = shuffle(subject.getParsedPronunciationAudios());
        if (shuffled.isEmpty()) {
            return null;
        }

        final Comparator<PronunciationAudio> comparator = buildComparator(
                audio -> isEqual(audio.getMetadata().getPronunciation(), lastMatchedAnswer), subject.getLevel(), null);

        Collections.sort(shuffled, comparator);

        return shuffled.get(0);
    }
}
