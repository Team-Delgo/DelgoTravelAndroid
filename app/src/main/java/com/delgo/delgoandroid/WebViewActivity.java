package com.delgo.delgoandroid;

import static android.content.Intent.ACTION_VIEW;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.URISyntaxException;

public class WebViewActivity extends AppCompatActivity {
    private final static int FILECHOOSER_NORMAL_REQ_CODE = 0;
    private WebView webView = null;
    private ValueCallback mFilePathCallback;
    private PackageManager packageManager;
    private long backBtnTime = 0;
    private int vibrateTime = 100;
    private int vibrateAmplitude = 30;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = (WebView) findViewById(R.id.webView);

        checkVerify();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.setWebContentsDebuggingEnabled(true);
        webView.setWebViewClient(new WebClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(), "BRIDGE");

        webView.loadUrl("https://www.delgo.pet");

    }

    @Override
    public void onBackPressed() {
        long curTime = System.currentTimeMillis();
        long gapTime = curTime - backBtnTime;
        if (webView.canGoBack()) {
            webView.goBack();
        } else if (0 <= gapTime && 2000 >= gapTime) {
            super.onBackPressed();
        } else {
            backBtnTime = curTime;
            Toast.makeText(this, R.string.on_back_pressed, Toast.LENGTH_SHORT).show();
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            Toast.makeText(this, R.string.on_back_pressed, Toast.LENGTH_SHORT).show();
        }
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
                new AlertDialog.Builder(this).setTitle(R.string.alert).setMessage(R.string.allow_permission)
                        .setPositiveButton(R.string.terminate, (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        }).setNegativeButton(R.string.set_permission, (dialog, which) -> {
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                            startActivityForResult(intent, 0);
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
                        new AlertDialog.Builder(this).setTitle(R.string.alert).setMessage(R.string.request_permission)
                                .setPositiveButton(R.string.terminate, (dialog, which) -> {
                                    dialog.dismiss();
                                    finish();
                                }).setNegativeButton(R.string.set_permission, (dialog, which) -> {
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
        /* 파일 선택 완료 후 처리 */
        switch(requestCode) {
            case FILECHOOSER_NORMAL_REQ_CODE:
                //fileChooser 로 파일 선택 후 onActivityResult 에서 결과를 받아 처리함
                if(resultCode == RESULT_OK) {
                    //파일 선택 완료 했을 경우
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mFilePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    }else{
                        mFilePathCallback.onReceiveValue(new Uri[]{data.getData()});
                    }
                    mFilePathCallback = null;
                } else {
                    //cancel 했을 경우
                    if(mFilePathCallback != null) {
                        mFilePathCallback.onReceiveValue(null);
                        mFilePathCallback = null;
                    }
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void vibrate(){
            final Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

            if(Build.VERSION.SDK_INT >= 26){
                vibrator.vibrate(VibrationEffect.createOneShot(vibrateTime, vibrateAmplitude));
            }else{
                vibrator.vibrate(vibrateTime);
            }
        }
        @JavascriptInterface
        public void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(getString(R.string.delgo), text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.copied, Toast.LENGTH_SHORT).show();

        }
        @JavascriptInterface
        public void setNotify() {
            // Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS);
            Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
            startActivity(intent);
        }
        @JavascriptInterface
        public void goToPlusFriends(){
            Intent schemeIntent = null;
            try {
                Log.d("kakao", getString(R.string.url_plus_friend));
                schemeIntent = Intent.parseUri(getString(R.string.url_plus_friend), Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                Log.d("kakao", "here");
            }
            startActivity(schemeIntent);
        }
    }


    class WebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("tel:")) {
                Intent dial = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(dial);
                return true;
            }

            if (Uri.parse(url).getScheme().equals("https://")) {
                Log.d("kakao", url);
                Intent intent = null;
                try {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                try {
                    startActivity(new Intent(ACTION_VIEW, Uri.parse(url)));

                    return true;
                } catch (ActivityNotFoundException e) {
                    String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null) {
                        view.loadUrl(fallbackUrl);
                        return true;
                    }
                }
                try {
                    // 실행 가능한 앱이 있으면 앱 실행
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!URLUtil.isNetworkUrl(url) && !URLUtil.isJavaScriptUrl(url)) {
                Log.d("kakao", url);
                final Uri uri;
                try {
                    uri = Uri.parse(url);
                } catch (Exception e) {
                    return false;
                }

                if ("intent".equals(uri.getScheme())) {
                    return startSchemeIntentToss(url);
                } else {
                    try {
                        webView.loadUrl(url);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }

            return false;
        }



        private boolean startSchemeIntentToss(String url) {
            final Intent schemeIntent;
            try {
                schemeIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                return false;
            }

            try {
                startActivity(schemeIntent);
                return true;
            } catch (ActivityNotFoundException e) {
                final String packageName = schemeIntent.getPackage();

                if (!TextUtils.isEmpty(packageName)) {
                    startActivity(new Intent(ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                findViewById(R.id.loading).setVisibility(View.GONE);
                findViewById(R.id.webView).setVisibility(View.VISIBLE);
            }, 500);

        }
    }

    class WebChromeClient extends android.webkit.WebChromeClient {
        @Override
        public void onPermissionRequest(PermissionRequest request) {
            Log.d("bridge", "Permission request");
            Log.d("bridge", request.getResources().toString());
            request.grant(request.getResources());
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, android.webkit.WebChromeClient.FileChooserParams fileChooserParams) {
            /* 파일 업로드 */
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
            }
            mFilePathCallback = filePathCallback;

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 0);

            return true;
        }
    }

}