package com.hitunguang.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// HITUNGUANG BRAND PALETTE v2 — Designed to feel premium, human, and trustworthy
// Replaced generic AI-slop Tailwind colors with a curated palette that evokes
// financial confidence: deep forest greens + warm amber golds + cool slate.
// ============================================================================

// ------------------ LIGHT COLORS ------------------

/** Main brand color — deep forest teal, calmer than generic Teal-600 */
val LightPrimary = Color(0xFF0F6E62)
/** On-primary text — crisp white */
val LightOnPrimary = Color(0xFFFFFFFF)
/** Primary container — soft seafoam for hover/chip backgrounds */
val LightPrimaryContainer = Color(0xFFD1F2EC)
/** On-primary container — dark for text on primary container */
val LightOnPrimaryContainer = Color(0xFF05443D)

/** Secondary — warm amber gold, suggests wealth/energy, avoids generic green */
val LightSecondary = Color(0xFFD58A28)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFDF0D5)
val LightOnSecondaryContainer = Color(0xFF523600)

/** Tertiary — muted slate for tertiary surfaces and accents */
val LightTertiary = Color(0xFF4F5364)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFDDE1F0)
val LightOnTertiaryContainer = Color(0xFF2D3142)

/** Background — very subtle cool grey-blue, cleaner than raw #FAFAFA */
val LightBackground = Color(0xFFF8FAFC)
val LightOnBackground = Color(0xFF1B1F23)

/** Surface — pure white for cards, elevated elements */
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1B1F23)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightOnSurfaceVariant = Color(0xFF556170)

// Light Surface Container Tones (Material 3 expressive surfaces)
val LightSurfaceDim = Color(0xFFE2E8F0)
val LightSurfaceBright = Color(0xFFFFFFFF)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF8FAFC)
val LightSurfaceContainer = Color(0xFFFFFFFF)
val LightSurfaceContainerHigh = Color(0xFFF1F5F9)
val LightSurfaceContainerHighest = Color(0xFFE2E8F0)

// Light Outline
val LightOutline = Color(0xFFCBD5E1)
val LightOutlineVariant = Color(0xFFE2E8F0)

// ------------------ DARK COLORS ------------------

/** Dark variant of primary — more luminous teal */
val DarkPrimary = Color(0xFF5EEAD4)
val DarkOnPrimary = Color(0xFF042F2E)
val DarkPrimaryContainer = Color(0xFF0F6E62)
val DarkOnPrimaryContainer = Color(0xFFD1F2EC)

/** Dark secondary — warm amber remains */
val DarkSecondary = Color(0xFFF8B868)
val DarkOnSecondary = Color(0xFF412700)
val DarkSecondaryContainer = Color(0xFFA6671E)
val DarkOnSecondaryContainer = Color(0xFFFDF0D5)

/** Dark tertiary — cool lavender-slate */
val DarkTertiary = Color(0xFFB6BDD9)
val DarkOnTertiary = Color(0xFF2D3142)
val DarkTertiaryContainer = Color(0xFF4F5364)
val DarkOnTertiaryContainer = Color(0xFFDDE1F0)

/** Dark background — very dark subtle navy/charcoal */
val DarkBackground = Color(0xFF0F1214)
val DarkOnBackground = Color(0xFFF8FAFC)

/** Dark surface */
val DarkSurface = Color(0xFF181B1E)
val DarkOnSurface = Color(0xFFF8FAFC)
val DarkSurfaceVariant = Color(0xFF2A2E36)
val DarkOnSurfaceVariant = Color(0xFF8C9AAA)

// Dark Surface Container Tones
val DarkSurfaceDim = Color(0xFF0F1214)
val DarkSurfaceBright = Color(0xFF3A3E48)
val DarkSurfaceContainerLowest = Color(0xFF090B0D)
val DarkSurfaceContainerLow = Color(0xFF0F1214)
val DarkSurfaceContainer = Color(0xFF181B1E)
val DarkSurfaceContainerHigh = Color(0xFF22262D)
val DarkSurfaceContainerHighest = Color(0xFF2A2E36)

// Dark Outline
val DarkOutline = Color(0xFF3A3E48)
val DarkOutlineVariant = Color(0xFF2A2E36)

// ============================================================================
// GRADIENT TOKENS (used for hero cards, balance cards, etc.)
// ============================================================================

/** Balance card light gradient — primary to secondary, warm financial feel */
val BalanceGradientStartLight = Color(0xFF0F6E62)
val BalanceGradientEndLight = Color(0xFFD58A28)

/** Balance card dark gradient */
val BalanceGradientStartDark = Color(0xFF0F6E62)
val BalanceGradientEndDark = Color(0xFFF8B868)

// ============================================================================
// SEMANTIC STATUS COLORS
// ============================================================================

val IncomeGreen = Color(0xFF08916B)  // richer, less neon than old Emerald-500
val ExpenseRed = Color(0xFFDC352B)   // slightly desaturated, more premium
val TransferBlue = Color(0xFF2563EB)

// Budget Status Colors
val BudgetSafe = IncomeGreen
val BudgetWarning = Color(0xFFF59E0B)   // amber stays recognizable
val BudgetDanger = ExpenseRed
