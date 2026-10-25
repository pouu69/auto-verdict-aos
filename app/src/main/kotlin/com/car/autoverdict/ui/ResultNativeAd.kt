package com.car.autoverdict.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.car.autoverdict.BuildConfig
import com.car.autoverdict.R
import com.car.autoverdict.ui.ads.LocalContentAdsAllowed
import com.car.autoverdict.ui.theme.AvTheme
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import java.util.concurrent.atomic.AtomicInteger

/** Keep the request and its result alive across lazy item recycling, keyed to this report URL. */
@Composable
internal fun rememberResultNativeAdState(resultKey: String): NativeAdSession<NativeAd>? {
    if (!LocalContentAdsAllowed.current) return null
    val unitId = AdIds.resultNative ?: return null
    val context = LocalContext.current
    val state = remember(context, unitId, resultKey) {
        val requestId = nativeAdRequestSequence.incrementAndGet()
        NativeAdSession<NativeAd>(
            request = { onLoaded, onFailed ->
                AdLoader.Builder(context, unitId)
                    .forNativeAd { ad ->
                        nativeAdDebug(requestId, "native_loaded adChoices=${ad.adChoicesInfo != null} adChoicesImages=${ad.adChoicesInfo?.images?.size} response=${ad.responseInfo}")
                        onLoaded(ad)
                    }
                    .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) { onFailed(error.toString()) }
                        override fun onAdLoaded() { nativeAdDebug(requestId, "listener_loaded") }
                        override fun onAdImpression() { nativeAdDebug(requestId, "impression") }
                        override fun onAdClicked() { nativeAdDebug(requestId, "clicked") }
                        override fun onAdOpened() { nativeAdDebug(requestId, "opened") }
                        override fun onAdClosed() { nativeAdDebug(requestId, "closed") }
                    })
                    .build()
                    .also { loader ->
                        nativeAdDebug(requestId, "request loader=${System.identityHashCode(loader)}")
                        loader.loadAd(AdRequest.Builder().build())
                    }
            },
            destroy = { it.destroy() },
            log = { nativeAdDebug(requestId, it) },
        )
    }
    DisposableEffect(state) {
        onDispose { state.dispose() }
    }
    return state
}

/** One explicitly labelled ad after the report. The lazy view borrows the screen-owned ad. */
@Composable
internal fun ResultNativeAd(state: NativeAdSession<NativeAd>?, modifier: Modifier = Modifier) {
    if (!LocalContentAdsAllowed.current || state == null) return
    LaunchedEffect(state) { state.load() }
    val ad = state.ad ?: return
    val colors = AvTheme.colors
    val foreground = colors.textPrimary.toArgb()
    val secondary = colors.textSecondary.toArgb()
    val background = colors.background.toArgb()
    val border = colors.border.toArgb()
    key(ad) {
        AndroidView(
            modifier = modifier.fillMaxWidth().padding(horizontal = AvTheme.spacing.screenHorizontal, vertical = 24.dp),
            factory = {
                nativeAdDebug(System.identityHashCode(ad), "view_create")
                buildNativeAdView(it, ad, foreground, secondary, background, border).also { view ->
                    view.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
                        nativeAdDebug(System.identityHashCode(ad), "view_layout width=${right-left} height=${bottom-top} attached=${view.isAttachedToWindow}")
                    }
                }
            },
            onRelease = { nativeAdDebug(System.identityHashCode(ad), "view_release"); it.destroy() },
        )
    }
}

private fun buildNativeAdView(context: Context, ad: NativeAd, foreground: Int, secondary: Int, background: Int, border: Int): NativeAdView {
    fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun text(size: Float, color: Int) = TextView(context).apply { textSize = size; setTextColor(color) }
    val root = NativeAdView(context)
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        this.background = GradientDrawable().apply {
            setColor(background)
            setStroke(dp(1), border)
            cornerRadius = dp(12).toFloat()
        }
    }
    root.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    // Reserve the top-right corner for the SDK's AdChoices overlay.
    column.addView(text(12f, secondary).apply {
        setText(R.string.ad_attribution)
        setPadding(0, 0, dp(40), dp(12))
    })
    val heading = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    val icon = ImageView(context).apply {
        setImageDrawable(ad.icon?.drawable)
        visibility = if (ad.icon == null) View.GONE else View.VISIBLE
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    heading.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
    val headline = text(18f, foreground).apply { text = ad.headline; setTypeface(typeface, Typeface.BOLD) }
    heading.addView(headline, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    column.addView(heading)
    val advertiser = text(12f, secondary).apply {
        text = ad.advertiser
        visibility = if (ad.advertiser == null) View.GONE else View.VISIBLE
        setPadding(0, dp(8), 0, dp(8))
    }
    column.addView(advertiser)
    val media = MediaView(context).apply { mediaContent = ad.mediaContent }
    column.addView(media, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)).apply { topMargin = dp(12) })
    val body = text(14f, secondary).apply {
        text = ad.body
        visibility = if (ad.body == null) View.GONE else View.VISIBLE
        setPadding(0, dp(12), 0, dp(12))
    }
    column.addView(body)
    val cta = Button(context).apply {
        text = ad.callToAction
        visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
        minimumHeight = dp(48)
    }
    column.addView(cta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    root.headlineView = headline
    root.iconView = icon
    root.advertiserView = advertiser
    root.bodyView = body
    root.callToActionView = cta
    root.mediaView = media
    // No custom click listeners: impressions, asset clicks and AdChoices belong to the SDK.
    root.setNativeAd(ad)
    return root
}

private val nativeAdRequestSequence = AtomicInteger()

private fun nativeAdDebug(requestId: Int, message: String) {
    if (BuildConfig.DEBUG) Log.d("ResultNative", "request=$requestId $message")
}
