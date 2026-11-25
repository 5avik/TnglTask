package pro.tngl.tangledtask.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import pro.tngl.tangledtask.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val LocalDotTypography = compositionLocalOf { true }
internal val LocalNothingMotion = compositionLocalOf { true }

private val DotMatrixFamily = FontFamily(Font(R.font.dot_matrix_mono_bold, weight = FontWeight.Bold))

@Composable
fun NothingDotMatrixText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val displayStyle = if (LocalDotTypography.current) {
        style.copy(fontFamily = DotMatrixFamily, fontWeight = FontWeight.Bold)
    } else {
        style
    }
    Text(text, modifier, color = color, style = displayStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun NothingCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val fraction by animateFloatAsState(checked.let { if (it) 1f else 0f },
        if (LocalNothingMotion.current) tween(160) else snap(), label = "checkbox")
    val colors = MaterialTheme.colorScheme
    Box(modifier.size(48.dp).toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(21.dp)) {
            val stroke = 1.6.dp.toPx()
            drawCircle(colors.onSurfaceVariant, radius = center.x - stroke / 2, style = Stroke(stroke))
            if (fraction > 0f) {
                drawCircle(colors.primary, radius = center.x * fraction)
                val start = Offset(size.width * .25f, size.height * .51f)
                val middle = Offset(size.width * .43f, size.height * .68f)
                val end = Offset(size.width * .75f, size.height * .34f)
                drawLine(colors.onPrimary.copy(alpha = fraction), start, middle, stroke, StrokeCap.Round)
                drawLine(colors.onPrimary.copy(alpha = fraction), middle, end, stroke, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun NothingSegmentedProgress(progress: Float, modifier: Modifier = Modifier) {
    val active = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(7.dp).semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f)
    }) {
        val count = 24
        val cell = size.width / count
        repeat(count) { i ->
            val amount = (progress * count - i).coerceIn(0f, 1f)
            val width = (cell - 2.dp.toPx()).coerceAtLeast(0f)
            val position = Offset(i * cell, 0f)
            drawRoundRect(track, position, Size(width, size.height), CornerRadius(1.dp.toPx()))
            if (amount > 0) drawRoundRect(active, position, Size(width * amount, size.height), CornerRadius(1.dp.toPx()))
        }
    }
}

@Composable
fun NothingProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        repeat(24) { index ->
            drawArc(if (index < progress * 24) colors.primary else colors.surfaceVariant,
                startAngle = -90f + index * 15f, sweepAngle = 10f, useCenter = false,
                topLeft = Offset(stroke, stroke), size = Size(size.width - 2 * stroke, size.height - 2 * stroke),
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun NothingCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, content = content)
}

@Composable
fun NothingButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier, enabled, shape = MaterialTheme.shapes.medium, content = content)
}

@Composable
fun NothingIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick, content = content)
}

@Composable
fun NothingSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Switch(checked, onCheckedChange, modifier, colors = SwitchDefaults.colors(
        checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary,
        uncheckedThumbColor = colors.onSurfaceVariant, uncheckedTrackColor = colors.surfaceVariant,
    ))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NothingBottomSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.extraLarge,
        dragHandle = {
            Box(Modifier.padding(vertical = 14.dp).size(28.dp, 3.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
        }, content = content)
}

@Composable
fun NothingNavigationBar(selected: Int, titles: List<String>, onSelect: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                titles.forEachIndexed { index, title ->
                    val active = index == selected
                    val ink = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(Modifier.weight(1f).heightIn(min = 56.dp)
                        .selectable(active, role = Role.Tab, onClick = { onSelect(index) }).padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.size(12.dp, 2.dp).background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape))
                        NothingNavIcon(index, ink)
                        Text(title, color = ink, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun NothingNavIcon(index: Int, color: Color) {
    Canvas(Modifier.size(21.dp)) {
        val step = size.width / 24
        val stroke = 1.5.dp.toPx()
        fun line(x: Int, y: Int, x2: Int, y2: Int) {
            drawLine(color, Offset(x * step, y * step), Offset(x2 * step, y2 * step), stroke, StrokeCap.Round)
        }
        when (index) {
            0 -> {
                drawCircle(color, 9 * step, style = Stroke(stroke))
                line(8, 12, 11, 15)
                line(11, 15, 16, 9)
            }
            1 -> {
                drawRoundRect(color, Offset(3 * step, 5 * step), Size(18 * step, 16 * step),
                    CornerRadius(2 * step), style = Stroke(stroke))
                line(3, 10, 21, 10)
                line(8, 3, 8, 7)
                line(16, 3, 16, 7)
            }
            2 -> for (y in listOf(6, 12, 18)) {
                drawCircle(color, step, Offset(4 * step, y * step))
                line(8, y, 21, y)
            }
            else -> {
                line(5, 19, 5, 12)
                line(12, 19, 12, 5)
                line(19, 19, 19, 9)
            }
        }
    }
}

@Composable
fun NothingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    singleLine: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(value, onValueChange, modifier, label = label, singleLine = singleLine,
        leadingIcon = leadingIcon, keyboardOptions = keyboardOptions, shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.onSurface,
            unfocusedBorderColor = colors.onSurfaceVariant,
            focusedLabelColor = colors.onSurface,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
        ))
}
