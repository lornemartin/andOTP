package io.orbit.mobile.visionotp.Dialogs;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;

import org.shadowice.flocke.andotp.R;
import io.orbit.mobile.visionotp.Utilities.Settings;
import io.orbit.mobile.visionotp.Utilities.Tools;

public class HideableDialog extends AppCompatDialog
    implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private final Settings settings;
    private final int hideSettingId;

    public HideableDialog(@NonNull Context context, int titleId, int msgId, int hideSettingId) {
        super(context, Tools.getThemeResource(context, R.attr.dialogTheme));

        this.settings = new Settings(context);
        this.hideSettingId = hideSettingId;

        setTitle(titleId);
        setContentView(R.layout.dialog_dont_show_again);

        TextView content = findViewById(R.id.dialogContent);
        CheckBox dontShowAgain = findViewById(R.id.dontShowAgain);
        Button buttonOk = findViewById(R.id.buttonOk);

        assert content != null;
        assert dontShowAgain != null;
        assert buttonOk != null;

        content.setText(msgId);

        dontShowAgain.setOnCheckedChangeListener(this);
        buttonOk.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (settings != null && hideSettingId > 0)
            settings.setBoolean(hideSettingId, b);
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }

    public static void ShowHidableDialog(Context context, int titleId, int msgId, int hideSettingId) {
        Settings settings = new Settings(context);

        if (!settings.getBoolean(hideSettingId, false)) {
            HideableDialog dialog = new HideableDialog(context, titleId, msgId, hideSettingId);
            dialog.show();
        }
    }
}
