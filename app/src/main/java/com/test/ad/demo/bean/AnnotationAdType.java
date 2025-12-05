package com.test.ad.demo.bean;

import androidx.annotation.IntDef;
import com.secmtp.sdk.core.api.ATAdConst;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef(value = {ATAdConst.ATMixedFormatAdType.NATIVE, ATAdConst.ATMixedFormatAdType.INTERSTITIAL, ATAdConst.ATMixedFormatAdType.REWARDED_VIDEO,
        ATAdConst.ATMixedFormatAdType.BANNER, ATAdConst.ATMixedFormatAdType.SPLASH, ATAdConst.ATMixedFormatAdType.MEDIA_VIDEO})
@Retention(RetentionPolicy.SOURCE)
public @interface AnnotationAdType {
    
}
