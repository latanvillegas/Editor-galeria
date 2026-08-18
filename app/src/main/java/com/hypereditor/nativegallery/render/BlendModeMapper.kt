package com.hypereditor.nativegallery.render

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Xfermode
import com.hypereditor.nativegallery.domain.model.LayerBlendMode

object BlendModeMapper {
    fun toPorterDuffXfermode(mode: LayerBlendMode): Xfermode {
        val porterDuffMode = when (mode) {
            LayerBlendMode.NORMAL -> PorterDuff.Mode.SRC_OVER
            LayerBlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            LayerBlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            LayerBlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            LayerBlendMode.DARKEN -> PorterDuff.Mode.DARKEN
            LayerBlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
        }
        return PorterDuffXfermode(porterDuffMode)
    }
}
