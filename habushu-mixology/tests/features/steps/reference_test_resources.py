from behave import *


@when("I reference a test resource in my test file")
def step_impl(context):
    test_data_file = "tests/resources/test-example-resources.txt"
    with open(test_data_file) as f:
        context.test_data_file_contents = f.read()


@then("the test resource may be successfully opened")
def step_impl(context):
    assert "Stirred, not shaken" == context.test_data_file_contents
