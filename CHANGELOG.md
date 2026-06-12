# Changelog

All notable changes to the HitungUang project will be documented in this file.

## [2.0.0] - 2026-06-12

### Added
- **Design System Overhaul (Phase 1)**: Overhauled visual foundation in `Color.kt`, `Type.kt`, `Spacing.kt`, `Radius.kt`, `Elevation.kt`, and `Theme.kt`.
  - Bundled premium **DM Sans** typeface as local font resources (`dm_sans_regular.ttf`, `dm_sans_medium.ttf`, `dm_sans_semibold.ttf`, `dm_sans_bold.ttf`) and created `dm_sans.xml` font family mapping.
  - Enabled **tabular numbers (tnum)** across all display, headline, and title text styles in `Type.kt` for perfect vertical alignment of financial amounts.
  - Replaced the generic blue palette with a deep trust blue primary (`#1A56DB` light, `#60A5FA` dark) and Slate theme accents (`#F8FAFC` background / Slate-900 surface containers).
  - Added semantic budget status colors (`BudgetSafe`, `BudgetWarning`, `BudgetDanger`).
  - Added Material 3 surface container tones (`surfaceDim`, `surfaceBright`, `surfaceContainerLowest/Low/Default/High/Highest`).
  - Expanded spacing system with `none`, `extraHuge` (40dp), and `massive` (48dp).
  - Expanded corner radius system with `none`, `extraSmall` (8dp), `extraLarge` (28dp), and `full` (100dp).
  - Expanded elevation system with `high` (4dp) and `extraHigh` (8dp).
- **Dashboard Redesign (Phase 2)**: Overhauled visual presentation of the main dashboard.
  - Redesigned the Hero Balance Card with a modern primary-to-primary-container gradient and custom typography.
  - Added a 2x2 grid of filled tonal buttons for quick actions.
  - Modernized the active budget health overview card with a thicker progress bar and semantic status colors.
  - Updated the spending category chart (donut chart Canvas stroke and inline category list).
  - Restricted recent transactions list to the 5 most recent items.
  - Completely removed the `InsightCard` from the dashboard layout.
- **Transaction List Screen Redesign (Phase 3)**: Overhauled the transaction history screen.
  - Set sorting chips to use `Radius.extraSmall` corner shape.
  - Redesigned date headers to use bold `titleMedium` typography.
  - Wrapped daily net totals in rounded, alpha-tinted semantic badges (`IncomeGreen` or `ExpenseRed`).
  - Redesigned transaction rows inside the list to align with swipe background corners (`Radius.medium`) and styled the category icon circle (`Spacing.extraHuge`) with tinted backgrounds.
  - Applied bold `titleMedium` to transaction amounts for tabular number alignment.
  - Redesigned empty state to use a larger icon (`Spacing.massive`) and consistent typography.
- **Transaction Form Redesign (Phase 4)**: Converted the transaction form from a cramped `AlertDialog` into a smooth `ModalBottomSheet`.
  - Added a Segmented Type Selector (Pengeluaran vs Pemasukan) with custom color indicators.
  - Implemented a Hero Amount Display showing formatted currency with semantic colors.
  - Introduced a horizontal scrollable Category Picker containing the top 8 categories, plus a "Lainnya" option to trigger the full dialog.
  - Created a custom Dompet/Wallet Selector with balance previews and type-based icons.
  - Standardized Date Picker triggering via a premium card design with Calendar icon.
  - Structured sticky bottom action buttons (Batal/Simpan) using custom spacing and design system tokens.
- **Wallet Screen Redesign (Phase 5)**: Overhauled the Akun (Wallet) screen to align with the V2 design system and consolidate settings.
  - Renamed the screen title to "Akun" and redesigned the Wallet Summary card with a premium primary-to-primary-container gradient.
  - Placed prominent filled tonal action buttons ("Transfer Dana" and "Riwayat") directly below the Wallet Summary card.
  - Grouped wallets into collapsible sections by account type (Tunai, Bank, E-Wallet) with dynamic subtotal calculations and expand/collapse chevrons.
  - Modernized wallet list items using `Radius.medium` corners, `surfaceContainerLow` backgrounds, and refined typography.
  - Integrated the full Settings menu directly into the scrollable screen layout below the wallet list, removing the need for a separate settings dialog.
  - Added direct navigation items for Theme Settings, Security (PIN/Biometric), Notifications, Backup & Restore, Receipt Archive, Category Management, and Recycle Bin.
  - Standardized the Floating Action Button shape and spacing to match the new design system tokens.
