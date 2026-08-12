# KawaiiPB Roadmap

Last updated: 2026-08-12

This document captures the prioritized improvement plan for the app.

## Phase 1. Critical Bugs

Goal: fix issues that can block users or break the flow.

- Priority: Highest
- Complexity: Medium
- Risk: High
- Impact: Immediate stability improvement
- Examples:
  - Camera failures
  - Printing failures
  - Null-state crashes
  - Incorrect navigation transitions

## Phase 2. Architecture Refactoring

Goal: reduce coupling and split oversized files without changing behavior.

- Priority: High
- Complexity: High
- Risk: Medium
- Impact: Lower maintenance cost and easier testing
- Examples:
  - Split `FlowScreen.kt`
  - Move reusable UI helpers into focused files
  - Extract shared ViewModel utilities

## Phase 3. Performance Improvements

Goal: reduce unnecessary work in image-heavy and Compose-heavy paths.

- Priority: High
- Complexity: Medium
- Risk: Low
- Impact: Better responsiveness and lower memory churn
- Examples:
  - Cache decoded bitmaps
  - Avoid repeated image scaling
  - Reduce redundant preview loading

## Phase 4. Code Cleanup

Goal: remove dead code, commented blocks, and duplicate helpers.

- Priority: Medium
- Complexity: Medium
- Risk: Low
- Impact: Smaller files and clearer ownership
- Examples:
  - Remove legacy commented blocks
  - Delete unused helpers
  - Normalize imports

## Phase 5. UI Improvements

Goal: improve spacing, hierarchy, and interaction clarity.

- Priority: Medium
- Complexity: Medium
- Risk: Medium
- Impact: Better operator experience
- Examples:
  - Tighter layout consistency
  - Better loading states
  - Improved feedback for capture and assignment

## Phase 6. Feature Improvements

Goal: expand functionality where it adds user value.

- Priority: Medium
- Complexity: Variable
- Risk: Medium
- Impact: More useful kiosk workflow
- Examples:
  - Better template selection
  - Smarter photo assignment
  - Richer preview controls

## Phase 7. Future Scalability

Goal: prepare the app for growth, reuse, and maintainability.

- Priority: Lower, but ongoing
- Complexity: High
- Risk: Medium
- Impact: Long-term sustainability
- Examples:
  - Stronger abstraction boundaries
  - Test-friendly architecture
  - Better package/module separation

## Notes

- All phases should preserve current behavior unless explicitly approved.
- Large refactors should be split into small, testable steps.
- Every change should be followed by build and unit-test verification.
