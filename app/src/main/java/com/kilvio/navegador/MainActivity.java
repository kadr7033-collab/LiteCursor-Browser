package com.kilvio.navegador;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.graphics.drawable.GradientDrawable;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private View cursorView;
    private FrameLayout trackpad;
    private boolean isDesktopMode = true;

    private float cursorX = 300f;
    private float cursorY = 500f;
    private float lastTouchX, lastTouchY;
    private long touchStartTime;

    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        cursorView = findViewById(R.id.cursorView);
        trackpad = findViewById(R.id.trackpad);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnReload = findViewById(R.id.btnReload);
        Button btnDesktop = findViewById(R.id.btnDesktop);

        // Estilo circular para el puntero del ratón
        GradientDrawable cursorShape = new GradientDrawable();
        cursorShape.setShape(GradientDrawable.OVAL);
        cursorShape.setColor(0xCC00E676);
        cursorShape.setStroke(3, 0xFFFFFFFF);
        cursorView.setBackground(cursorShape);

        // Borde para el trackpad
        GradientDrawable padShape = new GradientDrawable();
        padShape.setColor(0x551E1E1E);
        padShape.setCornerRadius(24f);
        padShape.setStroke(2, 0x8800E676);
        trackpad.setBackground(padShape);

        setupWebView();

        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnReload.setOnClickListener(v -> webView.reload());

        btnDesktop.setOnClickListener(v -> {
            isDesktopMode = !isDesktopMode;
            webView.getSettings().setUserAgentString(isDesktopMode ? DESKTOP_UA : MOBILE_UA);
            webView.getSettings().setUseWideViewPort(isDesktopMode);
            webView.getSettings().setLoadWithOverviewMode(isDesktopMode);
            btnDesktop.setTextColor(isDesktopMode ? 0xFF00E676 : 0xFF888888);
            webView.reload();
        });

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(urlInput.getText().toString());
                return true;
            }
            return false;
        });

        setupTrackpad();
        loadUrl("https://www.google.com");
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setUserAgentString(DESKTOP_UA);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                urlInput.setText(url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadUrl(String input) {
        String url = input.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://www.google.com/search?q=" + url.replace(" ", "+");
            }
        }
        webView.loadUrl(url);
    }

    private void setupTrackpad() {
        trackpad.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    touchStartTime = SystemClock.uptimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = (event.getX() - lastTouchX) * 1.8f;
                    float dy = (event.getY() - lastTouchY) * 1.8f;

                    RelativeLayout root = findViewById(R.id.rootLayout);
                    cursorX = Math.max(0, Math.min(cursorX + dx, root.getWidth() - cursorView.getWidth()));
                    cursorY = Math.max(webView.getTop(), Math.min(cursorY + dy, root.getHeight() - cursorView.getHeight()));

                    cursorView.setX(cursorX);
                    cursorView.setY(cursorY);

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    return true;

                case MotionEvent.ACTION_UP:
                    if (SystemClock.uptimeMillis() - touchStartTime < 200) {
                        performVirtualClick();
                    }
                    return true;
            }
            return false;
        });
    }

    private void performVirtualClick() {
        float webX = cursorX + (cursorView.getWidth() / 2f);
        float webY = cursorY - webView.getTop() + (cursorView.getHeight() / 2f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 50;

        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, webX, webY, 0);
        MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, webX, webY, 0);

        webView.dispatchTouchEvent(downEvent);
        webView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        upEvent.recycle();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
