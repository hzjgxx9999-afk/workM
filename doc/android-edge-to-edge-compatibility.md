# Android Window Compatibility Baseline

## Mandatory rules for new full-screen business pages

1. Full-screen business `Activity` must extend `EdgeToEdgeActivity`.
2. Call `applyEdgeToEdge()` immediately after `setContentView()`.
3. Give the top app bar, bottom action bar, and floating action button stable IDs so their system-bar insets can be applied.
4. Do not use a platform `Spinner` for a branded filter control. Use an app-controlled Material view with a popup menu or a Material exposed dropdown.
5. Do not use horizontal `layout_weight` for responsive filter rows. Use `ConstraintLayout` with explicit start/end constraints and a fixed or minimum action width.
6. Empty/loading states must share the content area with the list and be constrained above any floating action button.

## Validation matrix

- API 26, API 34, API 35, and API 36.
- Gesture navigation and three-button navigation.
- Cutout and non-cutout displays.
- Widths 320dp, 360dp, 393dp, 411dp, and 480dp.
- Font scales 0.85, 1.0, 1.15, and 1.3.

## Migration status

- Migrated business pages: dispatch list, dispatch create, dispatch detail, and spot-check review.
- Migrated special pages: launch activity uses safe content insets; dispatch photo preview uses a top control overlay and light-on-dark system-bar icons.
