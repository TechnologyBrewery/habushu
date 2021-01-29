from behave import *
import parse

# -- TYPE CONVERTER: For a simple, positive integer number.
@parse.with_pattern(r"\d+")
def parse_integer(text):
    return int(text)

# -- REGISTER TYPE-CONVERTER: With behave
register_type(Integer=parse_integer)

@given(u'a precondition specifying {items:Integer} items')
def step_impl(context, items):
    context.numberOfItems = items


@when(u'some action doubles the number of items')
def step_impl(context):
    context.numberOfItems = 2 * context.numberOfItems


@then(u'a postcondition checks that {expectedItems:Integer} items now exist')
def step_impl(context, expectedItems):
    assert context.numberOfItems == expectedItems