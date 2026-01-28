Feature: Poetry package version managed by Habushu
  Habushu will automatically manage the package version within the pyproject.toml file to match that of the POM file.

  Scenario Outline: SemVer 2 POM versions are translated to their PEP 440 equivalent
    Given a Habushu module with a POM version of "<semver>"
    When the POM version is translated to a PEP 440 version
    Then the pyproject.toml file is updated with the version "<pep440>"

    Examples:
      | semver        | pep440       | comment                  |
      | 1.2.3         | 1.2.3        | final release            |
      | 1.2.3-rc.4    | 1.2.3rc4     | semver 2 rc with dot     |
      | 1.2.3-rc4     | 1.2.3rc4     | semver 1 rc lowercase    |
      | 1.2.3-RC4     | 1.2.3rc4     | semver 1 rc uppercase    |
      | 1.2.3-alpha.4 | 1.2.3a4      | semver 2 alpha with dot  |
      | 1.2.3-alpha4  | 1.2.3a4      | semver 1 alpha lowercase |
      | 1.2.3-ALPHA4  | 1.2.3a4      | semver 1 alpha uppercase |
      | 1.2.3-beta.4  | 1.2.3b4      | semver 2 beta with dot   |
      | 1.2.3-beta4   | 1.2.3b4      | semver 1 beta lowercase  |
      | 1.2.3-BETA4   | 1.2.3b4      | semver 1 beta uppercase  |
      | 1.2.3.4-rc1   | 1.2.3.4rc1   | four segment with rc     |
      | 1.2.3.4-alpha1| 1.2.3.4a1    | four segment with alpha  |
      | 1.2.3.4-beta1 | 1.2.3.4b1    | four segment with beta   |

  Scenario: Maven Snapshot POM versions are translated to their PEP 440 equivalent
    Given a Habushu module with a POM version of "1.2.3-SNAPSHOT"
    When the POM version is translated to a PEP 440 version
    Then the pyproject.toml file is updated with the version "1.2.3.dev"

  Scenario Outline: Pre-release with SNAPSHOT suffix versions are translated to their PEP 440 equivalent
    Given a Habushu module with a POM version of "<semver>"
    When the POM version is translated to a PEP 440 version
    Then the pyproject.toml file is updated with the version "<pep440>"

    Examples:
      | semver               | pep440         | comment                      |
      | 1.2.3-rc1-SNAPSHOT   | 1.2.3rc1.dev   | rc with snapshot             |
      | 1.2.3-alpha1-SNAPSHOT| 1.2.3a1.dev    | alpha with snapshot          |
      | 1.2.3-beta1-SNAPSHOT | 1.2.3b1.dev    | beta with snapshot           |
      | 1.2.3.4-rc1-SNAPSHOT | 1.2.3.4rc1.dev | four segment rc with snapshot|