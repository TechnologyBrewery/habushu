import pytest

from habushu_uv_pytest.calculator import add, divide, multiply, subtract


@pytest.mark.integration_test
def test_add():
    assert add(1, 2) == 3
    assert add(-1, 1) == 0
    assert add(-1, -1) == -2


@pytest.mark.integration_test
def test_subtract():
    assert subtract(3, 2) == 1
    assert subtract(2, 3) == -1
    assert subtract(0, 0) == 0


@pytest.mark.integration_test
def test_multiply():
    assert multiply(2, 3) == 6
    assert multiply(-2, 3) == -6
    assert multiply(-2, -3) == 6


@pytest.mark.integration_test
def test_divide():
    assert divide(6, 3) == 2
    assert divide(6, -3) == -2
    assert divide(-6, -3) == 2


def test_divide_by_zero():
    with pytest.raises(ValueError):
        divide(5, 0)
