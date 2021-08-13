from behave import *
from helloworld import generate_random_string
import os

test_data_file = 'test-example-resources.txt'

@when(u'I reference a test resource in my test file')
def step_impl(context):
    context.random = generate_random_string(5)
    print(os.getcwd())
    with open(test_data_file) as f:
        assert ("Stirred, not shaken" == f.read())


## "Then" step for this test is defined in reference_src