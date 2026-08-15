# KawaiiPB Roadmap

Last updated: 2026-08-15

This roadmap reflects the current project reality and the next most valuable improvements for the app.

## Priority 1: Reduce flow complexity

Goal: make the kiosk flow easier to reason about and safer to refactor.

- Focus: `feature/flow/presentation`
- Why: this is the most state-heavy and risk-heavy part of the app
- Planned work:
  - split large flow stage logic into clearer sections
  - separate pure state transitions from UI code
  - reduce duplicate callback and transform logic

## Priority 2: Improve regression protection

Goal: prevent subtle behavior regressions during refactoring.

- Focus: capture, assignment, template, and print behavior
- Why: these are user-visible flows with many edge cases
- Planned work:
  - add targeted tests around state transitions
  - validate asset and layout resolution
  - verify print output generation after flow changes

## Priority 3: Align docs with actual assets

Goal: keep schema and documentation accurate with the real JSON files in the repo.

- Focus: `docs/json-schemas/` and `app/src/main/assets/`
- Why: asset contracts can drift if the UI changes without updating docs
- Planned work:
  - document the template manifest contract precisely
  - keep layout docs aligned with current layout files
  - validate asset filenames and keys against actual files

## Priority 4: Structural cleanup

Goal: continue reducing code duplication and selective coupling without broad architectural churn.

- Focus: shared UI and helper logic
- Why: small cleanup steps are lower risk than full rewrites
- Planned work:
  - reduce duplication in flow helpers
  - keep service boundaries clear
  - preserve the existing architecture until major refactor is justified

## Priority 5: Feature polish

Goal: improve usability and maintainability of the retail kiosk experience.

- Focus: session UX, template selection, and admin ergonomics
- Planned work:
  - improve clarity and consistency of the flow stages
  - reduce friction in template and layout selection
  - improve operator feedback and error states

## Notes

- The current app build is verified to compile successfully.
- The flow feature remains the primary refactor target.
- A full architecture rewrite is not recommended before the flow layer is stabilized and covered by tests.
