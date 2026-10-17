package com.car.autoverdict.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Corner radii from spec 4.4. */
@Immutable
data class AvShapes(
    val chip: Shape = RoundedCornerShape(percent = 50),
    val tile: Shape = RoundedCornerShape(12.dp),
    val buttonS: Shape = RoundedCornerShape(10.dp),
    val buttonM: Shape = RoundedCornerShape(14.dp),
    val buttonL: Shape = RoundedCornerShape(16.dp),
    val input: Shape = RoundedCornerShape(16.dp),
    val dialog: Shape = RoundedCornerShape(24.dp),
    val sheetTop: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
)
