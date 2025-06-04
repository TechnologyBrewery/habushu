from typing import Literal

import pytest


@pytest.fixture()
def sample_fixture() -> str:
    """Sample fixture."""
    return "fixture value"
