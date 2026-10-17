package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.car.autoverdict.report.Fact
import com.car.autoverdict.ui.components.AvFactTile
import com.car.autoverdict.ui.theme.AvTheme

/** Spec 5.2-5: key facts as a two-column tile grid. Callers skip it when [facts] is empty. */
@Composable
fun FactGrid(facts: List<Fact>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = AvTheme.spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { fact ->
                    AvFactTile(
                        label = fact.label,
                        value = fact.value,
                        tone = fact.tone.toAvTone(),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
