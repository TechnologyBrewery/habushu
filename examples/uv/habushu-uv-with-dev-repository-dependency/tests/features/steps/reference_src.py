from behave import when, then  # pylint: disable=no-name-in-module
from habushu_uv_with_dev_repository_dependency.reusable_module.worker import SubWorker
from habushu_uv_with_dev_repository_dependency.helloworld import generate_random_string
import logging


@when("I reference a src file in my test file")
def step_impl(context):
    logging.info("Referencing a src file...")
    context.random = generate_random_string(5)


@when("I reference a src file that has references to other src files")
def step_impl(context):
    subworker = SubWorker()
    context.result = subworker.do_something()


@then("the build can successfully resolve the imports")
def step_impl(context):
    assert len(context.random) == 5


@then("the build can successfully resolve the nested imports")
def step_impl(context):
    assert len(context.result) > 0
