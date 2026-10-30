package app.reportamelo.services

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AdMobManager {
    private var rewardedAd: RewardedAd? = null
    private const val TAG = "AdMobManager"

    suspend fun loadRewardedAd(context: Context, adUnitID: String): Boolean = suspendCoroutine { continuation ->
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            adUnitID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Failed to load rewarded ad: ${adError.message}")
                    rewardedAd = null
                    continuation.resume(false)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad was loaded.")
                    rewardedAd = ad
                    continuation.resume(true)
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onReward: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "Reward received with currency: ${rewardItem.type}, amount ${rewardItem.amount}.")
                onReward()
            }
        } ?: run {
            Log.d(TAG, "Ad wasn't ready")
        }
    }
}
