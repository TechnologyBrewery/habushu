# Major Additions

- Support for [uv](https://docs.astral.sh/uv/)
- Support for [Poetry](https://python-poetry.org) 2+

# Breaking Changes
- `exportRequirementsWithoutPathDependencies` has been deprecated. Use the `containerize-dependencies` goal instead. 
- `black` and `pylint` have been replaced with `ruff`.

# How to Upgrade
- Update the `habushu-maven-plugin` `<version>` configuration to `1.3.0`.

## Automatic Upgrades
- Poetry version 2+ migrations

## Manual Upgrades
If you would like Habushu's linting functionality to mimic the pre-3.0.0 behavior, we recommend updating your Ruff lint configurations to ignore "E4","E7" and "F". See Ruff's [configuration](https://docs.astral.sh/ruff/configuration/) docs for additional details.

# What's Changed
_to be auto-generated when published_