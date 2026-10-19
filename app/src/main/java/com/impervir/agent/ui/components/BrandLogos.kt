package com.impervir.agent.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.impervir.agent.data.Platform
import com.impervir.agent.data.Store
import com.impervir.agent.ui.theme.Brand
import com.impervir.agent.ui.theme.ImpervirTheme

/**
 * Official logos from Simple Icons (CC0). Never redraw these by hand.
 */
object BrandLogos {
    private fun logo(name: String, d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        }.build()

    val X by lazy { logo("x", "M18.901 1.153h3.68l-8.04 9.19L24 22.846h-7.406l-5.8-7.584-6.638 7.584H.474l8.6-9.83L0 1.154h7.594l5.243 6.932ZM17.61 20.644h2.039L6.486 3.24H4.298Z") }
    val LinkedIn by lazy { logo("linkedin", "M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z") }
    val Threads by lazy { logo("threads", "M12.186 24h-.007c-3.581-.024-6.334-1.205-8.184-3.509C2.35 18.44 1.5 15.586 1.472 12.01v-.017c.03-3.579.879-6.43 2.525-8.482C5.845 1.205 8.6.024 12.18 0h.014c2.746.02 5.043.725 6.826 2.098 1.677 1.29 2.858 3.13 3.509 5.467l-2.04.569c-1.104-3.96-3.898-5.984-8.304-6.015-2.91.022-5.11.936-6.54 2.717C4.307 6.504 3.616 8.914 3.589 12c.027 3.086.718 5.496 2.057 7.164 1.43 1.783 3.631 2.698 6.54 2.717 2.623-.02 4.358-.631 5.8-2.045 1.647-1.613 1.618-3.593 1.09-4.798-.31-.71-.873-1.3-1.634-1.75-.192 1.352-.622 2.446-1.284 3.272-.886 1.102-2.14 1.704-3.73 1.79-1.202.065-2.361-.218-3.259-.801-1.063-.689-1.685-1.74-1.752-2.964-.065-1.19.408-2.285 1.33-3.082.88-.76 2.119-1.207 3.583-1.291a13.853 13.853 0 0 1 3.02.142c-.126-.742-.375-1.332-.75-1.757-.513-.586-1.308-.883-2.359-.89h-.029c-.844 0-1.992.232-2.721 1.32L7.734 7.847c.98-1.454 2.568-2.256 4.478-2.256h.044c3.194.02 5.097 1.975 5.287 5.388.108.046.216.094.321.142 1.49.7 2.58 1.761 3.154 3.07.797 1.82.871 4.79-1.548 7.158-1.85 1.81-4.094 2.628-7.277 2.65Zm1.003-11.69c-.242 0-.487.007-.739.021-1.836.103-2.98.946-2.916 2.143.067 1.256 1.452 1.839 2.784 1.767 1.224-.065 2.818-.543 3.086-3.71a10.5 10.5 0 0 0-2.215-.221z") }
    val Telegram by lazy { logo("telegram", "M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z") }
    val Stripe by lazy { logo("stripe", "M13.976 9.15c-2.172-.806-3.356-1.426-3.356-2.409 0-.831.683-1.305 1.901-1.305 2.227 0 4.515.858 6.09 1.631l.89-5.494C18.252.975 15.697 0 12.165 0 9.667 0 7.589.654 6.104 1.872 4.56 3.147 3.757 4.992 3.757 7.218c0 4.039 2.467 5.76 6.476 7.219 2.585.92 3.445 1.574 3.445 2.583 0 .98-.84 1.545-2.354 1.545-1.875 0-4.965-.921-6.99-2.109l-.9 5.555C5.175 22.99 8.385 24 11.714 24c2.641 0 4.843-.624 6.328-1.813 1.664-1.305 2.525-3.236 2.525-5.732 0-4.128-2.524-5.851-6.594-7.305h.003z") }
    val Gumroad by lazy { logo("gumroad", "M12 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0Zm-.007 5.12c4.48 0 5.995 3.025 6.064 4.744h-3.239c-.069-.962-.897-2.406-2.896-2.406-2.136 0-3.514 1.857-3.514 4.126 0 2.27 1.378 4.125 3.514 4.125 1.93 0 2.758-1.512 3.103-3.025h-3.103v-1.238h6.509v6.327h-2.855v-3.989c-.207 1.444-1.102 4.264-4.617 4.264-3.516 0-5.584-2.82-5.584-6.326 0-3.645 2.276-6.602 6.618-6.602z") }
    val LemonSqueezy by lazy { logo("lemonsqueezy", "m7.4916 10.835 2.3748-6.5114a3.1497 3.1497 0 0 0-.065-2.3418C9.0315.183 6.9427-.398 5.2928.265 3.643.929 2.71 2.4348 3.512 4.3046l2.8197 6.5615c.219.509.97.489 1.16-.03m1.6798 1.0969 6.5334-2.7758c2.1699-.9219 2.7218-3.6907 1.022-5.2905l-.068-.063c-1.6669-1.5469-4.4217-1.002-5.3706 1.0359L8.3566 11.135c-.234.503.295 1.0199.8159.7979m.373.87 6.6454-2.5119c2.2078-.8349 4.6206.745 4.5886 3.0398l-.002.09c-.048 2.2358-2.3938 3.7376-4.5536 2.9467l-6.6724-2.4418a.595.595 0 0 1-.006-1.1229m-.386 1.9269 6.4375 2.9767a3.2997 3.2997 0 0 1 1.6658 1.6989c.769 1.7998-.283 3.6396-1.9328 4.3016-1.6499.662-3.4097.235-4.2097-1.6359l-2.8027-6.5694c-.217-.509.328-1.009.8419-.772") }

