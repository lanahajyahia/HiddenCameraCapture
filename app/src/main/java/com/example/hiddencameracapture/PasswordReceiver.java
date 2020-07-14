package com.example.hiddencameracapture;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class PasswordReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context ctxt, Intent intent) {
        ComponentName cn=new ComponentName(ctxt, PasswordReceiver.class);
        DevicePolicyManager mgr=
                (DevicePolicyManager)ctxt.getSystemService(Context.DEVICE_POLICY_SERVICE);

        mgr.setPasswordQuality(cn,
                DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);

        onPasswordChanged(ctxt, intent);
    }

    @Override
    public void onPasswordChanged(Context ctxt, Intent intent) {
        DevicePolicyManager mgr=
                (DevicePolicyManager)ctxt.getSystemService(Context.DEVICE_POLICY_SERVICE);
        int msgId;

        if (mgr.isActivePasswordSufficient()) {
            msgId=R.string.compliant;
        }
        else {
            msgId=R.string.not_compliant;
        }

        Toast.makeText(ctxt, msgId, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPasswordFailed(Context ctxt, Intent intent) {
//        Toast.makeText(ctxt, R.string.password_failed, Toast.LENGTH_LONG)
//                .show();
        CameraManager mgr = new CameraManager(ctxt);
        mgr.takePhoto();
        Toast.makeText(ctxt,"Photo saved to Pictures\\iSelfie",Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onPasswordSucceeded(Context ctxt, Intent intent) {
        Toast.makeText(ctxt, R.string.password_success, Toast.LENGTH_LONG)
                .show();
        Log.d("lana",""+ R.string.password_success);
    }


}


