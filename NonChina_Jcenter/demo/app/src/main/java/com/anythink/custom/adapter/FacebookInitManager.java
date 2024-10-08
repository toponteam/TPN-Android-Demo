package com.anythink.custom.adapter;

import android.content.Context;

import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.MediationInitCallback;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.BuildConfig;

import java.util.Map;

public class FacebookInitManager extends ATInitMediation {

    private boolean mIsInit;
    private volatile static FacebookInitManager sInstance;

    private FacebookInitManager() {

    }

    public static FacebookInitManager getInstance() {
        if (sInstance == null) {
            synchronized (FacebookInitManager.class) {
                if (sInstance == null)
                    sInstance = new FacebookInitManager();
            }
        }
        return sInstance;
    }

    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras) {
        initSDK(context, serviceExtras, null);
    }

    @Override
    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras, MediationInitCallback mediationInitCallback) {
        try {
            if (!mIsInit) {
                AudienceNetworkAds.initialize(context.getApplicationContext());
                mIsInit = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNetworkName() {
        return "Custom Facebook";
    }


    public String getNetworkVersion() {
        try {
            return BuildConfig.VERSION_NAME;
        } catch (Throwable e) {

        }
        return "";
    }

}
