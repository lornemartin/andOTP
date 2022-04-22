package io.orbit.mobile.visionotp.Tasks;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.orbit.mobile.visionotp.Database.Entry;
import io.orbit.mobile.visionotp.Utilities.Constants;
import io.orbit.mobile.visionotp.Utilities.DatabaseHelper;
import io.orbit.mobile.visionotp.Utilities.StorageAccessHelper;

import java.util.ArrayList;

public class PlainTextBackupTask extends GenericBackupTask {
    private final ArrayList<Entry> entries;

    public PlainTextBackupTask(Context context, ArrayList<Entry> entries, @Nullable Uri uri) {
        super(context, uri);
        this.entries = entries;
    }

    @Override
    @NonNull
    protected Constants.BackupType getBackupType() {
        return Constants.BackupType.PLAIN_TEXT;
    }

    @Override
    protected boolean doBackup() {
        String payload = DatabaseHelper.entriesToString(entries);
        return StorageAccessHelper.saveFile(applicationContext, uri, payload);
    }
}