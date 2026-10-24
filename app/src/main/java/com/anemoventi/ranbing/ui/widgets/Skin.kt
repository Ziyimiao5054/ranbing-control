@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.anemoventi.ranbing.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.ButtonDefaults as M3ButtonDefaults
import androidx.compose.material3.AlertDialog as M3AlertDialog
import androidx.compose.material3.TextButton as M3TextButton
import androidx.compose.material3.SwitchDefaults as M3SwitchDefaults
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.CardDefaults as M3CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton as M3FilledTonalIconButton
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.LargeFlexibleTopAppBar as M3LargeFlexibleTopAppBar
import androidx.compose.material3.LargeTopAppBar as M3LargeTopAppBar
import androidx.compose.material3.ListItem as M3ListItem
import androidx.compose.material3.ListItemDefaults as M3ListItemDefaults
import androidx.compose.material3.MaterialTheme as M3MaterialTheme
import androidx.compose.material3.NavigationBar as M3NavigationBar
import androidx.compose.material3.NavigationBarItem as M3NavigationBarItem
import androidx.compose.material3.RadioButton as M3RadioButton
import androidx.compose.material3.Scaffold as M3Scaffold
import androidx.compose.material3.Slider as M3Slider
import androidx.compose.material3.Switch as M3Switch
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TopAppBarDefaults as M3TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anemoventi.ranbing.ui.theme.LocalIsDark
import com.anemoventi.ranbing.ui.theme.LocalUseMiuix
import top.yukonga.miuix.kmp.basic.BasicComponent as MiuixListItem
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.SmallTitle as MiuixSmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar as MiuixSmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference as MiuixArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog

data class SkinTab(val label: String, val icon: ImageVector)

enum class SkinTextKind { TITLE, HEADLINE, BODY, CAPTION, VALUE }

/**
 * Skin-agnostic building blocks. Each helper picks the native widget of the active design
 * language (Material 3 or MIUIx) and follows the InstallerX visual language: a large collapsing
 * title, standalone rounded cards per row and a pill bottom navigation bar.
 */
