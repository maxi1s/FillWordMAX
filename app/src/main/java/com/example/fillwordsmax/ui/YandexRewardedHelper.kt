package com.example.fillwordsmax.ui

import android.app.Activity
import android.content.Context
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds

class YandexRewardedHelper(
    private val context: Context,
    private val adUnitId: String
) {
    private var rewardedAd: RewardedAd? = null
    private var rewardedAdLoader: RewardedAdLoader? = null
    private var onReward: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    fun loadAndShow(activity: Activity, onReward: () -> Unit, onError: (String) -> Unit = {}) {
        this.onReward = onReward
        this.onError = onError
        rewardedAdLoader = RewardedAdLoader(context)
        rewardedAdLoader?.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {}
                    override fun onAdFailedToShow(adError: AdError) {
                        onError(adError.description)
                    }
                    override fun onAdDismissed() {
                        rewardedAd?.setAdEventListener(null)
                        rewardedAd = null
                    }
                    override fun onAdClicked() {}
                    override fun onAdImpression(impressionData: ImpressionData?) {}
                    override fun onRewarded(reward: Reward) {
                        onReward?.invoke()
                    }
                })
                ad.show(activity)
            }
            override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                onError(adRequestError.description)
            }
        })
        val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
        rewardedAdLoader?.loadAd(adRequestConfiguration)
    }
} 