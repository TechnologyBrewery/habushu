# test_ruff_formatter.py

# 1. Inconsistent spacing around operators
def operator_spacing():
    x=  42
    y =x+5
    z = y *  10
    return z

# 2. Misaligned function definitions and parameters
def   misaligned_function   ( arg1,arg2 ,arg3 ) :
    return  arg1+arg2+arg3

# 3. poor spacing between different scopes and functions
def poor_spacing():
    a = 10
    b = 20
    return a + b
def poor_spacing_2():
    c = 30
    return c

# 4. Unnecessary parentheses
def unnecessary_parentheses():
    return (5 + (3 * 2))

# 5. Mixed use of single and double quotes
def mixed_quotes():
    string1 = 'This should be left the same'
    string2 = "This should also be left the same"
    return string1, string2

# 6. Multiple statements on one line
def multiple_statements(): a=5; b=10; print(a+b)

# 7. Inconsistent list formatting
def messy_list():
    numbers=[1,2 ,3 , 4,  5,6 ,7,8 , 9,10]
    return numbers

# 8. Misaligned dictionary formatting
def messy_dict():
    data = { 'key1':42 ,  'key2' : "value",  'key3': 100 }
    return data

# 9. Trailing commas and extra whitespace
def trailing_commas():
    return [1, 2, 3, 4, 5 , ]
