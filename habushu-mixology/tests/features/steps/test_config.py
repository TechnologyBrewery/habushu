from krausening.properties import PropertyManager


class TestConfig:
    """
    Configurations utility class for being able to read in and reload properties
    """

    def __init__(self):
        self.properties = None
        self.reload()

    def integration_test_enabled(self):
        """
        Returns whether the integration tests are enabled or not
        """
        return "True" == self.properties["integration.test.enabled"]

    def reload(self):
        self.properties = PropertyManager.get_instance().get_properties(
            "test.properties"
        )
