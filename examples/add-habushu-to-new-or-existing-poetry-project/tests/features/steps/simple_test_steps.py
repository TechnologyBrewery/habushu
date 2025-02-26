from behave import when, then  # pylint: disable=no-name-in-module
from add_habushu_to_new_or_existing_poetry_project.helloworld import hello_world
import logging


@when("the main script is executed")
def step_impl(context):
    context.result = hello_world()


@then("a description of the project is returned")
def step_impl(context):
    assert (
        context.result
        == "This is the example project that demonstrates how to add Habushu to a new or existing Poetry project."
    )
