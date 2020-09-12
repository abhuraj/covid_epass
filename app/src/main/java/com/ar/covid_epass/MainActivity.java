package com.ar.covid_epass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ar.covid_epass.databinding.ActivityMainBinding;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity implements  EasyPermissions.PermissionCallbacks{

    private static final String TAG = "act_Main";
    ActivityMainBinding mainBinding;
    String baseUrl = "https://covid19.mhpolice.in/";
    WebView webview1;
    Button btnBack,btnForward;
    private ValueCallback<Uri[]> mUploadMessage,uploadMessage;
    private Uri[] results;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;
    /**/
    private final static int FCR = 1;
    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private FilePickerDialog dialog;
    private PermissionRequest mPermissionRequest;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String[] PERM_CAMERA =
            {Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mainBinding = DataBindingUtil.setContentView(this,R.layout.activity_main);
        setContentView(R.layout.activity_main);
        initViews();
        setOnClickListener();
        requestPermission();

    }
    private void requestPermission() {
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);

        if(result1 == PackageManager.PERMISSION_GRANTED){
            if(hasCameraPermission()){
                setWebViewContent();
            }else{
                EasyPermissions.requestPermissions(
                        this,
                        "This app needs access to your camera so you can take pictures.",
                        REQUEST_CAMERA_PERMISSION,
                        PERM_CAMERA);
            }
        }else {
            ActivityCompat.requestPermissions(this, new String[]{ CAMERA}, 200);
        }

    }

    private void setOnClickListener() {
        ((Button)findViewById(R.id.btn_back)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goBack();
                    }
                });
        ((Button)findViewById(R.id.btn_forward)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goForward();
                    }
                });
    }

    private void initViews() {
        webview1 = findViewById(R.id.webview1);
    }

    private void setWebViewContent() {
        webview1.setWebViewClient(new MyBrowser());
        webview1.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webview1.canGoBackOrForward(3);
        webview1.computeScroll();
        webview1.zoomIn();
        webview1.zoomOut();
        webview1.reload();
        webview1.findNext(true);
        webview1.pageUp(true);
        webview1.pageDown(true);
        webview1.loadUrl(baseUrl);

        WebSettings webSettings = webview1.getSettings();
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setAppCacheEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadsImagesAutomatically(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        webSettings.getAllowFileAccess();

        webview1.setWebChromeClient(new WebChromeClient() {

            //For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);
            }

            // For Android 3.0+, above method not supported in some android 3+ versions, in such case we use this
            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Browser"),FCR);
            }

            //For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), MainActivity.FCR);
            }

            //For Android 5.0+
            public boolean onShowFileChooser(
                    WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                }
                mUploadMessage = filePathCallback;
                openFileSelectionDialog();//onShowFileChooser
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.i(TAG, "onPermissionRequest");
                mPermissionRequest = request;
                final String[] requestedResources = request.getResources();
                for (String r : requestedResources) {
                    if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        // In this sample, we only accept video capture request.
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Allow Permission to camera")
                                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        mPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                                        Log.d(TAG,"Granted");
                                    }
                                })
                                .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        mPermissionRequest.deny();
                                        Log.d(TAG,"Denied");
                                    }
                                });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                        break;
                    }
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                super.onPermissionRequestCanceled(request);
                Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileSelectionDialog() {

        if (null != dialog && dialog.isShowing()) {
            dialog.dismiss();
        }

        //Create a DialogProperties object.
        final DialogProperties properties = new DialogProperties();
        //Instantiate FilePickerDialog with Context and DialogProperties.
        dialog = new FilePickerDialog(this, properties);
        dialog.setTitle("Select a File");
        dialog.setPositiveBtnName("Select");
        dialog.setNegativeBtnName("Cancel");
        properties.selection_mode = DialogConfigs.MULTI_MODE; // for multiple files
        //properties.selection_mode = DialogConfigs.SINGLE_MODE; // for single file
        properties.selection_type = DialogConfigs.FILE_SELECT;

        //Method handle selected files.
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                results = new Uri[files.length];
                for (int i = 0; i < files.length; i++) {
                    String filePath = new File(files[i]).getAbsolutePath();
                    if (!filePath.startsWith("file://")) {
                        filePath = "file://" + filePath;
                    }
                    results[i] = Uri.parse(filePath);
                    Log.d(TAG, "file path: " + filePath);
                    Log.d(TAG, "file uri: " + String.valueOf(results[i]));
                }
                mUploadMessage.onReceiveValue(results);
                mUploadMessage = null;
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (null != mUploadMessage) {
                    if (null != results && results.length >= 1) {
                        mUploadMessage.onReceiveValue(results);
                    } else {
                        mUploadMessage.onReceiveValue(null);
                    }
                }
                mUploadMessage = null;
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (null != mUploadMessage) {
                    if (null != results && results.length >= 1) {
                        mUploadMessage.onReceiveValue(results);
                    } else {
                        mUploadMessage.onReceiveValue(null);
                    }
                }
                mUploadMessage = null;
            }
        });
        dialog.show();
    }

    // Create an image file
    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
//            if (null == mUploadMessage)
//                return;
//            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
//            // Use RESULT_OK only if you're implementing WebView inside an Activity
//            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null :
//                    intent.getData();
//            mUploadMessage.onReceiveValue(result);
//            mUploadMessage = null;
        } else {
            Toast.makeText(MainActivity.this, "Failed to Upload Image", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case FilePickerDialog.EXTERNAL_READ_PERMISSION_GRANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (dialog != null) {
                        openFileSelectionDialog();//onRequestPermissionsResult
                    }
                } else {
                    //Permission has not been granted. Notify the user.
                    Toast.makeText(this, "Permission is Required for getting list of files", Toast.LENGTH_SHORT).show();
                }
            }
            case 200:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(hasCameraPermission()){
                        setWebViewContent();
                    }else{
                        EasyPermissions.requestPermissions(this,
                                "This app needs access to your camera so you can take pictures.",
                                REQUEST_CAMERA_PERMISSION,PERM_CAMERA);
                    }
                }
            }
        }
    }

    private boolean hasCameraPermission() {
        return EasyPermissions.hasPermissions(MainActivity.this, PERM_CAMERA);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(getApplicationContext(), "Failed loading app!", Toast.LENGTH_SHORT).show();
        }
    }

    private void dialogExitApp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to close this application ?");
        builder.setTitle("close application");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        });
        builder.setCancelable(false).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    @Override
    public void onBackPressed() {
        if (webview1.canGoBack()) {
            webview1.goBack();
        } else {
            //super.onBackPressed();
            dialogExitApp();
        }
        /**/
        /*if (webview1.canGoBack()) {
            webview1.goBack();
            return;
        }
        super.onBackPressed();*/
    }

    public void goBack() {
        if (webview1 != null && webview1.canGoBack()) {
            webview1.goBack();
        }
    }

    public void goForward() {
        if (webview1 != null && webview1.canGoForward()) {
            webview1.goForward();
        }
    }
}