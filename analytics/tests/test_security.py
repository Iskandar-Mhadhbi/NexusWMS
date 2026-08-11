import pytest
from jose import JWTError, jwt

from app.core.config import settings
from app.core.security import decode_token, extract_role, is_analytics_authorized


def _make_token(claims: dict) -> str:
    return jwt.encode(claims, settings.jwt_secret, algorithm=settings.jwt_algorithm)


def test_decode_token_valid():
    token = _make_token({"sub": "user1", "role": "ADMIN"})
    claims = decode_token(token)
    assert claims["sub"] == "user1"
    assert claims["role"] == "ADMIN"


def test_decode_token_invalid_signature_raises():
    bad_token = jwt.encode({"role": "ADMIN"}, "wrong-secret", algorithm=settings.jwt_algorithm)
    with pytest.raises(JWTError):
        decode_token(bad_token)


def test_extract_role_present():
    assert extract_role({"role": "MANAGER"}) == "MANAGER"


def test_extract_role_missing_raises():
    with pytest.raises(ValueError):
        extract_role({"sub": "user1"})


def test_extract_role_does_not_strip_prefix():
    # Documents actual current behavior: no ROLE_ prefix stripping happens,
    # despite phase8_progress.md describing this as implemented. Flagged
    # separately — if stripping is intended, this test should be updated
    # alongside a fix to extract_role().
    assert extract_role({"role": "ROLE_ADMIN"}) == "ROLE_ADMIN"


@pytest.mark.parametrize("role,expected", [
    ("ADMIN", True),
    ("MANAGER", True),
    ("PICKER", False),
    ("", False),
])
def test_is_analytics_authorized(role, expected):
    assert is_analytics_authorized(role) == expected