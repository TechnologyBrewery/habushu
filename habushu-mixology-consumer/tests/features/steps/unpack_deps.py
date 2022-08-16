from behave import *
from habushu_mixology_consumer.nested.simple_dependency import (
    call_worker_from_nested_dir,
)


@when("I reference a dependency in my python code")
def step_impl(context):
    context.result = call_worker_from_nested_dir()


@then("the build can successfully execute the tests")
def step_impl(context):
    print(context.result)
    assert "nested so useful!" == context.result
