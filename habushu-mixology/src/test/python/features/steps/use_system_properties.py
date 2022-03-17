from behave import *
import unittest
import os
from config.test_config import TestConfig


@given(u'a system property is set in the pom')
def step_impl(context):
    setConfig(context)

@given(u'a system property is set in a profile')
def step_impl(context):
    setConfig(context)

@when(u'the behave tests are run')
def step_impl(context):
    context.krausening_base=os.environ['KRAUSENING_BASE']

@when(u'the behave tests are run using the profile')
def step_impl(context):
    context.krausening_base=os.environ['KRAUSENING_BASE']
    if(context.integration_test_enabled):
        context.krausening_extensions=os.environ['KRAUSENING_EXTENSIONS']
    
@then(u'the system property is available in the virtual environment')
def step_impl(context):
    assert(context.krausening_base)

@then(u'the system property from the profile is available in the virtual environment')
def step_impl(context):
    assert(context.krausening_base)
    if(context.integration_test_enabled):
        assert(context.krausening_extensions)

def setConfig(context):
    config = TestConfig()
    context.integration_test_enabled = config.integration_test_enabled()