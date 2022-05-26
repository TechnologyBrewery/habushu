from krausening_python.properties import PropertyManager


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
        integration_test_enable = False
        integration_enable_str = self.properties["integration.test.enabled"]
        if integration_enable_str:
            integration_test_enable = integration_enable_str == "True"
        return integration_test_enable

    def reload(self):
        self.properties = PropertyManager.get_instance().get_properties(
            "test.properties"
        )
