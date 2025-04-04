from behave import when, then  # pylint: disable=no-name-in-module
from habushu_poetry_package.reusable_module.worker import SubWorker


@when("I reference a src file that has references to other src files")
def step_impl(context):
    subworker = SubWorker()
    context.result = subworker.do_something()


@then("the build can successfully resolve the nested imports")
def step_impl(context):
    assert len(context.result) > 0
