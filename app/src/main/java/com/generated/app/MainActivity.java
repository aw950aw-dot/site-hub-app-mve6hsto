package com.generated.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static class SiteItem {
        String title;
        String url;
        WebView webView;
        boolean isLoaded = false;

        SiteItem(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    private final List<SiteItem> sites = new ArrayList<>();
    private int currentSiteIndex = -1;

    private LinearLayout rootLayout;
    private LinearLayout topBarLayout;
    private HorizontalScrollView tabsScroll;
    private LinearLayout tabsContainer;
    private FrameLayout webContainer;
    private LinearLayout homeLayout;
    private ProgressBar progressBar;
    private EditText searchInput;

    private float lastTouchY = 0;
    private boolean isTopBarHidden = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Intent serviceIntent = new Intent(this, BrowserBackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initializeSites();
        buildUI();
        handleIntent(getIntent());

        if (currentSiteIndex == -1) {
            showHomeDashboard();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void initializeSites() {
        sites.add(new SiteItem("מתמחים טופ", "https://mitmachim.top/"));
        sites.add(new SiteItem("OgenPlay", "https://ogenplay.com/"));
        sites.add(new SiteItem("App Factory", "https://app-factory-nt5l.onrender.com/"));
        sites.add(new SiteItem("GitHub", "https://github.com/"));
        sites.add(new SiteItem("JTech Forums", "https://jtechforums.org"));
        sites.add(new SiteItem("Nigun Pages", "https://nigun.pages.dev"));
        sites.add(new SiteItem("חיפוש Google", "https://www.google.com"));
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String url = data.toString();
                openUrlInActiveTab(url);
            }
        }
    }

    private void buildUI() {
        rootLayout = new LinearLayout(this);
        rootLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(0xFFF8F9FA);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        topBarLayout = new LinearLayout(this);
        topBarLayout.setOrientation(LinearLayout.VERTICAL);
        topBarLayout.setBackgroundColor(0xFFFFFFFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topBarLayout.setElevation(4f);
        }
        topBarLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        searchRow.setPadding(24, 20, 24, 12);

        TextView homeBtn = new TextView(this);
        homeBtn.setText("🏠");
        homeBtn.setTextSize(20);
        homeBtn.setPadding(12, 12, 24, 12);
        homeBtn.setOnClickListener(v -> showHomeDashboard());
        searchRow.addView(homeBtn);

        searchInput = new EditText(this);
        searchInput.setHint(R.string.search_hint);
        searchInput.setTextSize(15);
        searchInput.setSingleLine(true);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setBackgroundResource(R.drawable.bg_search);
        searchInput.setPadding(36, 20, 36, 20);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        searchInput.setLayoutParams(searchParams);
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    loadSearchOrUrl(query);
                }
                hideKeyboard();
                return true;
            }
            return false;
        });
        searchRow.addView(searchInput);

        TextView refreshBtn = new TextView(this);
        refreshBtn.setText("🔄");
        refreshBtn.setTextSize(18);
        refreshBtn.setPadding(24, 12, 12, 12);
        refreshBtn.setOnClickListener(v -> {
            if (currentSiteIndex >= 0 && currentSiteIndex < sites.size()) {
                WebView wv = sites.get(currentSiteIndex).webView;
                if (wv != null) wv.reload();
            }
        });
        searchRow.addView(refreshBtn);

        topBarLayout.addView(searchRow);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 6));
        topBarLayout.addView(progressBar);

        tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        tabsScroll.setPadding(16, 8, 16, 12);
        tabsScroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        tabsContainer = new LinearLayout(this);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsScroll.addView(tabsContainer);

        topBarLayout.addView(tabsScroll);
        rootLayout.addView(topBarLayout);

        webContainer = new FrameLayout(this);
        webContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f));
        rootLayout.addView(webContainer);

        buildHomeDashboard();

        for (int i = 0; i < sites.size(); i++) {
            createWebViewForSite(i);
        }

        updateTabsUI();

        setContentView(rootLayout);
    }

    private void buildHomeDashboard() {
        homeLayout = new LinearLayout(this);
        homeLayout.setOrientation(LinearLayout.VERTICAL);
        homeLayout.setPadding(32, 32, 32, 32);
        homeLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(R.string.home_title);
        title.setTextSize(22);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xFF202124);
        title.setPadding(0, 16, 0, 32);
        homeLayout.addView(title);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < sites.size(); i++) {
            final int index = i;
            SiteItem site = sites.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(28, 24, 28, 24);
            
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);

            TextView iconText = new TextView(this);
            iconText.setText("🌐");
            iconText.setTextSize(24);
            iconText.setPadding(0, 0, 24, 0);
            card.addView(iconText);

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            textLayout.setLayoutParams(textParams);

            TextView nameView = new TextView(this);
            nameView.setText(site.title);
            nameView.setTextSize(18);
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            nameView.setTextColor(0xFF1976D2);
            textLayout.addView(nameView);

            TextView urlView = new TextView(this);
            urlView.setText(site.url);
            urlView.setTextSize(13);
            urlView.setTextColor(0xFF5F6368);
            textLayout.addView(urlView);

            card.addView(textLayout);

            card.setOnClickListener(v -> switchToSite(index));
            grid.addView(card);
        }

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        scroll.addView(grid);

        homeLayout.addView(scroll);
        webContainer.addView(homeLayout);
    }

    private void createWebViewForSite(final int index) {
        final SiteItem site = sites.get(index);

        WebView wv = new WebView(this);
        WebSettings settings = wv.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(wv, true);
        }

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String url = request.getUrl().toString();
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (currentSiteIndex == index && searchInput != null) {
                    searchInput.setText(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                site.isLoaded = true;
                if (currentSiteIndex == index && searchInput != null) {
                    searchInput.setText(url);
                }
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (currentSiteIndex == index && progressBar != null) {
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        if (progressBar.getVisibility() == View.GONE) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                        progressBar.setProgress(newProgress);
                    }
                }
            }
        });

        wv.setOnLongClickListener(v -> {
            WebView.HitTestResult result = wv.getHitTestResult();
            if (result != null && (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                    result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                String linkUrl = result.getExtra();
                if (linkUrl != null && !linkUrl.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Copied Link", linkUrl);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, R.string.link_copied, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
            return false;
        });

        wv.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float currentY = event.getY();
                    float deltaY = currentY - lastTouchY;
                    if (deltaY < -25 && !isTopBarHidden) {
                        hideTopBar();
                    } else if (deltaY > 25 && isTopBarHidden) {
                        showTopBar();
                    }
                    break;
            }
            if (wv.getScrollY() == 0 && isTopBarHidden) {
                showTopBar();
            }
            return false;
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        wv.setLayoutParams(params);
        wv.setVisibility(View.GONE);

        site.webView = wv;
        webContainer.addView(wv);
    }

    private void switchToSite(int index) {
        if (index < 0 || index >= sites.size()) return;

        currentSiteIndex = index;
        if (homeLayout != null) {
            homeLayout.setVisibility(View.GONE);
        }

        for (int i = 0; i < sites.size(); i++) {
            WebView wv = sites.get(i).webView;
            if (wv != null) {
                if (i == index) {
                    wv.setVisibility(View.VISIBLE);
                    if (!sites.get(i).isLoaded) {
                        wv.loadUrl(sites.get(i).url);
                    }
                    if (searchInput != null) {
                        searchInput.setText(wv.getUrl() != null ? wv.getUrl() : sites.get(i).url);
                    }
                } else {
                    wv.setVisibility(View.GONE);
                }
            }
        }

        updateTabsUI();
        showTopBar();
    }

    private void showHomeDashboard() {
        currentSiteIndex = -1;
        if (homeLayout != null) {
            homeLayout.setVisibility(View.VISIBLE);
        }
        for (int i = 0; i < sites.size(); i++) {
            if (sites.get(i).webView != null) {
                sites.get(i).webView.setVisibility(View.GONE);
            }
        }
        if (searchInput != null) {
            searchInput.setText("");
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        updateTabsUI();
        showTopBar();
    }

    private void updateTabsUI() {
        if (tabsContainer == null) return;
        tabsContainer.removeAllViews();

        TextView homeTab = createTabButton("🏠 " + getString(R.string.menu_home), currentSiteIndex == -1);
        homeTab.setOnClickListener(v -> showHomeDashboard());
        tabsContainer.addView(homeTab);

        for (int i = 0; i < sites.size(); i++) {
            final int index = i;
            SiteItem site = sites.get(i);
            TextView tab = createTabButton(site.title, currentSiteIndex == index);
            tab.setOnClickListener(v -> switchToSite(index));
            tabsContainer.addView(tab);
        }
    }

    private TextView createTabButton(String title, boolean isSelected) {
        TextView tab = new TextView(this);
        tab.setText(title);
        tab.setTextSize(14);
        tab.setSingleLine(true);
        tab.setPadding(32, 16, 32, 16);
        tab.setGravity(Gravity.CENTER);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 0);
        tab.setLayoutParams(params);

        if (isSelected) {
            tab.setBackgroundResource(R.drawable.bg_tab_selected);
            tab.setTextColor(0xFF1976D2);
            tab.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(0xFF4A4A4A);
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        return tab;
    }

    private void loadSearchOrUrl(String query) {
        String url;
        if (query.startsWith("http://") || query.startsWith("https://")) {
            url = query;
        } else if (query.contains(".") && !query.contains(" ")) {
            url = "https://" + query;
        } else {
            url = "https://www.google.com/search?q=" + Uri.encode(query);
        }

        if (currentSiteIndex >= 0 && currentSiteIndex < sites.size()) {
            sites.get(currentSiteIndex).webView.loadUrl(url);
        } else {
            switchToSite(6);
            sites.get(6).webView.loadUrl(url);
        }
    }

    private void openUrlInActiveTab(String url) {
        for (int i = 0; i < sites.size(); i++) {
            if (url.contains(Uri.parse(sites.get(i).url).getHost())) {
                switchToSite(i);
                sites.get(i).webView.loadUrl(url);
                return;
            }
        }
        int target = currentSiteIndex >= 0 ? currentSiteIndex : 0;
        switchToSite(target);
        sites.get(target).webView.loadUrl(url);
    }

    private void hideTopBar() {
        if (!isTopBarHidden && topBarLayout != null) {
            isTopBarHidden = true;
            topBarLayout.animate().translationY(-topBarLayout.getHeight()).setDuration(200).start();
        }
    }

    private void showTopBar() {
        if (isTopBarHidden && topBarLayout != null) {
            isTopBarHidden = false;
            topBarLayout.animate().translationY(0).setDuration(200).start();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentSiteIndex >= 0 && currentSiteIndex < sites.size()) {
            WebView wv = sites.get(currentSiteIndex).webView;
            if (wv != null && wv.canGoBack()) {
                wv.goBack();
                return;
            }
        }
        if (currentSiteIndex != -1) {
            showHomeDashboard();
            return;
        }
        super.onBackPressed();
    }
}
