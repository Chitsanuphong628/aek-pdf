# AEK PDF — AI Agent System & Architecture Guide

This guide is written specifically for AI coding agents (and human contributors) interacting with, extending, or maintaining the **AEK PDF** codebase. It defines architectural principles, strict UX rules, memory safety guardrails, and implementation conventions.

---

## 1. Project Philosophy & Target Audience

- **Target User**: Everyday consumers, professionals, and elderly family members who need to combine scanned paper documents into PDFs easily.
- **Core Value Proposition**: 100% offline, privacy-first, zero ads, zero subscriptions, zero watermarks.
- **Tone & Aesthetics**: Clean, professional Apple Minimalist design (Human Interface Guidelines inspired).
- **Anti-AI Slop Mandate**: **ABSOLUTELY NO EMOJIS** in UI strings, toasts, button labels, logs, hints, or documentation. Emoji decorations (such as lightbulbs, sparkles, rockets) degrade user trust and give the product an amateurish, synthetic AI feel.

---

## 2. Strict UX & UI Design System

### Apple Minimalist Palette (`com.aekpdf.app.core.ui.theme.Color.kt`)
- `AppleCanvas`: `#F2F2F7` (System grouped background)
- `AppleCardSurface`: `#FFFFFF` (Pure white card background)
- `AppleBlue`: `#007AFF` (Primary interactive color)
- `AppleBlueTint`: `#EBF5FF` (Secondary pill / button container)
- `AppleRed`: `#FF3B30` (Destructive action / delete swipe background)
- `AppleGreen`: `#34C759` (Status badges)
- `LineGreen`: `#06C755` (Dedicated one-tap LINE share button)
- `AppleTextPrimary`: `#1C1C1E` (High-contrast typography)
- `AppleTextSecondary`: `#8E8E93` (Subtitles, metadata)
- `AppleHairlineBorder`: `#E5E5EA` (0.5dp subtle boundaries)

### Dynamic Color Policy
- `dynamicColor` is strictly set to `false` in `AekPdfTheme` (`Theme.kt`). Do NOT re-enable wallpaper-based Material You dynamic color, as it pollutes the clean Apple aesthetic with muted, muddy tones.

### Card Elevation & Hairline Styling
- Always use `Modifier.appleCard()` for elevated surfaces:
  ```kotlin
  fun Modifier.appleCard(
      cornerRadius: Dp = 18.dp,
      elevation: Dp = 2.dp,
      backgroundColor: Color = AppleCardSurface,
      borderColor: Color = AppleHairlineBorder
  ): Modifier
  ```
- This applies a 0.5dp hairline border combined with low-opacity ambient drop shadows (`Color.Black.copy(alpha = 0.05f)`), ensuring distinct separation from `#F2F2F7` without dark drop shadows.

### Gesture Interactions on Document Cards (`ImageToPdfScreen.kt`)
1. **Swipe to Delete**:
   - Implemented via `SwipeToDismissBox` (`EndToStart`).
   - Cards must NEVER show persistent trash can icons on their visible surface.
   - Swiping left smoothly reveals the `AppleRed` background with the white delete icon and "ลบหน้านี้".
   - Positional threshold is calibrated to 35% of card width (`totalDistance * 0.35f`).
2. **Drag & Drop Reordering**:
   - Implemented via a dedicated drag handle `Icon(Icons.Default.DragHandle)`.
   - The handle's `pointerInput` MUST be keyed to `pageId` (not index) to prevent gesture cancellation during recomposition when page positions swap.
   - When dragged, the card elevates to `elevation = 8.dp` and `scale = 1.03x` with smooth spring animation.

---

## 3. Critical Memory & Android PDF Rules

### The Streaming Rule
Document scans are often taken on 48MP–108MP smartphone cameras (8000x6000 px). Loading multiple raw bitmaps into memory simultaneously WILL cause an `OutOfMemoryError` (OOM).

When modifying PDF generation code (`StreamingPdfCreator.kt`):
- **NEVER** decode all pages into a `List<Bitmap>`.
- Always process pages **one at a time**: decode -> scale -> draw to `PdfDocument.Page` -> write -> recycle bitmap -> proceed to next page.
- Always use single-pass subsampling with `inSampleSize` calculated from target canvas bounds.

