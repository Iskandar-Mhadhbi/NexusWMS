import logging

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError
from redis.asyncio import Redis

from app.core.redis_client import get_redis
from app.core.security import decode_token, extract_role, is_analytics_authorized

logger = logging.getLogger("nexuswms.analytics.access")

bearer_scheme = HTTPBearer()


async def require_manager(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    redis: Redis = Depends(get_redis),
) -> dict:
    """
    Enforces JWT authentication and role-based access control.
    Mirrors Spring Boot's JwtAuthFilter logic:
      1. Decode and validate JWT signature
      2. Check Redis blocklist (suspended/terminated users)
      3. Verify role is ADMIN or MANAGER
    Logs every authorized access for accountability — analytics endpoints
    expose financial and performance data, so knowing who viewed what
    and when is a legitimate access-audit concern even on a read-only service.
    """
    unauthorized = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired token",
        headers={"WWW-Authenticate": "Bearer"},
    )
    forbidden = HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Insufficient permissions — ADMIN or MANAGER role required",
    )

    try:
        claims = decode_token(credentials.credentials)
    except JWTError:
        raise unauthorized

    try:
        role = extract_role(claims)
    except ValueError:
        raise unauthorized

    user_id = claims.get("sub")
    if user_id:
        blocklisted = await redis.get(f"blocklist:user:{user_id}")
        if blocklisted:
            raise unauthorized

    if not is_analytics_authorized(role):
        raise forbidden

    logger.info(
        "analytics_access user_id=%s email=%s role=%s",
        user_id,
        claims.get("email"),
        role,
    )

    return claims