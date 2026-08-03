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

package com.smouldering_durtles.wk.components;

import android.content.SharedPreferences;

import androidx.preference.PreferenceDataStore;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.smouldering_durtles.wk.WkApplication;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.annotation.Nullable;

import static com.smouldering_durtles.wk.util.ObjectSupport.safe;
import static com.smouldering_durtles.wk.util.ObjectSupport.safeNullable;

/**
 * Preference storage for the two values that must not sit in cleartext on disk: the WaniKani
 * API token, and the website password used by the deferred burn/resurrect scraping.
 *
 * <p>Backed by {@link EncryptedSharedPreferences}, with keys encrypted using AES256-SIV and
 * values using AES256-GCM under a Keystore-held master key. This is a separate preferences
 * file from the app's general settings, which stay unencrypted.
 */
public final class EncryptedPreferenceDataStore extends PreferenceDataStore {
    private @Nullable SharedPreferences encryptedPrefs = null;

    private SharedPreferences encryptedPrefs() throws IOException, GeneralSecurityException {
        if (encryptedPrefs == null) {
            final MasterKey masterKey = new MasterKey.Builder(WkApplication.getInstance(), MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(WkApplication.getInstance(),
                    "encrypted_shared_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        }
        return encryptedPrefs;
    }

    @Override
    public @Nullable String getString(final String key, final @Nullable String defValue) {
        return safeNullable(() -> encryptedPrefs().getString(key, defValue));
    }

    @Override
    public void putString(final String key, final @Nullable String value) {
        safe(() -> {
            final SharedPreferences.Editor editor = encryptedPrefs().edit();
            editor.putString(key, value);
            editor.apply();
        });
    }
}
