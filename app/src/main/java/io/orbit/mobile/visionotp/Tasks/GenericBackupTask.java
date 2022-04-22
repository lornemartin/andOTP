package io.orbit.mobile.visionotp.Tasks;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.shadowice.flocke.andotp.R;
import io.orbit.mobile.visionotp.Utilities.BackupHelper;
import io.orbit.mobile.visionotp.Utilities.Constants;
import io.orbit.mobile.visionotp.Utilities.Settings;
import io.orbit.mobile.visionotp.Utilities.StorageAccessHelper;

public abstract class GenericBackupTask extends UiBasedBackgroundTask<BackupTaskResult> {
    protected final Context applicationContext;
    protected final Settings settings;
    protected final Constants.BackupType type;
    protected Uri uri;

    public GenericBackupTask(Context context, @Nullable Uri uri) {
        super(BackupTaskResult.failure(BackupTaskResult.ResultType.BACKUP, R.string.backup_toast_export_failed));

        this.applicationContext = context.getApplicationContext();
        this.settings = new Settings(applicationContext);

        this.type = getBackupType();
        this.uri = uri;
    }

    @Override
    @NonNull
    protected BackupTaskResult doInBackground() {
        String fileName;

        if (uri == null) {
            BackupHelper.BackupFile backupFile = BackupHelper.backupFile(applicationContext, settings.getBackupLocation(), type);

            if (backupFile.file == null)
                return new BackupTaskResult(BackupTaskResult.ResultType.BACKUP,false, null, backupFile.errorMessage);

            uri = backupFile.file.getUri();
            fileName = backupFile.file.getName();
        } else {
            fileName = StorageAccessHelper.getContentFileName(applicationContext, uri);
        }

        boolean success = doBackup();

        if (success)
            return BackupTaskResult.success(BackupTaskResult.ResultType.BACKUP ,fileName);
        else
            return BackupTaskResult.failure(BackupTaskResult.ResultType.BACKUP, R.string.backup_toast_export_failed);
    }

    @NonNull
    protected abstract Constants.BackupType getBackupType();
    protected abstract boolean doBackup();
}