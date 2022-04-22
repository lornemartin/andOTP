package io.orbit.mobile.visionotp.Tasks;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.orbit.mobile.visionotp.Utilities.Constants;
import io.orbit.mobile.visionotp.Utilities.StorageAccessHelper;

public class PGPBackupTask extends GenericBackupTask {
    private final String payload;

    public PGPBackupTask(Context context, String payload, @Nullable Uri uri) {
        super(context, uri);
        this.payload = payload;
    }

    @Override
    @NonNull
    protected Constants.BackupType getBackupType() {
        return Constants.BackupType.OPEN_PGP;
    }

    @Override
    protected boolean doBackup() {
        return StorageAccessHelper.saveFile(applicationContext, uri, payload);
    }
}