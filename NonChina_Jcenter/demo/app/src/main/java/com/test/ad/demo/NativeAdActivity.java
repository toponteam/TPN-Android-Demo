package com.test.ad.demo;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.AdError;
import com.anythink.nativead.api.ATNative;
import com.anythink.nativead.api.ATNativeAdView;
import com.anythink.nativead.api.ATNativeDislikeListener;
import com.anythink.nativead.api.ATNativeEventExListener;
import com.anythink.nativead.api.ATNativeNetworkListener;
import com.anythink.nativead.api.ATNativePrepareExInfo;
import com.anythink.nativead.api.ATNativePrepareInfo;
import com.anythink.nativead.api.ATNativeView;
import com.anythink.nativead.api.NativeAd;
import com.anythink.nativead.unitgroup.api.CustomNativeAd;
import com.test.ad.demo.base.BaseActivity;
import com.test.ad.demo.bean.CommonViewBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NativeAdActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = NativeAdActivity.class.getSimpleName();
    public static final String NATIVE_SELF_RENDER_TYPE = "1";
    public static final String NATIVE_EXPRESS_TYPE = "2";

    private final List<String> mData = new ArrayList<>();

    private ATNative mATNative;
    private NativeAd mNativeAd;

    private ATNativeView mATNativeView;
    private View mSelfRenderView;
    private TextView mTVLoadAdBtn;
    private TextView mTVIsAdReadyBtn;
    private TextView mTVShowAdBtn;
    private View mPanel;

    @Override
    protected int getContentViewId() {
        return R.layout.activity_native;
    }

    @Override
    protected int getAdType() {
        return ATAdConst.ATMixedFormatAdType.NATIVE;
    }

    @Override
    protected void onSelectPlacementId(String placementId) {
        initATNativeAd(placementId);
    }

    @Override
    protected CommonViewBean getCommonViewBean() {
        final CommonViewBean commonViewBean = new CommonViewBean();
        commonViewBean.setTitleBar(findViewById(R.id.title_bar));
        commonViewBean.setTvLogView(findViewById(R.id.tv_show_log));
        commonViewBean.setSpinnerSelectPlacement(findViewById(R.id.spinner_1));

        String nativeType = getNativeAdTypeFromIntent();
        if (nativeType.equals(NATIVE_SELF_RENDER_TYPE)) {
            commonViewBean.setTitleResId(R.string.anythink_native_self);
        } else {
            commonViewBean.setTitleResId(R.string.anythink_native_express);
        }
        return commonViewBean;
    }

    @Override
    protected String getNativeAdType() {
        return getNativeAdTypeFromIntent();
    }

    private String getNativeAdTypeFromIntent() {
        return getIntent().getStringExtra("native_type");
    }

    @Override
    protected void initView() {
        super.initView();
        mTVLoadAdBtn = findViewById(R.id.load_ad_btn);
        mTVIsAdReadyBtn = findViewById(R.id.is_ad_ready_btn);
        mTVShowAdBtn = findViewById(R.id.show_ad_btn);
        initPanel();
    }

    @Override
    protected void initListener() {
        super.initListener();
        mTVLoadAdBtn.setOnClickListener(this);
        mTVIsAdReadyBtn.setOnClickListener(this);
        mTVShowAdBtn.setOnClickListener(this);
    }

    private void initPanel() {
        mPanel = findViewById(R.id.rl_panel);
        mATNativeView = findViewById(R.id.native_ad_view);
        mSelfRenderView = findViewById(R.id.native_selfrender_view);
        RecyclerView rvButtonList = findViewById(R.id.rv_button);
        GridLayoutManager manager = new GridLayoutManager(this, 2);
        rvButtonList.setLayoutManager(manager);

        final boolean[] isMute = new boolean[]{true};
        NativeVideoButtonAdapter adapter = new NativeVideoButtonAdapter(mData, new NativeVideoButtonAdapter.OnNativeVideoButtonCallback() {
            @Override
            public void onClick(String action) {
                if (action.equals(VideoAction.VOICE_CHANGE)) {
                    if (mNativeAd != null) {
                        mNativeAd.setVideoMute(!isMute[0]);
                        isMute[0] = !isMute[0];
                    }
                } else if (action.equals(VideoAction.VIDEO_RESUME)) {
                    if (mNativeAd != null) {
                        mNativeAd.resumeVideo();
                    }
                } else if (action.equals(VideoAction.VIDEO_PAUSE)) {
                    if (mNativeAd != null) {
                        mNativeAd.pauseVideo();
                    }
                } else if (action.equals(VideoAction.VIDEO_PROGRESS)) {
                    if (mNativeAd != null) {
                        String tips = "video duration: " + mNativeAd.getVideoDuration() + ", progress: " + mNativeAd.getVideoProgress();
                        Log.i(TAG, tips);
                        Toast.makeText(NativeAdActivity.this, tips, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        rvButtonList.setAdapter(adapter);
    }

    private void initATNativeAd(String placementId) {
        mATNative = new ATNative(this, placementId, new ATNativeNetworkListener() {
            @Override
            public void onNativeAdLoaded() {
                Log.i(TAG, "onNativeAdLoaded");
                printLogOnUI("load success...");
            }

            @Override
            public void onNativeAdLoadFail(AdError adError) {
                Log.i(TAG, "onNativeAdLoadFail, " + adError.getFullErrorInfo());
                printLogOnUI("load fail...：" + adError.getFullErrorInfo());
            }
        });

        mATNative.setAdSourceStatusListener(new ATAdSourceStatusListenerImpl());
    }

    private void loadAd(int adViewWidth, int adViewHeight) {
        printLogOnUI(getString(R.string.anythink_ad_status_loading));

        Map<String, Object> localExtra = new HashMap<>();
        localExtra.put(ATAdConst.KEY.AD_WIDTH, adViewWidth);
        localExtra.put(ATAdConst.KEY.AD_HEIGHT, adViewHeight);

        mATNative.setLocalExtra(localExtra);
        mATNative.makeAdRequest();
    }

    private boolean isAdReady() {
        boolean isReady = mATNative.checkAdStatus().isReady();
        Log.i(TAG, "isAdReady: " + isReady);
        printLogOnUI("isAdReady：" + isReady);

        List<ATAdInfo> atAdInfoList = mATNative.checkValidAdCaches();
        Log.i(TAG, "Valid Cahce size:" + (atAdInfoList != null ? atAdInfoList.size() : 0));
        if (atAdInfoList != null) {
            for (ATAdInfo adInfo : atAdInfoList) {
                Log.i(TAG, "\nCahce detail:" + adInfo.toString());
            }
        }

        return isReady;
    }

    private void showAd() {
//        NativeAd nativeAd = mATNative.getNativeAd();
        NativeAd nativeAd = mATNative.getNativeAd(getATShowConfig());
        if (nativeAd != null) {

            if (mNativeAd != null) {
                mNativeAd.destory();
            }
            mNativeAd = nativeAd;

            mNativeAd.setAdRevenueListener(new AdRevenueListenerImpl());
            mNativeAd.setNativeEventListener(new ATNativeEventExListener() {
                @Override
                public void onDeeplinkCallback(ATNativeAdView view, ATAdInfo adInfo, boolean isSuccess) {
                    Log.i(TAG, "onDeeplinkCallback:" + adInfo.toString() + "--status:" + isSuccess);
                    printLogOnUI("onDeeplinkCallback");
                }

                @Override
                public void onAdImpressed(ATNativeAdView view, ATAdInfo entity) {
                    Log.i(TAG, "native ad onAdImpressed:\n" + entity.toString());
                    printLogOnUI("onAdImpressed");
                }

                @Override
                public void onAdClicked(ATNativeAdView view, ATAdInfo entity) {
                    Log.i(TAG, "native ad onAdClicked:\n" + entity.toString());
                    printLogOnUI("onAdClicked");
                }

                @Override
                public void onAdVideoStart(ATNativeAdView view) {
                    Log.i(TAG, "native ad onAdVideoStart");
                    printLogOnUI("onAdVideoStart");
                }

                @Override
                public void onAdVideoEnd(ATNativeAdView view) {
                    Log.i(TAG, "native ad onAdVideoEnd");
                    printLogOnUI("onAdVideoEnd");
                }

                @Override
                public void onAdVideoProgress(ATNativeAdView view, int progress) {
                    Log.i(TAG, "native ad onAdVideoProgress:" + progress);
                    printLogOnUI("onAdVideoProgress");
                }
            });

            mNativeAd.setDislikeCallbackListener(new ATNativeDislikeListener() {
                @Override
                public void onAdCloseButtonClick(ATNativeAdView view, ATAdInfo entity) {
                    Log.i(TAG, "native ad onAdCloseButtonClick");
                    printLogOnUI("native ad onAdCloseButtonClick");

                    exitNativePanel();
                }
            });

            mATNativeView.removeAllViews();

            ATNativePrepareInfo mNativePrepareInfo = null;

            try {
                mNativePrepareInfo = new ATNativePrepareExInfo();

                if (mNativeAd.isNativeExpress()) {
                    mNativeAd.renderAdContainer(mATNativeView, null);
                } else {
                    SelfRenderViewUtil.bindSelfRenderView(this, mNativeAd.getAdMaterial(), mSelfRenderView, mNativePrepareInfo);
                    mNativeAd.renderAdContainer(mATNativeView, mSelfRenderView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            mNativeAd.prepare(mATNativeView, mNativePrepareInfo);
            mATNativeView.setVisibility(View.VISIBLE);
            mPanel.setVisibility(View.VISIBLE);
            initPanelButtonList(mNativeAd.getAdMaterial().getAdType());
        } else {
            printLogOnUI("this placement no cache!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyAd();
        if (mATNative != null) {
            mATNative.setAdListener(null);
            mATNative.setAdSourceStatusListener(null);
            mATNative.setAdMultipleLoadedListener(null);
        }
    }

    private void destroyAd() {
        if (mNativeAd != null) {
            mNativeAd.destory();
        }
    }

    @Override
    protected void onPause() {
        if (mNativeAd != null) {
            mNativeAd.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mNativeAd != null) {
            mNativeAd.onResume();
        }
        super.onResume();
    }

    private void initPanelButtonList(String adType) {
        if (Objects.equals(adType, CustomNativeAd.NativeAdConst.VIDEO_TYPE)) {
            boolean isNativeExpress = true;
            if (mNativeAd != null) {
                isNativeExpress = mNativeAd.isNativeExpress();
            }

            if (isNativeExpress) {
                return;
            }

            ATAdInfo atAdInfo = mNativeAd.getAdInfo();
            int networkId = atAdInfo.getNetworkFirmId();

            switch (networkId) {
                case 8: //for GDT
                    mData.add(VideoAction.VOICE_CHANGE);
                    mData.add(VideoAction.VIDEO_RESUME);
                    mData.add(VideoAction.VIDEO_PAUSE);
                    mData.add(VideoAction.VIDEO_PROGRESS);
                    break;
//                case 15:
//                    //for CSJ
//                    mData.add(VideoAction.VOICE_CHANGE);
//                    break;
                case 22:    //for BaiDu
                case 28:    //for KuaiShou
                    mData.add(VideoAction.VIDEO_PROGRESS);
                    break;
                case 66: //for Adx
                case 67: //for Direct
                    mData.add(VideoAction.VOICE_CHANGE);
                    mData.add(VideoAction.VIDEO_RESUME);
                    mData.add(VideoAction.VIDEO_PAUSE);
                    mData.add(VideoAction.VIDEO_PROGRESS);
                    break;
            }
        }
    }

    private void exitNativePanel() {
        mData.clear();
        destroyAd();
        mPanel.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mPanel.getVisibility() == View.VISIBLE) {
            exitNativePanel();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (v == null) return;

        switch (v.getId()) {
            case R.id.load_ad_btn:
                final int adViewWidth = mATNativeView.getWidth() != 0 ? mATNativeView.getWidth() : getResources().getDisplayMetrics().widthPixels;
                final int adViewHeight = adViewWidth * 3 / 4;
                loadAd(adViewWidth, adViewHeight);
                break;
            case R.id.is_ad_ready_btn:
                isAdReady();
                break;
            case R.id.show_ad_btn:
                ATNative.entryAdScenario(mCurrentPlacementId, AdConst.SCENARIO_ID.NATIVE_AD_SCENARIO);
                if (isAdReady()) {
                    showAd();
                }
                break;
        }
    }

    private ATShowConfig getATShowConfig() {
        ATShowConfig.Builder builder = new ATShowConfig.Builder();
        builder.scenarioId(AdConst.SCENARIO_ID.NATIVE_AD_SCENARIO);
        builder.showCustomExt(AdConst.SHOW_CUSTOM_EXT.NATIVE_AD_SHOW_CUSTOM_EXT);

        return builder.build();
    }
}
