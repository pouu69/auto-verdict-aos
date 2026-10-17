package com.car.autoverdict.result.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.report.EvaluationReport
import com.car.autoverdict.ui.components.AvBadge
import com.car.autoverdict.ui.theme.AvTheme
import com.car.autoverdict.ui.theme.AvTone
import com.car.autoverdict.ui.theme.tone
import com.car.autoverdict.ui.theme.verdictLabelRes

/**
 * Spec 5.2-3: car name and summary, verdict badge with a mid-sized score in the verdict tone,
 * a thin score bar, then the conclusion sentence as the screen's main message.
 */
@Composable
fun VerdictHeader(report: EvaluationReport, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    val tone = AvTone.fromVerdict(report.verdict)
    val toneColors = colors.tone(tone)
    val scoreDescription = stringResource(R.string.result_score_cd, report.score)
    val carName = report.car.name?.takeIf { it.isNotBlank() } ?: report.car.title
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = AvTheme.spacing.screenHorizontal)) {
        if (carName.isNotBlank()) {
            // Wraps rather than truncating; real names fit in two lines.
            Text(
                text = carName,
                style = typography.body1.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(if (report.car.summary.isNotBlank()) 2.dp else 8.dp))
        }
        if (report.car.summary.isNotBlank()) {
            Text(text = report.car.summary, style = typography.caption1, color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvBadge(text = stringResource(verdictLabelRes(report.verdict)), tone = tone)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.result_score, report.score),
                style = typography.title2,
                color = toneColors.text,
                modifier = Modifier.semantics { contentDescription = scoreDescription },
            )
        }
        Spacer(Modifier.height(8.dp))
        // Decorative: the score is already read out above.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(AvTheme.shapes.chip)
                .background(colors.surfaceMuted),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(report.score.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .clip(AvTheme.shapes.chip)
                    .background(toneColors.fill),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = report.headline,
            style = typography.title1,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
    }
}
