# a simple module
from habushu_mixology.util.useful import i_do_something_useful


class SubWorker:
    def __init__(self):
        pass

    def do_something(self):
        return i_do_something_useful()

    def run(self):
        print("Worker is running")
