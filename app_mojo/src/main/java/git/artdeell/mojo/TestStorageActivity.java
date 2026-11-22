package git.artdeell.mojo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import git.artdeell.mojo.prefs.LauncherPreferences;
import git.artdeell.mojo.tasks.AsyncAssetManager;

public class TestStorageActivity extends Activity {
    private final int REQUEST_STORAGE_REQUEST_CODE = 1;
    private AlertDialog mPermissionRequestDialog;
    private boolean mPermsRequired = false;
    private boolean mPermsDialogShown = false;
    private boolean mAllFilesRequired = false;
    private boolean mAllFilesDialogShown = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPermsDialogShown = false;
        if(needsAllFilesAccess()) {
            // Handled in onResume() so the user can come back from settings.
            mAllFilesRequired = true;
            return;
        }
        if(Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29 && !isStorageAllowed(this)) {
            mPermsRequired = true;
        } else exit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAllFilesRequired) {
            handleAllFilesResume();
            return;
        }
        if(!mPermsRequired) return;
        if(!mPermsDialogShown) requestStoragePermission();
        else showRerequestDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mPermissionRequestDialog != null) mPermissionRequestDialog.dismiss();
    }

    private static boolean needsAllFilesAccess() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager();
    }

    private void handleAllFilesResume() {
        if(!needsAllFilesAccess()) {
            // Granted (possibly via settings), continue normally.
            mAllFilesRequired = false;
            exit();
            return;
        }
        if(!mAllFilesDialogShown) {
            mAllFilesDialogShown = true;
            showAllFilesDialog();
            return;
        }
        // Already prompted once this launch and user did not grant; continue without it.
        mAllFilesRequired = false;
        exit();
    }

    private void showAllFilesDialog() {
        if(mPermissionRequestDialog != null) mPermissionRequestDialog.dismiss();
        mPermissionRequestDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.all_files_permission_title)
                .setMessage(R.string.all_files_permission_message)
                .setCancelable(false)
                .setPositiveButton(R.string.all_files_permission_grant, (d, i) -> openAllFilesSettings())
                .setNegativeButton(R.string.all_files_permission_later, (d, i) -> {
                    mAllFilesRequired = false;
                    exit();
                })
                .show();
    }

    private void openAllFilesSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    private void showRerequestDialog() {
        if(mPermissionRequestDialog != null) mPermissionRequestDialog.dismiss();
        mPermissionRequestDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.global_error)
                .setMessage(R.string.toast_permission_denied)
                .setPositiveButton(android.R.string.ok,(d,i)->requestStoragePermission())
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mPermsRequired = false;
                exit();
            } else {
                mPermsDialogShown = true;
                showRerequestDialog();
            }
        }
    }

    public static boolean isStorageAllowed(Context context) {
        //Getting the permission status
        int result1 = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);


        //If permission is granted returning true
        return result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_REQUEST_CODE);
    }

    private void exit() {
        if(!Tools.checkStorageRoot(this)) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            return;
        }
        //Initialize constants (implicitly) and preferences after we confirm that we have storage.
        LauncherPreferences.loadPreferences(this);
        AsyncAssetManager.unpackComponents(this);
        AsyncAssetManager.unpackSingleFiles(this);

        Intent intent =  new Intent(this, LauncherActivity.class);
        startActivity(intent);
        finish();
    }
}
