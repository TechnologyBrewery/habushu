from behave import when, then  # pylint: disable=no-name-in-module
from habushu_running_python_scripts.helloworld import generate_random_string
from habushu_running_python_scripts.generated import person_pb2
import logging


@when("I reference a generated src file in my test file")
def step_impl(context):
    logging.info("Referencing a src file...")
    context.random = generate_random_string(5)
    person = person_pb2.Person()  # pylint: disable=no-member
    person.email = "habushu@gmail.com"
    context.person = person


@then("the build can successfully resolve the imports")
def step_impl(context):
    assert len(context.random) == 5
    assert context.person.email == "habushu@gmail.com"
