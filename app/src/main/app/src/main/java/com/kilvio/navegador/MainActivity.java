package com.kilvio.navegador;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private FrameLayout cursorView;
    private View cursorDot;
    private LinearLayout trackpadContainer;
    private FrameLayout trackpad;
    private ProgressBar progressBar;
    private LinearLayout topBar;
    private HorizontalScrollView bookmarksScroll;
    private Button btnExitFullscreen;
    private Button btnDragMode;

    private boolean isDesktopMode = true;
    private boolean isMouseVisible = true;
    private boolean isDraggingHold = false;

    private float cursorX = 350f;
    private float cursorY = 550f;
    private float lastTouchX, lastTouchY;
    private float padRawX, padRawY;
    private long touchStartTime;
    private long dragDownTime;

    private static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        cursorView = findViewById(R.id.cursorView);
        cursorDot = findViewById(R.id.cursorDot);
        trackpadContainer = findViewById(R.id.trackpadContainer);
        trackpad = findViewById(R.id.trackpad);
        progressBar = findViewById(R.id.progressBar);
        topBar = findViewById(R.id.topBar);
        bookmarksScroll = findViewById(R.id.bookmarksScroll);
        btnExitFullscreen = findViewById(R.id.btnExitFullscreen);
        btnDragMode = findViewById(R.id.btnDragMode);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnReload = findViewById(R.id.btnReload);
        Button btnDesktop = findViewById(R.id.btnDesktop);
        Button btnMouseToggle = findViewById(R.id.btnMouseToggle);
        Button btnFullscreen = findViewById(R.id.btnFullscreen);
        Button btnScrollUp = findViewById(R.id.btnScrollUp);
        Button btnScrollDown = findViewById(R.id.btnScrollDown);
        TextView btnMovePad = findViewById(R.id.btnMovePad);

        applyModernStyles();
        setupBookmarks();
        setupWebView();

        btnBack.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        btnForward.setOnClickListener(v -> { if (webView.canGoForward()) webView.goForward(); });
        btnReload.setOnClickListener(v -> webView.reload());

        // Alternar modo PC / Móvil
        btnDesktop.setOnClickListener(v -> {
            isDesktopMode = !isDesktopMode;
            webView.getSettings().setUserAgentString(isDesktopMode ? DESKTOP_UA : MOBILE_UA);
            webView.getSettings().setUseWideViewPort(isDesktopMode);
            webView.getSettings().setLoadWithOverviewMode(isDesktopMode);
            btnDesktop.setTextColor(isDesktopMode ? 0xFF00E676 : 0xFF64748B);
            webView.reload();
        });

        // Ocultar / Mostrar Trackpad y Ratón Virtual
        btnMouseToggle.setOnClickListener(v -> {
            isMouseVisible = !isMouseVisible;
            int vis = isMouseVisible ? View.VISIBLE : View.GONE;
            trackpadContainer.setVisibility(vis);
            cursorView.setVisibility(vis);
            btnMouseToggle.setTextColor(isMouseVisible ? 0xFF00E676 : 0xFF64748B);
        });

        // Modo Pantalla Completa Inmersivo
        btnFullscreen.setOnClickListener(v -> {
            topBar.setVisibility(View.GONE);
            bookmarksScroll.setVisibility(View.GONE);
            btnExitFullscreen.setVisibility(View.VISIBLE);
        });

        btnExitFullscreen.setOnClickListener(v -> {
            topBar.setVisibility(View.VISIBLE);
            bookmarksScroll.setVisibility(View.VISIBLE);
            btnExitFullscreen.setVisibility(View.GONE);
        });

        // Rueda de Scroll Virtual Arriba / Abajo
        btnScrollUp.setOnClickListener(v -> webView.scrollBy(0, -350));
        btnScrollDown.setOnClickListener(v -> webView.scrollBy(0, 350));

        // Modo Arrastrar (Hold) para mover gráficos o seleccionar
        btnDragMode.setOnClickListener(v -> toggleDragHold());

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrl(urlInput.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        setupTrackpad(btnMovePad);
        loadUrl("https://www.google.com");
    }

    private void applyModernStyles() {
        // Barra URL redondeada tipo píldora
        GradientDrawable urlShape = new GradientDrawable();
        urlShape.setColor(0xFF222738);
        urlShape.setCornerRadius(40f);
        urlShape.setStroke(2, 0xFF334155);
        urlInput.setBackground(urlShape);

        // Estilo del puntero (Anillo de precisión + punto central)
        GradientDrawable ringShape = new GradientDrawable();
        ringShape.setShape(GradientDrawable.OVAL);
        ringShape.setColor(0x2200E676);
        ringShape.setStroke(3, 0xFF00E676);
        cursorView.setBackground(ringShape);

        GradientDrawable dotShape = new GradientDrawable();
        dotShape.setShape(GradientDrawable.OVAL);
        dotShape.setColor(0xFFFFFFFF);
        cursorDot.setBackground(dotShape);

        // Estilo del Trackpad Pro (Cristal oscuro con borde neón)
        GradientDrawable padShape = new GradientDrawable();
        padShape.setColor(0xCC161925);
        padShape.setCornerRadius(28f);
        padShape.setStroke(2, 0x9900E676);
        trackpadContainer.setBackground(padShape);

        // Botón flotante para salir de pantalla completa
        GradientDrawable exitShape = new GradientDrawable();
        exitShape.setShape(GradientDrawable.OVAL);
        exitShape.setColor(0xCCEF4444);
        btnExitFullscreen.setBackground(exitShape);
    }

    private void setupBookmarks() {
        int[] ids = {R.id.bmGoogle, R.id.bmTradingView, R.id.bmGoCharting, R.id.bmYouTube};
        String[] urls = {
            "https://www.google.com",
            "https://es.tradingview.com/chart/",
            "https://gocharting.com/terminal",
            "https://m.youtube.com"
        };

        for (int i = 0; i < ids.length; i++) {
            Button b = findViewById(ids[i]);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(0xFF1E2436);
            pill.setCornerRadius(25f);
            pill.setStroke(1, 0xFF334155);
            b.setBackground(pill);

            final String targetUrl = urls[i];
            b.setOnClickListener(v -> loadUrl(targetUrl));
        }
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

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                urlInput.setText(url);
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
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

    private void setupTrackpad(TextView btnMovePad) {
        // Mover el cursor por la pantalla y hacer clic o arrastrar
        trackpad.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    touchStartTime = SystemClock.uptimeMillis();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = (event.getX() - lastTouchX) * 1.9f;
                    float dy = (event.getY() - lastTouchY) * 1.9f;

                    RelativeLayout root = findViewById(R.id.rootLayout);
                    cursorX = Math.max(0, Math.min(cursorX + dx, root.getWidth() - cursorView.getWidth()));
                    cursorY = Math.max(webView.getTop(), Math.min(cursorY + dy, root.getHeight() - cursorView.getHeight()));

                    cursorView.setX(cursorX);
                    cursorView.setY(cursorY);

                    if (isDraggingHold) {
                        dispatchDragMove();
                    }

                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    return true;

                case MotionEvent.ACTION_UP:
                    if (SystemClock.uptimeMillis() - touchStartTime < 200) {
                        if (isDraggingHold) {
                            toggleDragHold(); // Suelta el clic mantenido al tocar
                        } else {
                            performVirtualClick();
                        }
                    }
                    return true;
            }
            return false;
        });

        // Permitir cambiar de lugar la caja del Trackpad arrastrando desde "⠿ MOVER"
        btnMovePad.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    padRawX = event.getRawX();
                    padRawY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - padRawX;
                    float dy = event.getRawY() - padRawY;
                    trackpadContainer.setX(trackpadContainer.getX() + dx);
                    trackpadContainer.setY(trackpadContainer.getY() + dy);
                    padRawX = event.getRawX();
                    padRawY = event.getRawY();
                    return true;
            }
            return false;
        });
    }

    private void toggleDragHold() {
        isDraggingHold = !isDraggingHold;
        float webX = cursorX + (cursorView.getWidth() / 2f);
        float webY = cursorY - webView.getTop() + (cursorView.getHeight() / 2f);

        if (isDraggingHold) {
            btnDragMode.setTextColor(0xFF00E676);
            dragDownTime = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(dragDownTime, dragDownTime, MotionEvent.ACTION_DOWN, webX, webY, 0);
            webView.dispatchTouchEvent(down);
            down.recycle();
        } else {
            btnDragMode.setTextColor(0xFF94A3B8);
            long now = SystemClock.uptimeMillis();
            MotionEvent up = MotionEvent.obtain(dragDownTime, now, MotionEvent.ACTION_UP, webX, webY, 0);
            webView.dispatchTouchEvent(up);
            up.recycle();
        }
    }

    private void dispatchDragMove() {
        float webX = cursorX + (cursorView.getWidth() / 2f);
        float webY = cursorY - webView.getTop() + (cursorView.getHeight() / 2f);
        long now = SystemClock.uptimeMillis();
        MotionEvent move = MotionEvent.obtain(dragDownTime, now, MotionEvent.ACTION_MOVE, webX, webY, 0);
        webView.dispatchTouchEvent(move);
        move.recycle();
    }

    private void performVirtualClick() {
        // Efecto visual al hacer clic
        cursorView.animate().scaleX(0.75f).scaleY(0.75f).setDuration(70).withEndAction(() ->
            cursorView.animate().scaleX(1f).scaleY(1f).setDuration(70).start()
        ).start();

        float webX = cursorX + (cursorView.getWidth() / 2f);
        float webY = cursorY - webView.getTop() + (cursorView.getHeight() / 2f);

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 45;

        MotionEvent downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, webX, webY, 0);
        MotionEvent upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, webX, webY, 0);

        webView.dispatchTouchEvent(downEvent);
        webView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        upEvent.recycle();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
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
