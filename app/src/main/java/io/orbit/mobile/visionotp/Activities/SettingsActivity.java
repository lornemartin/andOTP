/*
 * Copyright (C) 2017-2020 Jakob Nixdorf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.orbit.mobile.visionotp.Activities;

import android.app.backup.BackupManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import android.provider.DocumentsContract;
import android.util.Log;
import android.view.ViewStub;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.openintents.openpgp.util.OpenPgpAppPreference;
import org.openintents.openpgp.util.OpenPgpKeyPreference;
import io.orbit.mobile.visionotp.Preferences.CredentialsPreference;
import org.shadowice.flocke.andotp.R;
import io.orbit.mobile.visionotp.Tasks.ChangeEncryptionTask;
import io.orbit.mobile.visionotp.Utilities.BackupHelper;
import io.orbit.mobile.visionotp.Utilities.Constants;
import io.orbit.mobile.visionotp.Utilities.DatabaseHelper;
import io.orbit.mobile.visionotp.Utilities.EncryptionHelper;
import io.orbit.mobile.visionotp.Utilities.KeyStoreHelper;
import io.orbit.mobile.visionotp.Utilities.Settings;
import io.orbit.mobile.visionotp.Utilities.UIHelper;

import java.util.Locale;

import javax.crypto.SecretKey;

public class SettingsActivity extends BackgroundTaskActivity<ChangeEncryptionTask.Result>
        implements SharedPreferences.OnSharedPreferenceChangeListener, EncryptionHelper.EncryptionChangeCallback {

    private SettingsFragment fragment;
    private SharedPreferences prefs;

    private SecretKey encryptionKey = null;
    private boolean encryptionChanged = false;

    private Snackbar inProgress = null;
    private boolean canGoBack = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings_activity_title);
        setContentView(R.layout.activity_container);

        Toolbar toolbar = findViewById(R.id.container_toolbar);
        setSupportActionBar(toolbar);

        ViewStub stub = findViewById(R.id.container_stub);
        stub.inflate();

        Intent callingIntent = getIntent();
        byte[] keyMaterial = callingIntent.getByteArrayExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY);
        if (keyMaterial != null && keyMaterial.length > 0)
            encryptionKey = EncryptionHelper.generateSymmetricKey(keyMaterial);

        if (savedInstanceState != null) {
            encryptionChanged = savedInstanceState.getBoolean(Constants.EXTRA_SETTINGS_ENCRYPTION_CHANGED, false);

            byte[] encKey = savedInstanceState.getByteArray(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY);
            if (encKey != null) {
                encryptionKey = EncryptionHelper.generateSymmetricKey(encKey);
            }
        }

        fragment = new SettingsFragment();
        fragment.setEncryptionKey(encryptionKey);
        fragment.setEncryptionChangeCallback(this);

        getFragmentManager().beginTransaction()
                .replace(R.id.container_content, fragment)
                .commit();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(Constants.EXTRA_SETTINGS_ENCRYPTION_CHANGED, encryptionChanged);
        outState.putByteArray(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY, encryptionKey.getEncoded());
    }

    public void finishWithResult() {
        Intent data = new Intent();

        data.putExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_CHANGED, encryptionChanged);
        if (encryptionKey != null)
            data.putExtra(Constants.EXTRA_SETTINGS_ENCRYPTION_KEY, encryptionKey.getEncoded());

        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (canGoBack)
            finishWithResult();

        return true;
    }

    @Override
    public void onBackPressed() {
        if (canGoBack) {
            finishWithResult();
            super.onBackPressed();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        if (key.equals(getString(R.string.settings_key_theme)) ||
                key.equals(getString(R.string.settings_key_lang)) ||
                key.equals(getString(R.string.settings_key_special_features)) ||
                key.equals(getString(R.string.settings_key_backup_location)) ||
                key.equals(getString(R.string.settings_key_theme_mode)) ||
                key.equals(getString(R.string.settings_key_theme_black_auto))) {
            recreate();
        } else if(key.equals(getString(R.string.settings_key_encryption))) {
            if (settings.getEncryption() != Constants.EncryptionType.PASSWORD) {
                if (settings.getAndroidBackupServiceEnabled()) {
                    UIHelper.showGenericDialog(this,
                        R.string.settings_dialog_title_android_sync,
                        R.string.settings_dialog_msg_android_sync_disabled_encryption
                    );
                }

                settings.setAndroidBackupServiceEnabled(false);
                if (fragment.useAndroidSync != null) {
                    fragment.useAndroidSync.setEnabled(false);
                    fragment.useAndroidSync.setChecked(false);
                }
            } else {
                if (fragment.useAndroidSync != null)
                    fragment.useAndroidSync.setEnabled(true);
            }
        } else if(key.equals(getString(R.string.settings_key_enable_android_backup_service))) {
            Log.d(SettingsActivity.class.getSimpleName(), "onSharedPreferenceChanged called modifying settings_key_enable_android_backup_service service is now: " +
                    (settings.getAndroidBackupServiceEnabled() ? "enabled" : "disabled"));

            int message = settings.getAndroidBackupServiceEnabled() ? R.string.settings_toast_android_sync_enabled : R.string.settings_toast_android_sync_disabled;
            Snackbar.make(fragment.getView(), message, BaseTransientBottomBar.LENGTH_SHORT).show();
        }

        fragment.updateAutoBackup();
    }

    private void generateNewEncryptionKey() {
        if (settings.getEncryption() == Constants.EncryptionType.KEYSTORE) {
            encryptionKey = KeyStoreHelper.loadEncryptionKeyFromKeyStore(this, false);
            encryptionChanged = true;
        }
    }

    private void tryEncryptionChangeWithAuth(Constants.EncryptionType newEnc) {
        Intent authIntent = new Intent(this, AuthenticateActivity.class);
        authIntent.putExtra(Constants.EXTRA_AUTH_NEW_ENCRYPTION, newEnc.name());
        authIntent.putExtra(Constants.EXTRA_AUTH_MESSAGE, R.string.auth_msg_confirm_encryption);
        startActivityForResult(authIntent, Constants.INTENT_SETTINGS_AUTHENTICATE);
    }

    @Override
    public void onSuccessfulEncryptionChange(Constants.EncryptionType newEncryptionType, SecretKey newEncryptionKey) {
        encryptionKey = newEncryptionKey;
        encryptionChanged = true;

        fragment.encryption.setValue(newEncryptionType.name().toLowerCase());
        fragment.setEncryptionKey(newEncryptionKey);
    }

    @Override
    void onTaskResult(ChangeEncryptionTask.Result result) {
        switch (result.result) {
            case SUCCESS:
                onSuccessfulEncryptionChange(result.encryptionType, result.newEncryptionKey);
                Snackbar.make(fragment.getView(), R.string.settings_toast_encryption_change_success, BaseTransientBottomBar.LENGTH_SHORT).show();
                break;
            case BACKUP_FAILURE:
                Snackbar.make(fragment.getView(), R.string.settings_toast_encryption_backup_failed, BaseTransientBottomBar.LENGTH_SHORT).show();
                break;
            case CHANGE_FAILURE:
                Snackbar.make(fragment.getView(), R.string.settings_toast_encryption_change_failed, BaseTransientBottomBar.LENGTH_SHORT).show();
                break;
        }
    }

    private void startEncryptionChangeTask(Constants.EncryptionType newEnc, byte[] newKey) {
        ChangeEncryptionTask task = new ChangeEncryptionTask(this, encryptionKey, newEnc, newKey);
        startBackgroundTask(task);
    }

    @Override
    protected void setupUiForTaskState(boolean taskRunning) {
        if (taskRunning) {
            // TODO: Better in-progress notification
            inProgress = Snackbar.make(fragment.getView(), R.string.settings_toast_encryption_changing, BaseTransientBottomBar.LENGTH_INDEFINITE);
            inProgress.show();

            canGoBack = false;
        } else {
            if (inProgress != null)
                inProgress.dismiss();

            canGoBack = true;
        }
    }

    private void requestBackupAccess() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && settings.isBackupLocationSet())
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, settings.getBackupLocation());

        startActivityForResult(intent, Constants.INTENT_SETTINGS_BACKUP_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.INTENT_SETTINGS_AUTHENTICATE) {
            if (resultCode == RESULT_OK) {
                byte[] authKey = data.getByteArrayExtra(Constants.EXTRA_AUTH_PASSWORD_KEY);
                String newEnc = data.getStringExtra(Constants.EXTRA_AUTH_NEW_ENCRYPTION);

                if (authKey != null && authKey.length > 0 && newEnc != null && !newEnc.isEmpty()) {
                    Constants.EncryptionType newEncType = Constants.EncryptionType.valueOf(newEnc);
                    startEncryptionChangeTask(newEncType, authKey);
                } else {
                    Snackbar.make(fragment.getView(), R.string.settings_toast_encryption_no_key, BaseTransientBottomBar.LENGTH_SHORT).show();
                }
            } else {
                Snackbar.make(fragment.getView(), R.string.settings_toast_encryption_auth_failed, BaseTransientBottomBar.LENGTH_SHORT).show();
            }
        } else if (requestCode == Constants.INTENT_SETTINGS_BACKUP_LOCATION && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                settings.setBackupLocation(treeUri);
            }
        } else {
            // Handled in OpenPgpKeyPreference
            fragment.pgpSigningKey.handleOnActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        prefs = null;

        super.onDestroy();
    }

    public static class SettingsFragment extends PreferenceFragment {
        PreferenceCategory catUI;

        private Settings settings;

        private CredentialsPreference credentialsPreference;
        private SecretKey encryptionKey;
        private EncryptionHelper.EncryptionChangeCallback encryptionChangeCallback;

        private ListPreference encryption;
        private ListPreference useAutoBackup;
        private CheckBoxPreference useAndroidSync;

        private EditTextPreference pgpEncryptionKey;
        private OpenPgpKeyPreference pgpSigningKey;
        ListPreference themeMode;
        CheckBoxPreference themeBlack;
        ListPreference theme;

        public void encryptionChangeWithDialog(final Constants.EncryptionType encryptionType) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.settings_dialog_title_warning)
                    .setMessage(R.string.settings_dialog_msg_encryption_change)
                    .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        if (encryptionType == Constants.EncryptionType.PASSWORD)
                            ((SettingsActivity) getActivity()).tryEncryptionChangeWithAuth(encryptionType);
                        else if (encryptionType == Constants.EncryptionType.KEYSTORE)
                            ((SettingsActivity) getActivity()).startEncryptionChangeTask(encryptionType, null);
                    })
                    .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    })
                    .create()
                    .show();
        }

        public void updateAutoBackup() {
            if (useAutoBackup != null) {
                useAutoBackup.setEnabled(BackupHelper.autoBackupType(getActivity()) == Constants.BackupType.ENCRYPTED);
                if (!useAutoBackup.isEnabled())
                    useAutoBackup.setValue(Constants.AutoBackup.OFF.toString().toLowerCase(Locale.ENGLISH));

                if (useAutoBackup.isEnabled()) {
                    useAutoBackup.setSummary(R.string.settings_desc_auto_backup_password_enc);
                } else {
                    useAutoBackup.setSummary(R.string.settings_desc_auto_backup_requirements);
                }
            }
        }

        public void setEncryptionKey(SecretKey encryptionKey) {
            this.encryptionKey = encryptionKey;

            if (credentialsPreference != null)
                credentialsPreference.setOldEncryptionKey(encryptionKey);
        }

        public void setEncryptionChangeCallback(EncryptionHelper.EncryptionChangeCallback changeCallback) {
            this.encryptionChangeCallback = changeCallback;

            if (credentialsPreference != null)
                credentialsPreference.setEncryptionChangeCallback(changeCallback);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            settings = new Settings(getActivity());

            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

            addPreferencesFromResource(R.xml.preferences);

            credentialsPreference = (CredentialsPreference) findPreference(getString(R.string.settings_key_auth));
            credentialsPreference.setOldEncryptionKey(encryptionKey);
            credentialsPreference.setEncryptionChangeCallback(encryptionChangeCallback);

            CheckBoxPreference blockAutofill = (CheckBoxPreference) findPreference(getString(R.string.settings_key_block_autofill));
            CheckBoxPreference autoUnlockAfterAutofill = (CheckBoxPreference) findPreference(getString(R.string.settings_key_auto_unlock_after_autofill));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                blockAutofill.setEnabled(true);
                blockAutofill.setSummary(R.string.settings_desc_block_autofill);
                autoUnlockAfterAutofill.setEnabled(true);
                autoUnlockAfterAutofill.setSummary(R.string.settings_desc_auto_unlock_after_autofill);
            } else {
                blockAutofill.setEnabled(false);
                blockAutofill.setSummary(R.string.settings_desc_autofill_requires_android_o);
                autoUnlockAfterAutofill.setEnabled(false);
                autoUnlockAfterAutofill.setSummary(R.string.settings_desc_autofill_requires_android_o);
            }

            // Authentication
            catUI = (PreferenceCategory) findPreference(getString(R.string.settings_key_cat_ui));
            encryption = (ListPreference) findPreference(getString(R.string.settings_key_encryption));
            themeMode = (ListPreference) findPreference(getString(R.string.settings_key_theme_mode));
            themeBlack = (CheckBoxPreference) findPreference(getString(R.string.settings_key_theme_black_auto));
            theme = (ListPreference) findPreference(getString(R.string.settings_key_theme));

            encryption.setOnPreferenceChangeListener((preference, o) -> {
                String newEncryption = (String) o;
                Constants.EncryptionType encryptionType = Constants.EncryptionType.valueOf(newEncryption.toUpperCase());
                Constants.EncryptionType oldEncryptionType = settings.getEncryption();
                Constants.AuthMethod authMethod = settings.getAuthMethod();

                if (encryptionType != oldEncryptionType) {
                    if (encryptionType == Constants.EncryptionType.PASSWORD) {
                        if (authMethod != Constants.AuthMethod.PASSWORD && authMethod != Constants.AuthMethod.PIN) {
                            UIHelper.showGenericDialog(getActivity(), R.string.settings_dialog_title_error, R.string.settings_dialog_msg_encryption_invalid_with_auth);
                            return false;
                        } else {
                            if (settings.getAuthCredentials().isEmpty()) {
                                UIHelper.showGenericDialog(getActivity(), R.string.settings_dialog_title_error, R.string.settings_dialog_msg_encryption_invalid_without_credentials);
                                return false;
                            }
                        }

                        encryptionChangeWithDialog(Constants.EncryptionType.PASSWORD);
                    } else if (encryptionType == Constants.EncryptionType.KEYSTORE) {
                        encryptionChangeWithDialog(Constants.EncryptionType.KEYSTORE);
                    }
                }

                return false;
            });

            // Backup location
            Preference backupLocation = findPreference(getString(R.string.settings_key_backup_location));

            if (settings.isBackupLocationSet()) {
                backupLocation.setSummary(R.string.settings_desc_backup_location_set);
            } else {
                backupLocation.setSummary(R.string.settings_desc_backup_location);
            }

            backupLocation.setOnPreferenceClickListener(preference -> {
                ((SettingsActivity) getActivity()).requestBackupAccess();
                return true;
            });

            // OpenPGP
            OpenPgpAppPreference pgpProvider = (OpenPgpAppPreference) findPreference(getString(R.string.settings_key_openpgp_provider));
            pgpEncryptionKey = (EditTextPreference) findPreference(getString(R.string.settings_key_openpgp_key_encrypt));
            pgpSigningKey = (OpenPgpKeyPreference) findPreference(getString(R.string.settings_key_openpgp_key_sign));

            pgpSigningKey.setOpenPgpProvider(pgpProvider.getValue());

            pgpEncryptionKey.setEnabled(pgpProvider.getValue() != null && !pgpProvider.getValue().isEmpty());

            pgpProvider.setOnPreferenceChangeListener((preference, newValue) -> {
                pgpEncryptionKey.setEnabled(newValue != null && !((String) newValue).isEmpty());
                pgpSigningKey.setOpenPgpProvider((String) newValue);

                return true;
            });

            useAutoBackup = (ListPreference)findPreference(getString(R.string.settings_key_auto_backup_password_enc));
            updateAutoBackup();

            useAndroidSync = (CheckBoxPreference) findPreference(getString(R.string.settings_key_enable_android_backup_service));
            useAndroidSync.setEnabled(settings.getEncryption() == Constants.EncryptionType.PASSWORD);
            if(!useAndroidSync.isEnabled())
                useAndroidSync.setChecked(false);

            if (sharedPref.contains(getString(R.string.settings_key_special_features)) &&
                    sharedPref.getBoolean(getString(R.string.settings_key_special_features), false)) {
                addPreferencesFromResource(R.xml.preferences_special);

                Preference clearKeyStore = findPreference(getString(R.string.settings_key_clear_keystore));
                clearKeyStore.setOnPreferenceClickListener(preference -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setTitle(R.string.settings_dialog_title_clear_keystore);
                    if (settings.getEncryption() == Constants.EncryptionType.PASSWORD)
                        builder.setMessage(R.string.settings_dialog_msg_clear_keystore_password);
                    else if (settings.getEncryption() == Constants.EncryptionType.KEYSTORE)
                        builder.setMessage(R.string.settings_dialog_msg_clear_keystore_keystore);

                    builder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                        KeyStoreHelper.wipeKeys(getActivity());
                        if (settings.getEncryption() == Constants.EncryptionType.KEYSTORE) {
                            DatabaseHelper.wipeDatabase(getActivity());
                            ((SettingsActivity) getActivity()).generateNewEncryptionKey();
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    });

                    builder.create().show();
                    return false;
                });
            }
            // Remove Theme Mode selection option for devices below Android 10. Disable theme selection if Theme Mode is set auto
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                catUI.removePreference(themeMode);
                catUI.removePreference(themeBlack);
            } else {
                if (sharedPref.getString(getString(R.string.settings_key_theme_mode), getString(R.string.settings_default_theme_mode)).equals("auto"))
                    catUI.removePreference(theme);
                else
                    catUI.removePreference(themeBlack);
            }
        }
    }
}
