package com.stillwater.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.stillwater.app.ui.theme.Motion
import com.stillwater.app.ui.theme.Spacing

/*
 * Tappable selection cards (never free text for common choices). Selection is
 * shown with a seafoam border + tinted container — a quiet state change, no
 * pop animation.
 */

@Composable
fun OptionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    multiSelect: Boolean = false,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(Motion.GENTLE, easing = Motion.CalmEase),
        label = "optionBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(Motion.GENTLE, easing = Motion.CalmEase),
        label = "optionContainer",
    )

    val selectionModifier = if (multiSelect) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.minTouchTarget + Spacing.md)
            .border(1.dp, borderColor, MaterialTheme.shapes.large)
            .then(selectionModifier),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 1–5 agreement scale as five large dots. One row per statement; no numbers
 * shouting at the user, just anchored ends.
 */
@Composable
fun AgreementRow(
    statement: String,
    value: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = statement,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (1..5).forEach { step ->
                val selected = value == step
                val dotColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    animationSpec = tween(Motion.GENTLE, easing = Motion.CalmEase),
                    label = "agreementDot",
                )
                Surface(
                    modifier = Modifier
                        .size(Spacing.minTouchTarget)
                        .selectable(selected = selected, role = Role.RadioButton) {
                            onValueChange(step)
                        },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = dotColor,
                    border = if (selected) {
                        null
                    } else {
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        )
                    },
                ) {}
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Not really",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Very much",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
