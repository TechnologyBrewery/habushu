from behave import when, then  # pylint: disable=no-name-in-module
from habushu_uv_add_to_project.helloworld import hello_world
import logging


@when("the main script is executed")
def step_impl(context):
    context.result = hello_world()


@then("a description of the project is returned")
def step_impl(context):
    assert (
        context.result
        == "This is the example project that demonstrates how to add Habushu to a new or existing uv project."
    )