@Composable
fun SkinScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    if (LocalUseMiuix.current) {
        val background = MiuixTheme.colorScheme.background
        val onBackground = MiuixTheme.colorScheme.onBackground
        val dark = background.luminance() < 0.5f
        val pageBackground = if (dark) background else Color(0xFFF2F3F5)
        val collapsedBar = if (dark) background else Color.White
        val scheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFF287AF9),
                background = pageBackground,
                surface = pageBackground,
                onBackground = onBackground,
                onSurface = onBackground,
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF287AF9),
                background = pageBackground,
                surface = Color.White,
                onBackground = onBackground,
                onSurface = onBackground,
            )
        }
        val behavior = M3TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        M3MaterialTheme(colorScheme = scheme) {
            M3Scaffold(
                containerColor = pageBackground,
                topBar = {
                    M3LargeTopAppBar(
                        title = { M3Text(title) },
                        scrollBehavior = behavior,
                        navigationIcon = {
                            if (onBack != null) {
                                M3FilledTonalIconButton(onClick = onBack) {
                                    M3Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "返回",
                                    )
                                }
                            }
                        },
                        colors = M3TopAppBarDefaults.topAppBarColors(
                            containerColor = pageBackground,
                            scrolledContainerColor = collapsedBar,
                        ),
                    )
                },
                bottomBar = bottomBar,
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .nestedScroll(behavior.nestedScrollConnection),
                ) {
                    content(PaddingValues(0.dp))
                }
            }
        }
    } else {
        val behavior = M3TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        M3Scaffold(
            containerColor = M3MaterialTheme.colorScheme.surface,
            topBar = {
                M3LargeTopAppBar(
                    title = { M3Text(title) },
                    scrollBehavior = behavior,
                    navigationIcon = {
                        if (onBack != null) {
                            M3FilledTonalIconButton(onClick = onBack) {
                                M3Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    colors = M3TopAppBarDefaults.topAppBarColors(
                        containerColor = M3MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = M3MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = bottomBar,
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .nestedScroll(behavior.nestedScrollConnection),
            ) {
                content(PaddingValues(0.dp))
            }
        }
    }
}

/** Outer shell for the tabbed main screen: only the bottom bar is fixed here. */
@Composable
fun SkinMainScaffold(
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (LocalUseMiuix.current) {
        MiuixScaffold(bottomBar = bottomBar) { padding -> content(padding) }
    } else {
        M3Scaffold(
            containerColor = M3MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = bottomBar,
        ) { padding -> content(padding) }
    }
}

/**
 * One tab page: its own collapsing large title plus content. The bottom padding of the outer
 * scaffold is applied to the content so the fixed navigation bar never covers it.
 */
@Composable
fun SkinTabPage(
    title: String,
    outerPadding: PaddingValues,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val bottom = outerPadding.calculateBottomPadding()
    if (LocalUseMiuix.current) {
        // The reference app uses the Material 3 large flexible bar even on MIUIx.
        val behavior = M3TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val background = MiuixTheme.colorScheme.background
        val onBackground = MiuixTheme.colorScheme.onBackground
        val scheme = if (background.luminance() < 0.5f) {
            darkColorScheme(
                primary = Color(0xFF287AF9),
                background = background,
                surface = background,
                onBackground = onBackground,
                onSurface = onBackground,
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF287AF9),
                background = background,
                surface = background,
                onBackground = onBackground,
                onSurface = onBackground,
            )
        }
        val dark = background.luminance() < 0.5f
        val pageBackground = if (dark) background else Color(0xFFF2F3F5)
        val collapsedBar = if (dark) background else Color.White
        M3MaterialTheme(colorScheme = scheme) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBackground),
            ) {
                M3LargeFlexibleTopAppBar(
                    title = { M3Text(title) },
                    scrollBehavior = behavior,
                    actions = { actions() },
                    colors = M3TopAppBarDefaults.topAppBarColors(
                        containerColor = pageBackground,
                        scrolledContainerColor = collapsedBar,
                    ),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(behavior.nestedScrollConnection),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottom),
                    ) {
                        content()
                    }
                }
            }
        }
    } else {
        val behavior = M3TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Column(modifier = Modifier.fillMaxSize()) {
            M3LargeFlexibleTopAppBar(
                title = { M3Text(title) },
                scrollBehavior = behavior,
                actions = { actions() },
                colors = M3TopAppBarDefaults.topAppBarColors(
                    containerColor = M3MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = M3MaterialTheme.colorScheme.surface,
                ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(behavior.nestedScrollConnection),
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = bottom)) {
                    content()
                }
            }
        }
    }
}

enum class SkinStatusTone { ACTIVE, NEUTRAL, ERROR }

/**
 * Big status card, ported from the reference app's MIUIx `MiuixStatusGrid`: a tinted card with a
 * huge decorative icon in the bottom end corner, a 20sp title, a 14sp description and a third
 * detail line placed 36dp below.
 */
