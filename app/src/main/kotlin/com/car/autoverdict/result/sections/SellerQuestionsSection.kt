package com.car.autoverdict.result.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.car.autoverdict.R
import com.car.autoverdict.ui.components.AvButton
import com.car.autoverdict.ui.components.AvButtonSize
import com.car.autoverdict.ui.components.AvButtonStyle
import com.car.autoverdict.ui.components.AvSectionHeader
import com.car.autoverdict.ui.theme.AvTheme

/**
 * Spec 5.2-11: numbered questions to send the seller, with copy and share. The copied/shared
 * text comes from the report's shareText, so this only lists [questions] for reading.
 */
@Composable
fun SellerQuestionsSection(
    questions: List<String>,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AvTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        AvSectionHeader(title = stringResource(R.string.result_section_questions), count = questions.size)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AvTheme.spacing.screenHorizontal)
                .padding(top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            questions.forEachIndexed { index, question ->
                Text(
                    text = stringResource(R.string.result_question_item, index + 1, question),
                    style = AvTheme.typography.body2,
                    color = colors.textPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvButton(
                    text = stringResource(R.string.result_questions_copy),
                    onClick = onCopy,
                    style = AvButtonStyle.Secondary,
                    size = AvButtonSize.Small,
                )
                AvButton(
                    text = stringResource(R.string.result_questions_share),
                    onClick = onShare,
                    style = AvButtonStyle.Secondary,
                    size = AvButtonSize.Small,
                )
            }
        }
    }
}
