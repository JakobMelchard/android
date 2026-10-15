# Changelog

## [0.1.2](https://github.com/JakobMelchard/fz/compare/v0.1.1...v0.1.2) (2026-10-15)


### Bug Fixes

* **release:** sign with the keystore's only key when no alias is set ([#43](https://github.com/JakobMelchard/fz/issues/43)) ([b609bd1](https://github.com/JakobMelchard/fz/commit/b609bd14eefc483601d93ff3eec3d240c66da0b0))

## [0.1.1](https://github.com/JakobMelchard/fz/compare/v0.1.0...v0.1.1) (2026-10-15)


### Features

* hide Gboard suggestion strip on the query field ([#7](https://github.com/JakobMelchard/fz/issues/7)) ([00d1a54](https://github.com/JakobMelchard/fz/commit/00d1a54a7a4053a55edcdd418b954e7d52a5bcf5))
* initial fz launcher ([2e108c2](https://github.com/JakobMelchard/fz/commit/2e108c298ceb838b09ce15c1d6f56765f5ced587))
* scroll the full app list instead of the top 8 ([#42](https://github.com/JakobMelchard/fz/issues/42)) ([e002516](https://github.com/JakobMelchard/fz/commit/e002516023c0584ef0e9506a2c096db635c490f8))


### Bug Fixes

* clamp frecency age when the clock moved back ([#39](https://github.com/JakobMelchard/fz/issues/39)) ([4cce453](https://github.com/JakobMelchard/fz/commit/4cce4536575ffccf809b1881fdbe4d0f6f59dfeb))
* guard launch, parse usage defensively, rank once per query ([#29](https://github.com/JakobMelchard/fz/issues/29)) ([a22e455](https://github.com/JakobMelchard/fz/commit/a22e455995c87ea9cdf1c9be3491c06eae46aa07))
* keep usage history when the app list comes back empty ([#41](https://github.com/JakobMelchard/fz/issues/41)) ([47b68e2](https://github.com/JakobMelchard/fz/commit/47b68e2186f3c7f054b45ce7a5cdb81f12c9135b))
* load the app list off the main thread on start and package change ([#35](https://github.com/JakobMelchard/fz/issues/35)) ([1c06573](https://github.com/JakobMelchard/fz/commit/1c06573b2937e042243c2a645fa17c08500ffc86))