- **Budget Screen Redesign (Phase 6)**: Redesigned budget health tracking and scannability.
  - Implemented custom overall Circular Progress Gauge color-coded dynamically by usage threshold.
  - Integrated category icons via `CategoryIconHelper.getIconByName` in rounded tinted wrappers.
  - Redesigned active budget items with status badges and thicker M3 progress bars.
- **OCR Review Screen Redesign (Phase 7)**: Overhauled visual presentation and interaction for scanned receipt review.
  - Implemented collapsible image preview card utilizing `AnimatedVisibility` and `ContentScale.Fit`.
  - Added real-time confidence indicator badges (checkmark/warning icons) to Merchant, Date, Wallet, and Category fields.
  - Implemented interactive swipe-to-delete support (`SwipeToDismissBox`) for receipt item cards.
  - Pinned a sticky bottom action bar containing standardized M3 save/dismiss buttons.

## [1.9.0] - 2026-06-10

### Added
- **Navigation Transitions**: Integrated Jetpack Compose `AnimatedContent` API to animate tab switches inside `MainActivity.kt` with a smooth crossfade fade transition (220ms).
- **Accessibility & Performance Auditing**: Confirmed dynamic font scaling support, screen reader content descriptions, contrast verification, and optimized flow-based lazy list loads.

## [1.8.0] - 2026-06-10

### Added
- **Digital Receipt Backup**: Updated `BackupManager.kt` to package digital receipt image files (from `filesDir/receipts` directory) under `receipts/` inside the ZIP backup files.
- **Digital Receipt Restore**: Updated `RestoreManager.kt` to extract and restore the `receipts/` directory during file backup restorations, maintaining visual digital archives across backup cycles.

## [1.7.0] - 2026-06-10

### Added
- **Wallet Summary Card**: Added a total balance overview card showing the combined saldo and the number of active wallets at the top of `AccountListScreen.kt`.
- **Wallet Cards Visual Redesign**: Redesigned wallet list cards to use 16dp rounded corners, circular icon wrapper backgrounds, and currency-formatted balance values.
- **Budget Summary Card**: Integrated a comprehensive summary header at the top of active budgets inside `BudgetListScreen.kt` featuring total budget limit, spent amount, remaining amount, a combined progress indicator, and a budget health status overview (e.g., Safe, Warning, and Over Budget counts).
- **Budget Health/Status Badges**: Added colored status badges (Safe/Aman, Warning, Over Budget) in the top-right corner of each `BudgetProgressCard`.

## [1.6.0] - 2026-06-09

### Added
- **Period & Sort Filter Chips**: Added horizontal scrollable FilterChip rows for period selection ("Semua", "Hari Ini", "Minggu Ini", "Bulan Ini", "Tahun Ini", "Pilih Tanggal") and sorting options ("Terbaru", "Terlama", "Tertinggi", "Terendah") in `TransactionListScreen.kt`.
- **Date Range Picker Dialog**: Integrated native Material 3 `DatePickerDialog` and `DateRangePicker` to support custom date range selection when "Pilih Tanggal" is tapped.
- **Swipe-to-Edit & Swipe-to-Delete**: Wrapped transaction items in the main list with `SwipeToDismissBox`, enabling quick edit (left-to-right swipe) and delete (right-to-left swipe) gestures.
- **Visual Category Icons Consistency**: Added circular category icon displays to list items in both `TransactionListScreen.kt` and `SearchScreen.kt`.

## [1.5.0] - 2026-06-09

### Added
- **Hero Balance Card**: Redesigned and unified the `BalanceCard` to integrate total balance, income, expense, and net difference in one single card.
- **Quick Actions Grid**: Added a new `QuickActionsSection` displaying a 2x2 grid for quick actions (Add Expense, Add Income, Transfer, Scan), and enabled triggering the transfer dialog directly on the Dashboard.
- **Sisa Budget (Remaining Budget)**: Updated the Dashboard active budget cards to display the computed remaining amount ("Sisa Budget") instead of the budget limit.
- **Recent Transactions Icons**: Updated the transaction cards in the Dashboard to show circular category icons on the left of each transaction card.
- **Category Icon joining**: Enhanced `TransactionDao` and mappings to join `category_icon` on transaction details queries, making the icon available for transaction cards and the Recycle Bin.

## [1.4.0] - 2026-06-09

