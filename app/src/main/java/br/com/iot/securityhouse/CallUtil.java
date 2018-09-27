package br.com.iot.securityhouse;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;

import static android.support.v4.app.ActivityCompat.requestPermissions;

public class CallUtil {

    public static void checkPermissionPhoneCall(Activity activity, String phone) {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.CALL_PHONE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(activity, new String[]{android.Manifest.permission.CALL_PHONE},1);
        } else {
            activity.startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse("tel:" + phone)));
        }
    }
}