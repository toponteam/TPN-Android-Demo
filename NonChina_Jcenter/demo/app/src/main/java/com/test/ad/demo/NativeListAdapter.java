package com.test.ad.demo;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.AdError;
import com.anythink.nativead.api.ATNative;
import com.anythink.nativead.api.ATNativeAdView;
import com.anythink.nativead.api.ATNativeNetworkListener;
import com.anythink.nativead.api.ATNativeView;
import com.anythink.nativead.api.ATNativeDislikeListener;
import com.anythink.nativead.api.ATNativeEventListener;
import com.anythink.nativead.api.ATNativePrepareInfo;
import com.anythink.nativead.api.NativeAd;
import com.test.ad.demo.bean.RecycleViewDataBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NativeListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private String TAG = NativeListAdapter.class.getSimpleName();
    /**
     * data
     */
    private static final int TYPE_DATA = 0;
    /**
     * ad
     */
    private static final int TYPE_AD = 1;
    /**
     * load more data
     */
    private static final int TYPE_MORE = 2;


    private List<RecycleViewDataBean> mData;

    final int limitAdSize = 20;
    private List<RecycleViewDataBean> mNativeAdBeanList;

    private OnNativeListCallback mOnNativeListCallback;


    private ATNative mATNative;

    ConcurrentHashMap<String, NativeAd> mImpressionAdMap;


    public NativeListAdapter(List<RecycleViewDataBean> data, OnNativeListCallback onNativeListCallback) {
        this.mData = data;
        this.mImpressionAdMap = new ConcurrentHashMap<>();
        this.mOnNativeListCallback = onNativeListCallback;
        this.mNativeAdBeanList = new ArrayList<>(5);
    }

    public void setNativeAdHandler(ATNative nativeAdHandler) {
        mATNative = nativeAdHandler;
    }

    public void addData(List<RecycleViewDataBean> data) {
        if (data != null) {
            int oldSize = mData.size();
            int InsertSize = data.size();
            this.mData.addAll(data);

            this.notifyItemRangeChanged(oldSize, InsertSize);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int itemViewType) {
        switch (itemViewType) {
            case TYPE_AD:
                return onCreateAdViewHolder(viewGroup);

            case TYPE_MORE:
                return onCreateMoreViewHolder(viewGroup);

            case TYPE_DATA:
            default:
                return onCreateDataViewHolder(viewGroup);
        }
    }

    private AdViewHolder onCreateAdViewHolder(@NonNull ViewGroup viewGroup) {
        Log.i(TAG, "onCreateAdViewHolder: create adView");
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.native_list_ad_item, viewGroup, false);
        return new AdViewHolder(view);
    }

    private DataViewHolder onCreateDataViewHolder(@NonNull ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.native_list_data_item, viewGroup, false);
        return new DataViewHolder(view);
    }

    private MoreViewHolder onCreateMoreViewHolder(@NonNull ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.native_list_more_item, viewGroup, false);
        return new MoreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        int itemViewType = getItemViewType(position);

        switch (itemViewType) {
            case TYPE_AD:
                if (viewHolder instanceof AdViewHolder) {
                    Log.i(TAG, "onBindViewHolder:" + viewHolder.toString());
                    onBindAdViewHolder((AdViewHolder) viewHolder, position);
                }
                break;
            case TYPE_MORE:
                if (viewHolder instanceof MoreViewHolder) {
                    onBindMoreViewHoler((MoreViewHolder) viewHolder);
                }
                break;

            case TYPE_DATA:
            default:
                if (viewHolder instanceof DataViewHolder) {
                    onBindDataViewHolder((DataViewHolder) viewHolder, position);
                }
                break;
        }
    }

    /**
     * bind ad view,use nativeAd cache
     */
    private void onBindAdViewHolder(AdViewHolder viewHolder, int position) {
        Log.i(TAG, "onBindAdViewHolder");
        RecycleViewDataBean recycleViewDataBean = mData.get(position);

        //海外SDK处理
        if (!ATSDK.isCnSDK() && !recycleViewDataBean.isLoadingAd) {
            recycleViewDataBean.isLoadingAd = true;
            mATNative.setAdListener(new ATNativeNetworkListener() {
                @Override
                public void onNativeAdLoaded() {
                    viewHolder.mAdContainerRoot.setVisibility(View.VISIBLE);
                    recycleViewDataBean.isLoadingAd = false;
                    NativeAd nativeAd = mATNative.getNativeAd();
                    recycleViewDataBean.nativeAd = nativeAd;

                    bindNativeAdWithViewHolder(nativeAd, viewHolder, recycleViewDataBean, position);
                }

                @Override
                public void onNativeAdLoadFail(AdError error) {
                    viewHolder.mAdContainerRoot.setVisibility(View.GONE);
                    recycleViewDataBean.isLoadingAd = false;
                }
            });
            mATNative.makeAdRequest();
            return;
        }
        boolean hasUseNewAd = false;
        if (recycleViewDataBean.nativeAd == null) {
            recycleViewDataBean.nativeAd = mATNative.getNativeAd();
            hasUseNewAd = true;
        }
        if (hasUseNewAd) {
            Log.i(TAG, "start to request new ad object.");
            //It is judged that if a new Ad has been obtained or the advertisement cannot be obtained temporarily, the Ad will be loaded immediately
            mATNative.makeAdRequest();
        }

        //Controll the Ad Cache Size
        controlNativeAdCacheSize(recycleViewDataBean, hasUseNewAd);

        bindNativeAdWithViewHolder(recycleViewDataBean.nativeAd, viewHolder, recycleViewDataBean, position);
    }

    private void bindNativeAdWithViewHolder(NativeAd nativeAd, AdViewHolder viewHolder, RecycleViewDataBean recycleViewDataBean, int position) {
        if (nativeAd == null) {
            //Temporarily hide the item when the ad object is empty, and display it after the Ad is obtained
            Log.i(TAG, "onBindAdViewHolder: NativeAd is null, it would be gone now.");
            RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) viewHolder.itemView.getLayoutParams();
            viewHolder.itemView.setVisibility(View.GONE);
            param.height = 0;
            param.width = 0;
            viewHolder.itemView.setLayoutParams(param);
        } else {
            //Show Ad
            Log.i(TAG, "onBindAdViewHolder: NativeAd exist, start to render view.");
            Log.i(TAG, "onBindAdViewHolder: RenderAd: " + nativeAd.getAdInfo().toString());
            viewHolder.itemView.setVisibility(View.VISIBLE);
            RecyclerView.LayoutParams param = (RecyclerView.LayoutParams) viewHolder.itemView.getLayoutParams();
            param.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            param.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            viewHolder.itemView.setLayoutParams(param);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_HORIZONTAL;
            viewHolder.mATNativeView.setLayoutParams(params);

            viewHolder.setCurrentRecycleViewDataBean(recycleViewDataBean);
            mImpressionAdMap.put(String.valueOf(nativeAd.hashCode()), nativeAd);
            renderAdView(nativeAd, viewHolder, position);
        }
    }

    //Simple implementation to limit the specified number of advertisement caches in a List to avoid excessive memory
    private void controlNativeAdCacheSize(RecycleViewDataBean recycleViewDataBean, boolean hasUseNewAd) {
        if (hasUseNewAd) {
            if (recycleViewDataBean.nativeAd != null) {
                mNativeAdBeanList.add(recycleViewDataBean);
            }
        }

        if (mNativeAdBeanList.size() > limitAdSize) {
            RecycleViewDataBean removeBean = mNativeAdBeanList.get(0);
            if (removeBean.nativeAd != null) {
                //Remove the oldest Ad directly after the number is exceeded
                mNativeAdBeanList.remove(0);
                Log.i(TAG, "controlNativeAdCacheSize: Over Ad Size, Remove AD:" + removeBean.nativeAd.getAdInfo());
                removeBean.nativeAd.destory();
                removeBean.nativeAd = null;
            }
        }
    }

    private void renderAdView(final NativeAd nativeAd, final AdViewHolder adViewHolder, int position) {
        nativeAd.setNativeEventListener(new ATNativeEventListener() {
            @Override
            public void onAdImpressed(ATNativeAdView view, ATAdInfo entity) {
                Log.i(TAG, "native ad onAdImpressed--------\n" + entity.toString());
            }

            @Override
            public void onAdClicked(ATNativeAdView view, ATAdInfo entity) {
                Log.i(TAG, "native ad onAdClicked--------\n" + entity.toString());
            }

            @Override
            public void onAdVideoStart(ATNativeAdView view) {
                Log.i(TAG, "native ad onAdVideoStart--------");
            }

            @Override
            public void onAdVideoEnd(ATNativeAdView view) {
                Log.i(TAG, "native ad onAdVideoEnd--------");
            }

            @Override
            public void onAdVideoProgress(ATNativeAdView view, int progress) {
                Log.i(TAG, "native ad onAdVideoProgress--------:" + progress);
            }
        });

        nativeAd.setDislikeCallbackListener(new ATNativeDislikeListener() {
            @Override
            public void onAdCloseButtonClick(ATNativeAdView view, ATAdInfo entity) {
                Log.i(TAG, "onAdCloseButtonClick: remove " + position);
                removeAdView(position);
            }
        });

        try {
            Log.i(TAG, "native ad start to render ad------------- ");

            ATNativePrepareInfo nativePrepareInfo = null;
            adViewHolder.mATNativeView.removeAllViews();

            if (nativeAd.isNativeExpress()) {
                //Ad rendering for templates
                adViewHolder.mATNativeView.getLayoutParams().height = SelfRenderViewUtil.dip2px(adViewHolder.mATNativeView.getContext(), 300);
                nativeAd.renderAdContainer(adViewHolder.mATNativeView, null);
                adViewHolder.mSelfRenderView.setVisibility(View.GONE);
            } else {
                //Ad rendering for self-render
                adViewHolder.mATNativeView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                nativePrepareInfo = new ATNativePrepareInfo();
                adViewHolder.mSelfRenderView.setVisibility(View.VISIBLE);
//                SelfRenderViewUtil.bindSelfRenderView(this, nativeAd.getAdMaterial(), selfRenderView, nativePrepareInfo, adViewHeight);
                SelfRenderViewUtil.bindSelfRenderView(adViewHolder.mATNativeView.getContext(), nativeAd.getAdMaterial(), adViewHolder.mSelfRenderView, nativePrepareInfo);
                nativeAd.renderAdContainer(adViewHolder.mATNativeView, adViewHolder.mSelfRenderView);
            }

            nativeAd.prepare(adViewHolder.mATNativeView, nativePrepareInfo);
            nativeAd.onResume();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * bind mock data
     */
    private void onBindDataViewHolder(DataViewHolder viewHolder, int position) {
        viewHolder.mTvData.setText(mData.get(position).content);
    }

    private void onBindMoreViewHoler(MoreViewHolder viewHolder) {
        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnNativeListCallback != null) {
                    mOnNativeListCallback.onClickLoadMore();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() + 1 : 0;
    }

    @Override
    public int getItemViewType(int position) {

        if (position == mData.size()) {
            return TYPE_MORE;
        }

        RecycleViewDataBean recycleViewDataBean = mData.get(position);
        switch (recycleViewDataBean.dataType) {
            case RecycleViewDataBean.AD_DATA_TYPE:
                return TYPE_AD;
            case RecycleViewDataBean.NORMAL_DATA_TYPE:
            default:
                return TYPE_DATA;
        }

    }

    public void removeAdView(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
    }

    public void onResume() {
        Iterator<NativeAd> iterator = mImpressionAdMap.values().iterator();
        while (iterator.hasNext()) {
            NativeAd nativeAd = iterator.next();
            nativeAd.onResume();
            Log.i(TAG, "Ad View onResume:" + nativeAd.toString());
        }
    }

    public void onPause() {
        Iterator<NativeAd> iterator = mImpressionAdMap.values().iterator();
        while (iterator.hasNext()) {
            NativeAd nativeAd = iterator.next();
            nativeAd.onPause();
            Log.i(TAG, "Ad View onPause:" + nativeAd.toString());
        }
    }

    public void onDestroy() {

        if (mData != null) {
            Log.i(TAG, "Recycle Destory:" + mData.size());
            Iterator<RecycleViewDataBean> dataBeanIterator = mData.iterator();
            while (dataBeanIterator.hasNext()) {
                RecycleViewDataBean recycleViewDataBean = dataBeanIterator.next();
                if (recycleViewDataBean.nativeAd != null) {
                    recycleViewDataBean.nativeAd.destory();
                }
            }
            mData.clear();
            mData = null;
        }

        mOnNativeListCallback = null;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof AdViewHolder) {
            Log.i(TAG, "Ad View recycled:" + holder.getLayoutPosition() + "---holder:" + holder.toString());
            RecycleViewDataBean recycleViewDataBean = ((AdViewHolder) holder).recycleViewDataBean;

            if (recycleViewDataBean != null && recycleViewDataBean.nativeAd != null) {
                mImpressionAdMap.remove(String.valueOf(recycleViewDataBean.nativeAd.hashCode()), recycleViewDataBean.nativeAd);
                recycleViewDataBean.nativeAd.onPause();
            }
        }
    }

    public static class DataViewHolder extends RecyclerView.ViewHolder {

        View mView;
        TextView mTvData;

        DataViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            mTvData = mView.findViewById(R.id.tv_data);
        }
    }

    public static class AdViewHolder extends RecyclerView.ViewHolder {

        View mAdContainerRoot;
        ATNativeView mATNativeView;
        View mSelfRenderView;
        RecycleViewDataBean recycleViewDataBean;

        AdViewHolder(@NonNull View itemView) {
            super(itemView);
            mAdContainerRoot = itemView.findViewById(R.id.fl_ad_container_root);
            mATNativeView = itemView.findViewById(R.id.ad_container);
            mSelfRenderView = mATNativeView.findViewById(R.id.self_render_view);
        }

        protected void setCurrentRecycleViewDataBean(RecycleViewDataBean recycleViewDataBean) {
            this.recycleViewDataBean = recycleViewDataBean;
        }
    }

    public static class MoreViewHolder extends RecyclerView.ViewHolder {

        View mView;
        TextView mTvMore;

        MoreViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            mTvMore = itemView.findViewById(R.id.tv_more);
        }
    }

    public interface OnNativeListCallback {
        void onClickLoadMore();
    }
}
