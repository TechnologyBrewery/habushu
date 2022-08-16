from dotenv import *
import logging


def before_all(context):
    """
    Uses python-dotenv to set environment variables (utilized by Krausening) by loading a .env file
    based on the value of the "-D environment=[environment-value]" provided when executing behave.
    In this scenario, if *no* environment value is provided, the default ".env" is loaded.  If an
    environment value *is* provided, the .env file named .env.environment-value is loaded.
    For example, if -D environment=integration_test is provided through Habushu as a command line
    option to behave, ".env.integration_test" will be loaded into the virtual environment
    """
    context.config.setup_logging()

    environment = context.config.userdata.get("environment")
    env_file_path = find_dotenv(f".env.{environment}" if environment else ".env")
    logging.info(f"Loading environment variables from {env_file_path}")
    load_dotenv(env_file_path)
