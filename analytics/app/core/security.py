from jose import JWTError, jwt

from app.core.config import settings

ALLOWED_ROLES = {"ADMIN", "MANAGER"} 



def decode_token(token: str) -> dict:
    """
    Decode and validate a JWT token signed by the Spring Boot auth service.
    Returns the full claims payload on success.
    Raises JWTError on invalid or expired tokens.
    """
    return jwt.decode(
        token,
        settings.jwt_secret,
        algorithms=[settings.jwt_algorithm],
    )


def extract_role(claims: dict) -> str:
    """
    Extract the role claim from the decoded JWT payload.
    Spring Boot stores role as 'role' in the claims body.
    """
    role = claims.get("role")
    if not role:
        raise ValueError("Token is missing role claim") 
    return role


def is_analytics_authorized(role: str) -> bool:
    """Return True if the role is permitted to access analytics endpoints."""
    return role in ALLOWED_ROLES