import string
import random
from habushu_poetry_package.reusable_module.worker import SubWorker

print("I'm alive!")


def start_worker():
    worker = SubWorker()
    worker.run()
    return worker.do_something()
