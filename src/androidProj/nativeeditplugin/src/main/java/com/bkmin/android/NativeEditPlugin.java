package com.bkmin.android;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.unity3d.player.UnityPlayer;

import org.json.JSONObject;

public class NativeEditPlugin {
    public static Activity unityActivity;
    public static ViewGroup rootView;
    private static RelativeLayout mainLayout;
    private static ViewGroup topViewGroup;
    private static String unityName = "";

    static final String LOG_TAG = "NativeEditPlugin";

    private static View getLeafView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); ++i) {
                View childView = vg.getChildAt(i);
                View result = getLeafView(childView);
                if (result != null)
                    return result;
            }
            return null;
        } else {
            Log.i(LOG_TAG, "Found leaf view");
            return view;
        }
    }

    @SuppressWarnings("unused")
    public static void InitPluginMsgHandler(final String _unityName) {
        unityActivity = UnityPlayer.currentActivity;
        unityName = _unityName;

        unityActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (mainLayout != null)
                    topViewGroup.removeView(mainLayout);

                rootView = unityActivity.findViewById(android.R.id.content);
                View topMostView = getLeafView(rootView);
                topViewGroup = (ViewGroup) topMostView.getParent();
                mainLayout = new RelativeLayout(unityActivity);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                topViewGroup.addView(mainLayout, rlp);

                rootView.setOnSystemUiVisibilityChangeListener
                        (new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int visibility) {
                                int systemUiVisibilitySettings =
                                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                                rootView.setSystemUiVisibility(systemUiVisibilitySettings);
                            }
                        });
            }
        });
    }

    @SuppressWarnings("unused")
    public static void ClosePluginMsgHandler() {
        unityActivity.runOnUiThread(new Runnable() {
            public void run() {
                topViewGroup.removeView(mainLayout);
            }
        });
    }

    public static void SendUnityMessage(JSONObject jsonMsg) {
        UnityPlayer.UnitySendMessage(unityName, "OnMsgFromPlugin", jsonMsg.toString());
    }

    @SuppressWarnings("unused")
    public static String SendUnityMsgToPlugin(final int nSenderId, final String jsonMsg) {
        final Runnable task = new Runnable() {
            public void run() {
                EditBox.processRecvJsonMsg(mainLayout, nSenderId, jsonMsg);
            }
        };
        unityActivity.runOnUiThread(task);
        return new JSONObject().toString();
    }

}
