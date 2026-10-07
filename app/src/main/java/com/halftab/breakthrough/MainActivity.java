package com.halftab.breakthrough;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

/** Minimal offline WebView host for Break Through.
 *  No third-party runtime framework is required.
 */
public class MainActivity extends Activity {
    private WebView webView;
    private boolean pageLoaded = false;
    private boolean stateSent = false;
    private final NativeBridge nativeBridge = new NativeBridge();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setFitsSystemWindows(false);
        webView.setPadding(0, 0, 0, 0);
        webView.setLongClickable(false);
        webView.setOnLongClickListener(v -> true);
        webView.setHapticFeedbackEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);

        webView.addJavascriptInterface(nativeBridge, "BreakThroughNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                sendAppState(true);
            }
        });

        setContentView(webView);
        webView.loadUrl("file:///android_asset/www/index.html");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override public void onBackInvoked() {
                            handleBack();
                        }
                    });
        }
        enterImmersiveMode();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setAttributes(params);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        enterImmersiveMode();
        sendAppState(true);
    }

    @Override protected void onPause() {
        sendAppState(false);
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            handleBack();
        }
    }

    private void handleBack() {
        if (!pageLoaded || webView == null) return;
        webView.evaluateJavascript("window.BreakThroughNativeBack&&window.BreakThroughNativeBack();", null);
    }

    private void sendAppState(boolean active) {
        if (!pageLoaded || webView == null) return;
        if (stateSent == active) return;
        stateSent = active;
        String js = "window.BreakThroughNativeState&&window.BreakThroughNativeState(" + active + ");";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    private void enterImmersiveMode() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            hideSystemBarsLegacy();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBarsLegacy() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public final class NativeBridge {
        @JavascriptInterface public boolean isAndroid() { return true; }

        @JavascriptInterface public void haptic(String style) {
            try {
                long duration = "MEDIUM".equals(style) ? 25L : 12L;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    if (vm != null) vm.getDefaultVibrator().vibrate(
                            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (v != null && v.hasVibrator()) v.vibrate(duration);
                }
            } catch (Exception ignored) { }
        }

        @JavascriptInterface public void exitApp() {
            runOnUiThread(() -> {
                finishAndRemoveTask();
            });
        }
    }
}
