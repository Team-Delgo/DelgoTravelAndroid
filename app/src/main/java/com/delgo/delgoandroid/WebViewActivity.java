package com.delgo.delgoandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class WebViewActivity extends AppCompatActivity {
    // private final long TIME = 0;
    private WebView webView = null;
    public ValueCallback<Uri> filePathCallbackNormal;
    public ValueCallback<Uri[]> filePathCallbackLollipop;
    public final static int FILE_CHOOSER_NORMAL_REQ_CODE = 2001;
    public final static int FILE_CHOOSER_LOLLIPOP_REQ_CODE = 2002;
    private Uri CAMERA_IMAGE_URI = null;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = (WebView) findViewById(R.id.webView);

        checkVerify();

        webView.setWebViewClient(new WebClient());
        webView.setWebChromeClient(new WebChromeClient());

        webView.getSettings().setJavaScriptEnabled(true);

        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setBuiltInZoomControls(false);

        webView.getSettings().setAllowFileAccessFromFileURLs(true);


        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);

        webView.addJavascriptInterface(new WebBridge(), "BRIDGE");

//        if(Build.VERSION.SDK_INT >= 19) {
//            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
//        } else {
//            webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
//        }
//        getWindow().setFlags(
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
//                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
//
//        if(Build.VERSION.SDK_INT >= 21) {
//            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
//        }

        webView.loadUrl("http://49.50.161.156:8080");
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkVerify() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //카메라 또는 저장공간 권한 획득 여부 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                new AlertDialog.Builder(this).setTitle("알림").setMessage("앱 이용을 위해선 권한 허용이 필요합니다.")
                        .setPositiveButton("종료", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        }).setNegativeButton("권한 설정", (dialog, which) -> {
                            dialog.dismiss();
                            Intent intent= new Intent(Settings.ACTION_SETTINGS);
                            startActivityForResult(intent,0);
                        }).setCancelable(false).show();
            } else {
                // 카메라 및 저장공간 권한 요청
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    //권한 획득 여부에 따른 결과 반환
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        // 카메라, 저장소 중 하나라도 거부한다면 앱실행 불가 메세지 띄움
                        new AlertDialog.Builder(this).setTitle("알림").setMessage("앱 이용을 위해선 권한 허용이 필요합니다.")
                                .setPositiveButton("종료", (dialog, which) -> {
                                    dialog.dismiss();
                                    finish();
                                }).setNegativeButton("권한 설정", (dialog, which) -> {
                                    dialog.dismiss();
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                    getApplicationContext().startActivity(intent);
                                }).setCancelable(false).show();

                        return;
                    }
                }

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_CHOOSER_NORMAL_REQ_CODE:
                if (resultCode == RESULT_OK) {
                    if (filePathCallbackNormal == null) return;
                    Uri result = data == null ? null : data.getData();
                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                }
                break;
            case FILE_CHOOSER_LOLLIPOP_REQ_CODE:
                if (resultCode == RESULT_OK) {
                    if (filePathCallbackLollipop == null) return;
                    if (data == null)
                        data = new Intent();
                    if (data.getData() == null)
                        data.setData(CAMERA_IMAGE_URI);

                    filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    filePathCallbackLollipop = null;
                } else {
                    if (filePathCallbackLollipop != null) {

                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }

                    if (filePathCallbackNormal != null) {
                        filePathCallbackNormal.onReceiveValue(null);
                        filePathCallbackNormal = null;
                    }
                }
                break;
            default:

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

//    @SuppressLint("IntentReset")
//    private void runCamera(boolean _isCapture) {
//        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//
//        File path = Environment.getExternalStorageDirectory();
//        File file = new File(path, "cam.png");
//        // File 객체의 URI 를 얻는다.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            String strpa = getApplicationContext().getPackageName();
//            cameraImageUri = FileProvider.getUriForFile(this, strpa + ".fileprovider", file);
//        } else {
//            cameraImageUri = Uri.fromFile(file);
//        }
//        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
//
//        if (!_isCapture) { // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때
//
//            Intent pickIntent = new Intent(Intent.ACTION_PICK);
//            pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
//            pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//
//            String pickTitle = "사진 가져오기";
//            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
//
//            // 카메라 intent 포함시키기..
//            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
//            startActivityForResult(chooserIntent, FILE_CHOOSER_LOLLIPOP_REQ_CODE);
//        } else {// 바로 카메라 실행..
//            startActivityForResult(intentCamera, FILE_CHOOSER_LOLLIPOP_REQ_CODE);
//        }
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

//    private class WebChromeClientClass extends WebChromeClient {
//        // 자바스크립트의 alert창
//        @Override
//        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
//            new AlertDialog.Builder(view.getContext())
//                    .setTitle("Alert")
//                    .setMessage(message)
//                    .setPositiveButton(android.R.string.ok,
//                            (dialog, which) -> result.confirm())
//                    .setCancelable(false)
//                    .create()
//                    .show();
//            return true;
//        }
//
//        // 자바스크립트의 confirm창
//        @Override
//        public boolean onJsConfirm(WebView view, String url, String message,
//                                   final JsResult result) {
//            new AlertDialog.Builder(view.getContext())
//                    .setTitle("Confirm")
//                    .setMessage(message)
//                    .setPositiveButton("Yes",
//                            (dialog, which) -> result.confirm())
//                    .setNegativeButton("No",
//                            (dialog, which) -> result.cancel())
//                    .setCancelable(false)
//                    .create()
//                    .show();
//            return true;
//        }
//
//        public boolean onShowFileChooser(
//                WebView webView, ValueCallback<Uri[]> filePathCallback,
//                FileChooserParams fileChooserParams) {
//
//            if (filePathCallbackLollipop != null) {
//                filePathCallbackLollipop.onReceiveValue(null);
//                filePathCallbackLollipop = null;
//            }
//            filePathCallbackLollipop = filePathCallback;
//
//            boolean isCapture = fileChooserParams.isCaptureEnabled();
//
//            runCamera(isCapture);
//            return true;
//        }
//    }

    class WebBridge {
        @JavascriptInterface
        public void testAndroid() {
            // 실행할 내용
        }
    }

    class WebClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                findViewById(R.id.loading).setVisibility(View.GONE);
                findViewById(R.id.webView).setVisibility(View.VISIBLE);
            }, 500);
        }
    }

}