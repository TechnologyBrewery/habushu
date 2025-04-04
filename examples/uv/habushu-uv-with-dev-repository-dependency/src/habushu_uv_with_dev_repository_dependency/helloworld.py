import string
import random
from habushu_uv_with_dev_repository_dependency.reusable_module.worker import SubWorker

print("I'm alive!")


def start_worker():
    worker = SubWorker()
    worker.run()
    return worker.do_something()


def generate_random_string(n):
    return "".join(
        random.choice(string.ascii_uppercase + string.digits) for _ in range(n)
    )
