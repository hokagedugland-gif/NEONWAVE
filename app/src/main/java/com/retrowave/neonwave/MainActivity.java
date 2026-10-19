package com.retrowave.neonwave;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Native Android bridge: only a loopback HTTP origin loads this WebView. */
public final class MainActivity extends Activity {
    private static final int REQUEST_AUDIO = 11, REQUEST_FILES = 12;
    private WebView view;
    private LocalServer server;
    private ValueCallback<Uri[]> pendingFiles;
    private long lastPlayChange = 0;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF0B0A18);
        getWindow().setNavigationBarColor(0xFF0B0A18);
        view = new WebView(this);
        view.setBackgroundColor(0xFF0B0A18);
        setContentView(view);
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView web, String url) {
                return !url.startsWith("http://127.0.0.1:");
            }
        });
        view.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingFiles != null) pendingFiles.onReceiveValue(null);
                pendingFiles = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.setType("audio/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(intent, REQUEST_FILES);
                    return true;
                } catch (Exception ex) {
                    pendingFiles = null;
                    return false;
                }
            }
        });
        view.addJavascriptInterface(new Object() {
            @JavascriptInterface public void requestAudioPermission() { runOnUiThread(MainActivity.this::requestAudioPermission); }
            @JavascriptInterface public void playing(boolean active, String title, String artist) {
                // Keep a foreground media notification when the WebView plays music.
                // This does not turn WebView playback into a native audio engine.
                runOnUiThread(() -> {
                    long now=System.currentTimeMillis();
                    if (now-lastPlayChange<150 && !active) return;
                    lastPlayChange=now;
                    Intent intent=new Intent(MainActivity.this,MusicService.class);
                    intent.setAction(active?MusicService.PLAY:MusicService.STOP);
                    intent.putExtra("title",title); intent.putExtra("artist",artist);
                    try { if (active) startForegroundService(intent); else stopService(intent); }
                    catch (Exception e) { android.util.Log.w("NEONWAVE","Service start skipped",e); }
                });
            }
        },"NativeHost");
        try {
            server = new LocalServer(this);
            server.start();
            view.loadUrl(server.url());
        } catch (Exception e) {
            view.loadData("<h2>NEON/WAVE startup failed</h2><pre>" + e.getMessage() + "</pre>","text/html","UTF-8");
        }
        requestAudioPermission();
    }
    private String permission() {
        return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
    }
    private void requestAudioPermission() {
        if (checkSelfPermission(permission()) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{ permission() }, REQUEST_AUDIO);
        else if(view!=null)view.evaluateJavascript("window.onAndroidPermissionResult&&window.onAndroidPermissionResult()",null);
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==REQUEST_AUDIO&&view!=null)view.evaluateJavascript("window.onAndroidPermissionResult&&window.onAndroidPermissionResult()",null);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQUEST_FILES||pendingFiles==null)return;
        Uri[] uris=null;
        if(resultCode==RESULT_OK&&data!=null){
            if(data.getClipData()!=null){int n=data.getClipData().getItemCount();uris=new Uri[n];for(int i=0;i<n;i++)uris[i]=data.getClipData().getItemAt(i).getUri();}
            else if(data.getData()!=null)uris=new Uri[]{ data.getData() };
        }
        pendingFiles.onReceiveValue(uris); pendingFiles=null;
    }
    @Override public void onBackPressed() {
        // Avoid accidentally closing music playback when navigating between panels.
        if(view!=null&&view.canGoBack()) view.goBack(); else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (view != null){view.loadUrl("about:blank");view.destroy();view=null;}
        if (server != null)server.close();
        try {stopService(new Intent(this, MusicService.class));}catch(Exception ignored){}
        super.onDestroy();
    }
}