@Composable
fun SkinStatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: SkinStatusTone,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
) {
    if (LocalUseMiuix.current) {
        val dark = LocalIsDark.current
        val container = when (tone) {
            SkinStatusTone.ACTIVE -> if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
            SkinStatusTone.ERROR -> if (dark) Color(0xFF381A1A) else Color(0xFFFAEEEE)
            SkinStatusTone.NEUTRAL -> MiuixTheme.colorScheme.surfaceContainer
        }
        val accent = when (tone) {
            SkinStatusTone.ACTIVE -> Color(0xFF36D167)
            SkinStatusTone.ERROR -> Color(0xFFD13636)
            SkinStatusTone.NEUTRAL -> MiuixTheme.colorScheme.onSurfaceVariantSummary
        }
        val textColor = MiuixTheme.colorScheme.onSurface
        val descColor = textColor.copy(alpha = 0.8f)

        MiuixCard(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(0.dp),
            colors = MiuixCardDefaults.defaultColors(
                color = container,
                contentColor = textColor,
            ),
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
            onClick = onClick,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(46.dp, 28.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    MiuixIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(150.dp),
                        tint = accent.copy(alpha = 0.8f),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    MiuixText(
                        modifier = Modifier.fillMaxWidth(),
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    Spacer(Modifier.height(2.dp))
                    MiuixText(
                        modifier = Modifier.fillMaxWidth(),
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = descColor,
                    )
                    if (detail != null) {
                        Spacer(Modifier.height(36.dp))
                        MiuixText(
                            modifier = Modifier.fillMaxWidth(),
                            text = detail,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = descColor,
                        )
                    } else {
                        // Keep the reference card's proportions so the large corner icon still reads.
                        Spacer(Modifier.height(56.dp))
                    }
                }
            }
        }
    } else {
        val container = when (tone) {
            SkinStatusTone.ACTIVE -> M3MaterialTheme.colorScheme.secondaryContainer
            SkinStatusTone.ERROR -> M3MaterialTheme.colorScheme.errorContainer
            SkinStatusTone.NEUTRAL -> M3MaterialTheme.colorScheme.surfaceContainer
        }
        val contentColor = when (tone) {
            SkinStatusTone.ACTIVE -> M3MaterialTheme.colorScheme.onSecondaryContainer
            SkinStatusTone.ERROR -> M3MaterialTheme.colorScheme.onErrorContainer
            SkinStatusTone.NEUTRAL -> M3MaterialTheme.colorScheme.onSurface
        }
        M3Card(
            modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = M3CardDefaults.cardColors(containerColor = container),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                M3Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = contentColor,
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    M3Text(
                        text = title,
                        style = M3MaterialTheme.typography.titleMedium,
                        color = contentColor,
                    )
                    M3Text(
                        text = subtitle,
                        style = M3MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                    if (detail != null) {
                        M3Text(
                            text = detail,
                            style = M3MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkinCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalUseMiuix.current) {
        val cardModifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        if (onClick != null) {
            MiuixCard(
                modifier = cardModifier,
                insideMargin = PaddingValues(contentPadding),
                onClick = onClick,
            ) {
                content()
            }
        } else {
            MiuixCard(
                modifier = cardModifier,
                insideMargin = PaddingValues(contentPadding),
            ) {
                content()
            }
        }
    } else {
        val shape = RoundedCornerShape(20.dp)
        val clickModifier = if (onClick != null) {
            Modifier.clip(shape).clickable(onClick = onClick)
        } else {
            Modifier
        }
        M3Card(
            modifier = modifier.fillMaxWidth().then(clickModifier),
            shape = shape,
            colors = M3CardDefaults.cardColors(
                containerColor = M3MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun SkinIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalUseMiuix.current) {
        MiuixIconButton(onClick = onClick, modifier = modifier) {
            MiuixIcon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
    } else {
        M3FilledTonalIconButton(onClick = onClick, modifier = modifier) {
            M3Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

@Composable
fun SkinSectionTitle(text: String, modifier: Modifier = Modifier) {
    if (LocalUseMiuix.current) {
        // MIUIx pages have no section captions; cards are separated by spacing only.
        return
    }
    M3Text(
        text = text,
        modifier = modifier.padding(start = 12.dp, top = 20.dp, bottom = 4.dp),
        style = M3MaterialTheme.typography.titleSmall,
        color = M3MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SkinText(
    text: String,
    kind: SkinTextKind = SkinTextKind.BODY,
    modifier: Modifier = Modifier,
) {
    if (LocalUseMiuix.current) {
        MiuixText(
            text = text,
            modifier = modifier,
            style = when (kind) {
                SkinTextKind.TITLE -> MiuixTheme.textStyles.title4
                SkinTextKind.HEADLINE -> MiuixTheme.textStyles.headline2
                SkinTextKind.VALUE -> MiuixTheme.textStyles.title3
                SkinTextKind.BODY -> MiuixTheme.textStyles.body2
                SkinTextKind.CAPTION -> MiuixTheme.textStyles.footnote1
            },
            color = when (kind) {
                SkinTextKind.CAPTION -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                SkinTextKind.HEADLINE -> MiuixTheme.colorScheme.onBackground
                else -> MiuixTheme.colorScheme.onSurface
            },
        )
    } else {
        M3Text(
            text = text,
            modifier = modifier,
            style = when (kind) {
                SkinTextKind.TITLE -> M3MaterialTheme.typography.titleMedium
                SkinTextKind.HEADLINE -> M3MaterialTheme.typography.headlineLarge
                SkinTextKind.VALUE -> M3MaterialTheme.typography.headlineSmall
                SkinTextKind.BODY -> M3MaterialTheme.typography.bodyMedium
                SkinTextKind.CAPTION -> M3MaterialTheme.typography.bodySmall
            },
            color = when (kind) {
                SkinTextKind.CAPTION -> M3MaterialTheme.colorScheme.onSurfaceVariant
                SkinTextKind.HEADLINE -> M3MaterialTheme.colorScheme.onBackground
                else -> M3MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
fun SkinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val container = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.surfaceContainer
    } else {
        M3MaterialTheme.colorScheme.surfaceContainer
    }
    val textColor = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurface
    } else {
        M3MaterialTheme.colorScheme.onSurface
    }
    val hintColor = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    } else {
        M3MaterialTheme.colorScheme.onSurfaceVariant
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 16.sp, color = textColor),
        cursorBrush = SolidColor(textColor),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = hintColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    androidx.compose.foundation.text.BasicText(
                        text = placeholder,
                        style = TextStyle(fontSize = 16.sp, color = hintColor),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
fun SkinPromptDialog(
    show: Boolean,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    confirmText: String,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    if (LocalUseMiuix.current) {
        MiuixWindowDialog(
            show = true,
            title = title,
            onDismissRequest = onDismiss,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SkinInputField(value = value, onValueChange = onValueChange, placeholder = "璁惧鍚嶇О")
                if (error != null) {
                    MiuixText(
                        text = error,
                        color = Color(0xFFD70014),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkinButton(
                        text = "取消",
                        modifier = Modifier.weight(1f),
                        containerColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MiuixTheme.colorScheme.onSurface,
                        onClick = onDismiss,
                    )
                    SkinButton(
                        text = confirmText,
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                    )
                }
            }
        }
    } else {
        M3AlertDialog(
            onDismissRequest = onDismiss,
            title = { M3Text(title) },
            text = {
                Column {
                    SkinInputField(value = value, onValueChange = onValueChange, placeholder = "璁惧鍚嶇О")
                    if (error != null) {
                        M3Text(
                            text = error,
                            color = Color(0xFFD70014),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                M3TextButton(onClick = onConfirm) { M3Text(confirmText) }
            },
            dismissButton = {
                M3TextButton(onClick = onDismiss) { M3Text("取消") }
            },
        )
    }
}

@Composable
fun SkinAlertDialog(
    show: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "取消",
) {
    if (!show) return
    if (LocalUseMiuix.current) {
        MiuixWindowDialog(
            show = true,
            title = title,
            summary = message,
            onDismissRequest = onDismiss,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkinButton(
                    text = dismissText,
                    modifier = Modifier.weight(1f),
                    containerColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                    onClick = onDismiss,
                )
                SkinButton(
                    text = confirmText,
                    modifier = Modifier.weight(1f),
                    containerColor = Color(0xFFD70014),
                    onClick = onConfirm,
                )
            }
        }
    } else {
        M3AlertDialog(
            onDismissRequest = onDismiss,
            title = { M3Text(title) },
            text = { M3Text(message) },
            confirmButton = {
                M3TextButton(onClick = onConfirm) {
                    M3Text(confirmText, color = Color(0xFFD70014))
                }
            },
            dismissButton = {
                M3TextButton(onClick = onDismiss) { M3Text(dismissText) }
            },
        )
    }
}

@Composable
fun SkinButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
) {
    if (LocalUseMiuix.current) {
        MiuixButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (containerColor != null) {
                MiuixButtonDefaults.buttonColors(
                    color = containerColor,
                    contentColor = contentColor,
                )
            } else {
                MiuixButtonDefaults.buttonColorsPrimary()
            },
        ) {
            MiuixText(
                text = text,
                color = if (containerColor != null) contentColor else Color.Unspecified,
            )
        }
    } else {
        M3Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (containerColor != null) {
                M3ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                )
            } else {
                M3ButtonDefaults.buttonColors()
            },
        ) {
            M3Text(text)
        }
    }
}

/** Statistic card. MIUIx puts the title top-left and the value bottom-left like the reference. */
@Composable
fun SkinStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    if (LocalUseMiuix.current) {
        MiuixCard(modifier = modifier, insideMargin = PaddingValues(16.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                MiuixText(
                    modifier = Modifier.fillMaxWidth(),
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                MiuixText(
                    modifier = Modifier.fillMaxWidth(),
                    text = value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    } else {
        SkinCard(modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SkinText(value, SkinTextKind.VALUE)
                SkinText(label, SkinTextKind.CAPTION)
            }
        }
    }
}

/** Label / value row used inside a card, styled like the reference app's info rows. */
@Composable
fun SkinInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SkinText(label, SkinTextKind.TITLE)
        SkinText(value, SkinTextKind.CAPTION)
    }
}

/** Non-clickable icon row, used for informational settings entries. */
@Composable
fun SkinIconRow(
    title: String,
    summary: String,
    icon: ImageVector,
) {
    if (LocalUseMiuix.current) {
        SkinCard(contentPadding = 0.dp) {
            MiuixListItem(
                title = title,
                summary = summary,
                insideMargin = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                startAction = {
                    MiuixIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                },
            )
        }
    } else {
        SkinCard(contentPadding = 0.dp) {
            M3ListItem(
                headlineContent = { M3Text(title) },
                supportingContent = { M3Text(summary) },
                leadingContent = {
                    M3Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = M3MaterialTheme.colorScheme.onSurface,
                    )
                },
                colors = M3ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable
fun SkinSwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    card: Boolean = true,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val row: @Composable () -> Unit = {
        if (LocalUseMiuix.current) {
            MiuixListItem(
                title = title,
                summary = summary,
                modifier = if (enabled) Modifier else Modifier.alpha(0.45f),
                insideMargin = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                endActions = {
                    MiuixSwitch(
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = onCheckedChange,
                    )
                },
            )
        } else {
            M3ListItem(
                headlineContent = { M3Text(title) },
                supportingContent = summary?.let { text -> { M3Text(text) } },
                trailingContent = {
                    M3Switch(
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = onCheckedChange,
                        thumbContent = if (checked) {
                            {
                                M3Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(M3SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                },
                colors = M3ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )
        }
    }
    if (card) {
        SkinCard(contentPadding = 0.dp) { row() }
    } else {
        row()
    }
}

@Composable
fun SkinChoiceRow(
    title: String,
    summary: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (LocalUseMiuix.current) {
        val selectedContainer = androidx.compose.ui.graphics.lerp(
            MiuixTheme.colorScheme.surfaceContainer,
            MiuixTheme.colorScheme.primary,
            0.22f,
        )
        MiuixCard(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(0.dp),
            colors = if (selected) {
                MiuixCardDefaults.defaultColors(
                    color = selectedContainer,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                )
            } else {
                MiuixCardDefaults.defaultColors()
            },
        ) {
            MiuixListItem(
                title = title,
                summary = summary,
                insideMargin = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                onClick = onClick,
                endActions = {
                    SkinRadioIndicator(selected)
                },
            )
        }
    } else {
        M3Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = M3CardDefaults.cardColors(
                containerColor = if (selected) {
                    M3MaterialTheme.colorScheme.secondaryContainer
                } else {
                    M3MaterialTheme.colorScheme.surfaceContainer
                },
            ),
        ) {
            M3ListItem(
                headlineContent = { M3Text(title) },
                supportingContent = summary?.let { text -> { M3Text(text) } },
                trailingContent = {
                    M3RadioButton(selected = selected, onClick = null)
                },
                colors = M3ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

/** Navigation row with a leading icon and a trailing chevron. */
@Composable
fun SkinNavRow(
    title: String,
    summary: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    if (LocalUseMiuix.current) {
        SkinCard(contentPadding = 0.dp) {
            MiuixArrowPreference(
                title = title,
                summary = summary,
                insideMargin = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                startAction = {
                    MiuixIcon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                },
                onClick = onClick,
            )
        }
    } else {
        SkinCard(contentPadding = 0.dp) {
            M3ListItem(
                headlineContent = { M3Text(title) },
                supportingContent = summary?.let { text -> { M3Text(text) } },
                leadingContent = {
                    M3Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = M3MaterialTheme.colorScheme.onSurface,
                    )
                },
                trailingContent = {
                    M3Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = M3MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(onClick = onClick),
                colors = M3ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

/** Reference-style circular radio indicator (ring + dot) for the MIUIx skin. */
@Composable
private fun SkinRadioIndicator(selected: Boolean) {
    if (!LocalUseMiuix.current) {
        M3RadioButton(selected = selected, onClick = null)
        return
    }
    val color = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(color, CircleShape),
            )
        }
    }
}

/** MIUIx secondary-page bar: circular back button and a left aligned title on one row. */
@Composable
private fun MiuixCompactTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                MiuixIcon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.width(16.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            MiuixText(
                text = title,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onBackground,
            )
        }
        actions()
    }
}

@Composable
fun SkinNavigationBar(
    tabs: List<SkinTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    if (LocalUseMiuix.current) {
        MiuixNavigationBar {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                            role = Role.Tab,
                        )
                        .padding(top = 2.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MiuixIcon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(26.dp),
                            tint = if (selected) {
                                MiuixTheme.colorScheme.onSurface
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            },
                        )
                    }
                    MiuixText(
                        text = tab.label,
                        style = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        color = if (selected) {
                            MiuixTheme.colorScheme.onSurface
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                    )
                }
            }
        }
    } else {
        M3NavigationBar(
            containerColor = M3MaterialTheme.colorScheme.surfaceContainer,
        ) {
            tabs.forEachIndexed { index, tab ->
                M3NavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = { M3Icon(tab.icon, contentDescription = tab.label) },
                    label = { M3Text(tab.label) },
                )
            }
        }
    }
}

@Composable
fun SkinPillRow(
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    center: Boolean = false,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    if (center) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, label ->
                    PillItem(label, index == selectedIndex, enabled) { onSelect(index) }
                }
            }
        }
    } else {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, label ->
                PillItem(label, index == selectedIndex, enabled) { onSelect(index) }
            }
        }
    }
}

@Composable
private fun PillItem(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (LocalUseMiuix.current) {
        val container = if (selected) {
            MiuixTheme.colorScheme.primary.copy(alpha = if (enabled) 0.22f else 0.12f)
        } else {
            MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (enabled) 1f else 0.5f)
        }
        val content = if (selected) {
            MiuixTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.45f)
        } else {
            MiuixTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(container)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            MiuixText(text = label, color = content)
        }
    } else {
        M3FilterChip(
            selected = selected,
            enabled = enabled,
            onClick = onClick,
            label = { M3Text(label) },
        )
    }
}

@Composable
fun SkinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
) {
    if (LocalUseMiuix.current) {
        MiuixSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    } else {
        M3Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

@Composable
fun SkinLabeledSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SkinText(title)
            SkinText(valueText, SkinTextKind.CAPTION)
        }
        SkinSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

@Composable
fun SkinMonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    if (LocalUseMiuix.current) {
        MiuixText(
            text = text,
            modifier = modifier,
            color = if (color == Color.Unspecified) MiuixTheme.colorScheme.onSurfaceVariantSummary else color,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
        )
    } else {
        M3Text(
            text = text,
            modifier = modifier,
            color = if (color == Color.Unspecified) M3MaterialTheme.colorScheme.onSurfaceVariant else color,
            style = M3MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
fun SkinNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    suffix: String? = null,
) {
    val container = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.surfaceContainerHigh
    } else {
        M3MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurface
    } else {
        M3MaterialTheme.colorScheme.onSurface
    }
    val hintColor = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    } else {
        M3MaterialTheme.colorScheme.onSurfaceVariant
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(fontSize = 16.sp, color = textColor),
        cursorBrush = SolidColor(textColor),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        androidx.compose.foundation.text.BasicText(
                            text = placeholder,
                            style = TextStyle(fontSize = 16.sp, color = hintColor),
                        )
                    }
                    inner()
                }
                if (suffix != null) {
                    androidx.compose.foundation.text.BasicText(
                        text = suffix,
                        style = TextStyle(fontSize = 14.sp, color = hintColor),
                    )
                }
            }
        },
    )
}

@Composable
fun SkinHexField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val container = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.surfaceContainerHigh
    } else {
        M3MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurface
    } else {
        M3MaterialTheme.colorScheme.onSurface
    }
    val hintColor = if (LocalUseMiuix.current) {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    } else {
        M3MaterialTheme.colorScheme.onSurfaceVariant
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = textColor,
        ),
        cursorBrush = SolidColor(textColor),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                androidx.compose.foundation.text.BasicText(
                    text = placeholder,
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = hintColor),
                )
            }
            inner()
        },
    )
}
