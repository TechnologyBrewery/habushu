Feature: Referrences to Pylint and Black are removed

  Scenario: Pylint and Black referrences are removed
    Given a project exists that referrences pylint and black
    When the remove-pylint-black-migration migration runs
    Then pylint and black referrences are removed from pyproject.toml

  Scenario: Python files have Pylint comments that need to be removed
    Given a python file exists that has pylint comments
    When the remove-pylint-comments-migration migration runs
    Then pylint comments are removed from python files
