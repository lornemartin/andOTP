package io.orbit.mobile.visionotp.Tasks;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.orbit.mobile.visionotp.Database.Entry;
import io.orbit.mobile.visionotp.Utilities.BackupHelper;
import io.orbit.mobile.visionotp.Utilities.Constants;
import io.orbit.mobile.visionotp.Utilities.DatabaseHelper;

import java.util.ArrayList;

public class EncryptedBackupTask extends GenericBackupTask {
    private final String password;
    private final ArrayList<Entry> entries;

    public EncryptedBackupTask(Context context, ArrayList<Entry> entries, String password, @Nullable Uri uri) {
        super(context, uri);
        this.entries = entries;
        this.password = password;
    }

    @Override
    @NonNull
    protected Constants.BackupType getBackupType() {
        return Constants.BackupType.ENCRYPTED;
    }

    @Override
    protected boolean doBackup() {
        String payload = DatabaseHelper.entriesToString(entries);
        return BackupHelper.backupToFile(applicationContext, uri, password, payload);
    }
}
