from behave import *
from src.main.python.helloworld import generate_random_string

@when(u'I reference a src file in my test file')
def step_impl(context):
    context.random = generate_random_string(5)


@then(u'the build can successfully execute the tests')
def step_impl(context):
    assert (len(context.random) == 5)