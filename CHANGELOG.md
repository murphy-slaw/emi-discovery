# Changelog for [EMIDiscovery](https://github.com/murphy-slaw/emi-discovery)

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.8] 2026-07-12

### Fixed
- InventoryChanged mixin now properly loaded on both sides on Forge
- Declared dependency on Cloth Config

## [1.1.7] 2026-07-11

### Fixed
- Cache displayed stacks intelligently to reduce CPU overhead in GUIs
- Properly declare dependencies

## [1.1.6-1] 2026-04-14

### Fixed

Fixed build issue causing mixin remapping issues on forge.

## [1.1.6] 2026-03-16

### Changed
- Catch and log NPE in isCraftable as a workaround while continuing to investigate.

## [1.1.5] 2026-03-15

### Fixed
- Catch and log NPE explicitly in problematic cache access

## [1.1.4] 2026-03-15

### Fixed
- Handle unexpected cache exceptions gracefully

## [1.1.3] 2026-03-12

### Fixed

- Fixed a remapping issue with ingredient tooltips.

## [1.1.2] 2026-02-28

### Changed

- Added a cache for expensive item visibility calculations to improve index rendering performance.

## [1.1.1] 2026-02-27

### Fixed

- Fixed mixin remapping issue in RecipeScreenMixin causing a crash.

## [1.1.0] 2026-02-27

### Added

- Added a config option to show craftable but undiscovered items in the Index as well as the Craftable panel.
- Added a config option to toggle filtering for recipes with undiscovered workstations.

## [1.0.1] 2026-02-23

### Fixed

- Fixed a mixin-related crash when loading some recipe pages.

## [1.0.0] 2026-02-22

Initial public release.