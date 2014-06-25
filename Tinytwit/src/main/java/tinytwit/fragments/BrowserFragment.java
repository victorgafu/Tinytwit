package tinytwit.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import tinytwit.activities.R;
import tinytwit.utilities.NetworkReceiver;

/**
 * Fragment of the webview created if in the tweet exists an url
 *
 * Created by Victor on 04/06/2014.
 */
public class BrowserFragment extends Fragment {

    // UI
    private WebView web;

    private NetworkReceiver receiver;
    private String url;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        url = getArguments().getString("url");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_browser, container, false);
        receiver = new NetworkReceiver(view.getContext());
        web = (WebView) view.findViewById(R.id.browser);

        // Prepare the WebView to accept the shortened url from the tweets.
        web.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }
        });
        loadURL();
        return view;

    }

    /**
     * Load the linked web at the tweet if there's any and if we have connection. If there's
     * not connection shows the default message
     *
     */
    public void loadURL() {
        // Show the web in the WebView if network is unavailable show offline web message
        if (receiver.checkSilentConnetion()) {
            web.loadUrl(url);
        } else {
            web.loadUrl("file:///android_asset/offline.html");
        }
    }

}

