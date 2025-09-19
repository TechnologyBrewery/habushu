@containerizeDependenciesUpdateRequirements
Feature: Test updating requirements.txt file to rewrite path-based dependencies in preparation to containerize Python applications with monorepo dependencies
  The containerization feature requires some pre-processing of the generated requirements.txt files to remove any path-based dependencies.

  Background:
    Given the following requirement file based at "/<workingdir>/target/my-app/":
      """requirements
      # Direct paths to monorepo projects
      /<workingdir>/target/util
      ../util
      ./nested-util
      # option args for install
      -r other-reqs.txt
      -c constraints.txt
      --index-url=https://my-pypi.org/simple/
      --extra-index-url=my-pvt-index
      #Blank lines should be ignored

      # Editable monorepo projects
      -e /<workingdir>/target/editable-util
      -e ../editable-relative-util
      -e ./editable-nested-util
      # URI-based monorepo projects
      file:///<workingdir>/target/my-app/nested-util
      # named URI-based monorepo projects
      util @ file://localhost/<workingdir>/target/util
      other-util@file:///<workingdir>/target/other-util
      util [extraopt] \
         @ file:///<workingdir>/target/util
      # Package name with pinned version constraint
      util==1.2.3
      # Packages from web
      lib @ https://nightly-build/package/lib
      https://remote-wheelhouse/direct-package.whl
      # Random extra wheel checked into project
      ../extra-packages/leftpad-1.0.0-py3-none-any.whl
      """

  Scenario: Parse through given requirements.txt file and return all path-based requirements
    When path-based requirements are retrieved
    Then the following paths are returned:
      | /<workingdir>/target/util                                          |
      | /<workingdir>/target/other-util                                    |
      | /<workingdir>/target/my-app/nested-util                            |
      | /<workingdir>/target/extra-packages/leftpad-1.0.0-py3-none-any.whl |
      | /<workingdir>/target/editable-util                                 |
      | /<workingdir>/target/editable-relative-util                        |
      | /<workingdir>/target/my-app/editable-nested-util                   |

  Scenario: Rewrites <package-manager> path-based requirements in requirements.txt file
    Given the following path mappings:
      | /<workingdir>/target/util                                          | ${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl                   |
      | /<workingdir>/target/other-util                                    | ${WHEEL_HOUSE}/other-util-1.0.0-py3-none-any.whl             |
      | /<workingdir>/target/my-app/nested-util                            | ${WHEEL_HOUSE}/nested_util-1.0.0-py3-none-any.whl            |
      | /<workingdir>/target/extra-packages/leftpad-1.0.0-py3-none-any.whl | ${WHEEL_HOUSE}/leftpad-1.0.0-py3-none-any.whl                |
      | /<workingdir>/target/editable-util                                 | ${WHEEL_HOUSE}/editable_util-1.0.0-py3-none-any.whl          |
      | /<workingdir>/target/editable-relative-util                        | ${WHEEL_HOUSE}/editable_relative_util-1.0.0-py3-none-any.whl |
      | /<workingdir>/target/my-app/editable-nested-util                   | ${WHEEL_HOUSE}/editable_nested_util-1.0.0-py3-none-any.whl   |
    When wheels are relocated to staging directory
    Then the requirements file is updated to:
      """requirements
      # Direct paths to monorepo projects
      file://${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl
      file://${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl
      file://${WHEEL_HOUSE}/nested_util-1.0.0-py3-none-any.whl
      # option args for install
      -r other-reqs.txt
      -c constraints.txt
      --index-url=https://my-pypi.org/simple/
      --extra-index-url=my-pvt-index
      #Blank lines should be ignored

      # Editable monorepo projects
      file://${WHEEL_HOUSE}/editable_util-1.0.0-py3-none-any.whl
      file://${WHEEL_HOUSE}/editable_relative_util-1.0.0-py3-none-any.whl
      file://${WHEEL_HOUSE}/editable_nested_util-1.0.0-py3-none-any.whl
      # URI-based monorepo projects
      file://${WHEEL_HOUSE}/nested_util-1.0.0-py3-none-any.whl
      # named URI-based monorepo projects
      util @ file://${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl
      other-util@file://${WHEEL_HOUSE}/other-util-1.0.0-py3-none-any.whl
      util [extraopt] \
         @ file://${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl
      # Package name with pinned version constraint
      util==1.2.3
      # Packages from web
      lib @ https://nightly-build/package/lib
      https://remote-wheelhouse/direct-package.whl
      # Random extra wheel checked into project
      file://${WHEEL_HOUSE}/leftpad-1.0.0-py3-none-any.whl
      """

    Scenario: Relocated paths include hashes if hashes are already present
      Given the following requirement file based at "/<workingdir>/target/my-app/":
      """requirements
      ../util
      aiohappyeyeballs==2.6.1 ; python_full_version >= "3.11.4" and python_version < "4" \
          --hash=sha256:c3f9d0113123803ccadfdf3f0faa505bc78e6a72d1cc4806cbd719826e943558 \
          --hash=sha256:f349ba8f4b75cb25c99c5c2d84e997e485204d2902a9597802b0371f09331fb8
      """
      And the following path mappings:
        | /<workingdir>/target/util   | ${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl |
      When wheels are relocated to staging directory
      Then the requirements file is updated to:
      """requirements
      file://${WHEEL_HOUSE}/util-1.0.0-py3-none-any.whl \
          --hash=sha256:2706220a770e6d738e912661946add633669b76637e1b85c9d9d142df5890094
      aiohappyeyeballs==2.6.1 ; python_full_version >= "3.11.4" and python_version < "4" \
          --hash=sha256:c3f9d0113123803ccadfdf3f0faa505bc78e6a72d1cc4806cbd719826e943558 \
          --hash=sha256:f349ba8f4b75cb25c99c5c2d84e997e485204d2902a9597802b0371f09331fb8
      """
