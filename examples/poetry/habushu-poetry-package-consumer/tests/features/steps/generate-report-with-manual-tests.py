from behave import given, when, then  # pylint: disable=no-name-in-module


@given("a Python module with at least one automated test and at least one manual test")
def step_impl(context):
    return


@when("I build the python module")
def step_impl(context):
    return


@then("my build should succeed")
def step_impl(context):
    return


@then("I should be able to see a HTML report of the tests in the target directory")
def step_impl(context):
    return