### Added
- **Wallet Type Support**: Added "E-Wallet" as a selectable account type in `AccountFormDialog` and supported rendering the `AccountBalanceWallet` icon for e-wallets in `AccountListScreen`.
- **Transfer Reversion (Undo)**: Exposed `revertTransfer` in the repository layer to refund the sender account (including the admin fee) and deduct the transferred amount from the receiver account, then delete the transfer record.
- **Transfer History Screen**: Created a scrollable `TransferHistoryScreen` rendering a list of transfers, with deep navigation from a history icon button in the `AccountListScreen` TopAppBar.
- **Transfer Detail View**: Created `TransferDetailDialog` displaying transfer details (sender/receiver names, note, fee, date, amount) and allowing users to revert the transfer with a confirmation prompt.
- **Navigation Integration**: Wired centralized navigation for the Transfer History screen in `MainActivity.kt`.

## [1.3.0] - 2026-06-09

### Added
- **Database Seeding**: Room `Callback` seeding that pre-populates the database with 10 default categories (Expense & Income) on creation using raw SQL.
- **Default Categories Recovery**: Room database transaction method `restoreDefaultCategories` in `CategoryDao` to reactivate all default categories, insert missing ones, and clean default category entries from the Recycle Bin.
- **Soft-Delete for Defaults**: Allowed default categories to be soft-deleted in `DeleteCategoryUseCase` and preserved them at the row level by preventing hard-deletion in `RecycleBinRepositoryImpl`.
- **Preset Icon Picker**: A grid icon picker in `CategoryFormDialog` displaying preset Material icons instead of a raw text input field.
- **Searchable Category Picker**: A custom searchable dialog `CategoryPickerDialog` with a search filter field, scrollable icon list, and a direct manage categories button.
- **Navigation Integration**: Added "Kelola Kategori" setting option to settings dialog and integrated back button navigation inside the Category List screen.

## [1.2.0] - 2026-06-09

### Added
- **Design System Tokens**: Created centralized `Spacing.kt`, `Radius.kt`, and `Elevation.kt` containing standardized UI dimension and styling tokens.
- **Theme System**: Persisted the user's theme settings (System, Light, Dark) to DataStore, reactively bound it to `HitungUangTheme`, and implemented a settings dialog selector in `MainActivity`.
- **Universal Recycle Bin & Soft Delete**:
  - Implemented Room database schema migration V2 -> V3 to introduce `is_deleted` and `deleted_at` columns on `categories`, `accounts`, and `budgets` tables.
  - Generalised `RecycleBinDao` to `LEFT JOIN` Category, Wallet, and Budget details dynamically based on entity type.
  - Updated DAO queries to exclude soft-deleted categories, accounts, and budgets from active lists.
  - Restructured `RecycleBinItem` domain model and `RecycleBinMapper` to translate specific entity details into common UI display properties (`title`, `subtitle`, `amountText`, `isExpense`).
  - Added generalized restoration and permanent deletion support for all four entity types in `RecycleBinRepositoryImpl`.
  - Updated `RecycleBinScreen` and `RecycleBinViewModel` to support and render all deleted items uniformly.

## [1.1.0] - 2026-06-09

### Added
- Camera runtime permission check and request flow in `ReceiptScannerScreen`.
- Error handling card with "Coba Lagi" (Retry) button when receipt capture or OCR fails in `ReceiptScannerScreen`.
- State survival on screen rotation for temporary OCR Uri, error messages, loading states, and navigation flags using `rememberSaveable` in `MainActivity` and `ReceiptScannerScreen`.

### Fixed
- **OCR Scan Crash**: Checked coroutine active state (`continuation.isActive`) inside `OcrManagerImpl` before resuming continuation, preventing `IllegalStateException` crashes.
- **Security Flow**: Fixed an issue where changing settings (e.g., toggling hide balance) locked the app by introducing `isFirstCollect` inside `SecurityViewModel` to only trigger lock on cold start.
- **Dashboard Section Duplication**:
  - Removed duplicate `AccountSummaryCard` (Daftar Akun) from the Dashboard screen.
  - Grouped `BalanceCard` (Total Saldo) and `FinancialSummaryInfo` (Pemasukan, Pengeluaran, Selisih) together at the top of the screen to form the unified "Hero Balance Card".
  - Limited recent transactions list count to 5 (down from 10) to match the `UI_SPEC_V2` spec.
- **Visual Bugs & Legend Colors**:
  - Fixed color mapping discrepancy in `ExpenseChartCard` by sorting category entries descending by value in both the Canvas pie-chart and the Legend list.
  - Corrected title icon tint in `InsightCard` to use `onTertiaryContainer` for theme compliance.
