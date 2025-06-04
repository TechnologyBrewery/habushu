import os


def test_env_vars():
    """Test that environment variables are set correctly."""
    assert os.environ["TEST_MODULE"] == "habushu-uv-pytest"
    assert os.environ["ENV_VAR"] == "HelloWorld"
