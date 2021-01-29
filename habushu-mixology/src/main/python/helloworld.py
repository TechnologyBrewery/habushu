import string
import random

print("I'm alive!")

def generate_random_string(n):
    return ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(n))
    