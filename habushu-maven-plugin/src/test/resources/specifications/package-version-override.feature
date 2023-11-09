Feature: Poetry package version managed by Habushu
  Habushu will automatically manage the package version within the pyproject.toml file to match that of the POM file.

  Scenario Outline: SemVer 2 POM versions are translated to their PEP 440 equivalent
    Given a Habushu module with a POM version of "<semver>"
    When the POM version is translated to a PEP 440 version
    Then the pyproject.toml file is updated with the version "<pep440>"

    Examples:
      | semver        | pep440       | comment        |
      | 1.2.3         | 1.2.3        | final release  |
      | 1.2.3-rc.4    | 1.2.3rc4     | semver 2 rc    |
      | 1.2.3-rc4     | 1.2.3-rc4    | semver 1 rc    |
      | 1.2.3-alpha.4 | 1.2.3alpha4  | semver 2 alpha |
      | 1.2.3-alpha4  | 1.2.3-alpha4 | semver 1 alpha |
      | 1.2.3-beta.4  | 1.2.3beta4   | semver 2 beta  |
      | 1.2.3-beta4   | 1.2.3-beta4  | semver 1 beta  |

  Scenario: Maven Snapshot POM versions are translated to their PEP 440 equivalent
    Given a Habushu module with a POM version of "1.2.3-SNAPSHOT"
    When the POM version is translated to a PEP 440 version
    Then the pyproject.toml file is updated with the version "1.2.3.dev"