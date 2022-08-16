from behave import *
from test_config import TestConfig


@when("the Krausening-managed configuration is loaded")
def step_impl(context):
    context.krausening_config = TestConfig()


@then("the default Krausening profile is loaded")
def step_impl(context):
    assert not context.krausening_config.integration_test_enabled()


@then("the integration-test Krausening profile is loaded")
def step_impl(context):
    assert context.krausening_config.integration_test_enabled()
