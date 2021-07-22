from behave import *
from helloworld import generate_random_string
from habushu.reusable_module.worker import SubWorker

@when(u'I reference a src file in my test file')
def step_impl(context):
    context.random = generate_random_string(5)

@when(u'I reference a src file that has references to other src files')
def step_impl(context):
    subworker = SubWorker()
    context.result = subworker.do_something()

@then(u'the build can successfully execute the tests')
def step_impl(context):
    assert (len(context.random) == 5)

@then(u'the build can successfully resolve the imports')
def step_impl(context):
    assert(len(context.result) > 0)