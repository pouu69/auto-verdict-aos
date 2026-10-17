package com.car.autoverdict.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.theme.AvTheme

enum class StepState { Done, Active, Pending }

data class LoadingStep(val label: String, val state: StepState)

/** Step list for staged loading. Wiring to real collector stages happens in sub-project 1. */
@Composable
fun AvLoadingSteps(steps: List<LoadingStep>, modifier: Modifier = Modifier) {
    val colors = AvTheme.colors
    val typography = AvTheme.typography
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEach { step ->
            val stateLabel = stringResource(
                when (step.state) {
                    StepState.Done -> R.string.loading_step_done
                    StepState.Active -> R.string.loading_step_active
                    StepState.Pending -> R.string.loading_step_pending
                },
            )
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = stateLabel },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    when (step.state) {
                        StepState.Done -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
                        StepState.Active -> CircularProgressIndicator(modifier = Modifier.size(18.dp), color = colors.accentText, strokeWidth = 2.dp)
                        StepState.Pending -> Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(20.dp))
                    }
                }
                Text(
                    text = step.label,
                    style = typography.body2,
                    color = if (step.state == StepState.Pending) colors.textTertiary else colors.textPrimary,
                )
            }
        }
    }
}
