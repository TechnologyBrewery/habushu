@repositoryHandling
Feature: Python Repository handling

  Scenario: Default repository urls
    Given a repository URL of "https://myrepo.org"
    Then the search index URL is "https://myrepo.org/simple/"
    Then the publish URL is "https://myrepo.org/simple/"

  Scenario Outline: Repository search index URL
    Given a repository URL of "<baseUrl>"
    And a search index path of "<index>"
    Then the search index URL is "<url>"

    Examples:
      | baseUrl               | index    | url                          |
      | https://pvt-repo.org  | search   | https://pvt-repo.org/search/ |
      | https://pvt-repo.org/ | search   | https://pvt-repo.org/search/ |
      | https://pvt-repo.org  | search/  | https://pvt-repo.org/search/ |
      | https://pvt-repo.org/ | /search/ | https://pvt-repo.org/search/ |
      | https://pvt-repo.org  |          | https://pvt-repo.org/        |
      | https://pvt-repo.org/ |          | https://pvt-repo.org/        |
      | https://pvt-repo.org  | null     | https://pvt-repo.org/        |

  Scenario Outline: Repository publish URL
    Given a repository URL of "<baseUrl>"
    And a publish path of "<publish>"
    Then the publish URL is "<url>"

    Examples:
      | baseUrl               | publish  | url                          |
      | https://pvt-repo.org  | upload   | https://pvt-repo.org/upload/ |
      | https://pvt-repo.org/ | upload   | https://pvt-repo.org/upload/ |
      | https://pvt-repo.org  | upload/  | https://pvt-repo.org/upload/ |
      | https://pvt-repo.org/ | /upload/ | https://pvt-repo.org/upload/ |
      | https://pvt-repo.org  |          | https://pvt-repo.org/        |
      | https://pvt-repo.org/ |          | https://pvt-repo.org/        |
      | https://pvt-repo.org  | null     | https://pvt-repo.org/        |

  Scenario Outline: Equality
    Given a repository URL of "<baseUrl1>"
    And a search index path of "<index1>"
    And a publish path of "<publish1>"
    And another repository with "<baseUrl2>", "<index2>", and "<publish2>"
    Then they are equal

    Examples:
      | baseUrl1              | index1 | publish1 | baseUrl2              | index2 | publish2 |
      | https://pvt-repo.org  | search | upload   | https://pvt-repo.org  | search | upload   |
      | https://pvt-repo.org/ | search | upload   | https://pvt-repo.org  | simple | legacy   |
      | https://pvt-repo.org  |        | null     | https://pvt-repo.org/ | search | upload   |
      
