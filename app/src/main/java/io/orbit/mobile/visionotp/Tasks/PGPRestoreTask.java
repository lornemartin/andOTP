package io.orbit.mobile.visionotp.Tasks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import io.orbit.mobile.visionotp.Utilities.StorageAccessHelper;

public class PGPRestoreTask extends GenericRestoreTask {
    private final Intent decryptIntent;

    public PGPRestoreTask(Context context, Uri uri, Intent decryptIntent) {
        super(context, uri);
        this.decryptIntent = decryptIntent;
    }

    @Override
    @NonNull
    protected BackupTaskResult doInBackground() {
        String data = StorageAccessHelper.loadFileString(applicationContext, uri);

        return new BackupTaskResult(BackupTaskResult.ResultType.RESTORE,true, data, 0, true, decryptIntent, uri);
    }
}