    fun of(platform: Platform): ImageVector = when (platform) {
        Platform.X -> X
        Platform.LinkedIn -> LinkedIn
        Platform.Threads -> Threads
        Platform.Telegram -> Telegram
    }

    fun of(store: Store): ImageVector = when (store) {
        Store.Stripe -> Stripe
        Store.Gumroad -> Gumroad
        Store.LemonSqueezy -> LemonSqueezy
    }
}

fun Platform.brandColor(): Color = when (this) {
    Platform.X -> Brand.X
    Platform.LinkedIn -> Brand.LinkedIn
    Platform.Threads -> Brand.Threads
    Platform.Telegram -> Brand.Telegram
}

fun Store.brandColor(): Color = when (this) {
    Store.Stripe -> Brand.Stripe
    Store.Gumroad -> Brand.Gumroad
    Store.LemonSqueezy -> Brand.LemonSqueezy
}

/**
 * Platform badge: the official logo in its brand color on a soft tile.
 * X and Threads are black marks, so they flip to white in dark theme.
 */
@Composable
fun PlatformBadge(platform: Platform, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val dark = ImpervirTheme.extras.isDark
    val mark = when {
        dark && (platform == Platform.X || platform == Platform.Threads) -> Color.White
        else -> platform.brandColor()
    }
    val tile = if (dark) Color.White.copy(alpha = 0.08f) else Color(0xFFF0ECE6)
    Box(
        modifier.size(size).background(tile, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = BrandLogos.of(platform),
            contentDescription = platform.label,
            colorFilter = ColorFilter.tint(mark),
            modifier = Modifier.padding(size * 0.2f).size(size * 0.6f),
        )
    }
}

@Composable
fun StoreBadge(store: Store, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier.size(size).background(store.brandColor().copy(alpha = 0.14f), RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = BrandLogos.of(store),
            contentDescription = store.label,
            colorFilter = ColorFilter.tint(if (store == Store.Gumroad) Color(0xFF141318) else store.brandColor()),
            modifier = Modifier.size(size * 0.56f),
        )
    }
}
