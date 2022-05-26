from behave import *
from habushu_mixology.reusable_module.worker import SubWorker
from habushu_mixology.helloworld import generate_random_string


@when("I reference a src file in my test file")
def step_impl(context):
    context.random = generate_random_string(5)


@when("I reference a src file that has references to other src files")
def step_impl(context):
    subworker = SubWorker()
    context.result = subworker.do_something()


@then("the build can successfully execute the tests")
def step_impl(context):
    assert len(context.random) == 5


@then("the build can successfully resolve the imports")
def step_impl(context):
    assert len(context.result) > 0