### BitmapFactory Gotcha
In `BitmapFactory.decodeStream(..., inJustDecodeBounds = true)`, Android SDK **always returns null** because bounds are queried without allocating bitmap memory.
Do NOT write code like:
```kotlin
// WRONG: will always trigger the Elvis operator!
openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
```
Instead, verify stream non-null first, then execute `use`:
```kotlin
// CORRECT:
val stream = context.contentResolver.openInputStream(uri) ?: return null
stream.use { BitmapFactory.decodeStream(it, null, options) }
```

### EXIF Orientation
Document photos frequently contain EXIF rotation tags (90, 180, 270 degrees). `ImageProcessor.kt` must always read `ExifInterface` and apply proper rotation before drawing to PDF canvas.

---

## 4. Codebase Architecture Map

```
com.aekpdf.app/
├── AekPdfApplication.kt                     # Application initialization, Room Database singleton
├── MainActivity.kt                          # Compose Navigation Graph & Share-Intent Receiver
├── core/
│   ├── database/
│   │   ├── AppDatabase.kt                   # Room DB definition
│   │   ├── PdfHistoryDao.kt                 # Insert, query, delete history
│   │   └── PdfHistoryEntity.kt              # History record entity
│   ├── model/
│   │   └── Models.kt                        # DocumentPage, PdfExportConfig, PageFormat, ExportQuality, PageFilter
│   ├── pdf/
│   │   ├── StreamingPdfCreator.kt           # OOM-safe PDF Document generator
│   │   └── ImageProcessor.kt                # EXIF rotation, subsampling, grayscale/B&W filters
│   ├── storage/
│   │   └── PdfStorageManager.kt             # Android FileProvider and Scoped Storage share intents
│   └── ui/theme/
│       ├── Color.kt                         # Apple System Palette tokens
│       ├── Theme.kt                         # AekPdfTheme definition (dynamicColor = false)
│       ├── Type.kt                          # Typography definitions
│       └── AppleModifier.kt                 # appleCard, appleSquircleBadge, appleSquircleGradientBadge
└── features/
    ├── home/
    │   └── HomeScreen.kt                    # Hub screen & future feature roadmap
    ├── imagetopdf/
    │   ├── ImageToPdfScreen.kt              # Clean list, Thai name presets, SwipeToDismiss, DragHandle
    │   └── ImageToPdfViewModel.kt           # State management, movePage, generatePdf
    ├── viewer/
    │   ├── PdfPreviewScreen.kt              # Instant preview, PDF rendering, direct LINE sharing
    │   └── PdfPreviewViewModel.kt
    └── history/
        ├── HistoryScreen.kt                 # Saved PDFs list, date formatting, re-share, delete
        └── HistoryViewModel.kt
```

---

## 5. Extensibility Roadmap & Adding New Features

The codebase is built with modular clean architecture to support future expansion:

1. **PDF Merge (`features/pdfmerge`)**:
   - Reuse `PdfStorageManager` and `PdfHistoryDao`.
   - Use Android native `PdfRenderer` to unpack pages or merge through native canvas streams.
2. **PDF Compress (`features/pdfcompress`)**:
   - Render pages at lower DPI/subsampling and re-encode with configurable JPEG quality.
3. **Camera Document Scanner (`play-services-mlkit-document-scanner`)**:
   - The ML Kit dependency (`16.0.0`) is already included in `build.gradle.kts`.
   - Trigger via `GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).build()`.

---

## 6. Verification & Quality Gates

Before committing any changes, AI agents must execute and verify the following commands:

```bash
# 1. Run all unit tests
./gradlew testDebugUnitTest

# 2. Build debug APK
./gradlew assembleDebug

# 3. Check for unauthorized emojis
python3 -c "
import os
for root, dirs, files in os.walk('app/src/main'):
    for f in files:
        if f.endswith(('.kt', '.xml')):
            with open(os.path.join(root, f), 'r', encoding='utf-8') as fp:
                for idx, line in enumerate(fp, 1):
                    for ch in line:
                        if 0x1F300 <= ord(ch) <= 0x1FAFF or 0x2600 <= ord(ch) <= 0x27BF:
                            raise SystemExit(f'Emoji found at {f}:{idx} -> {ch}')
print('Codebase clean of emojis.')
"
```
