from habushu.reusable_module.worker import SubWorker

def call_worker_from_nested_dir():
    sub_worker = SubWorker()
    return "nested " + sub_worker.do_something()