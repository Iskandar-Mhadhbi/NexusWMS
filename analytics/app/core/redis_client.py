from redis.asyncio import Redis
from redis.asyncio.connection import ConnectionPool

from app.core.config import settings

_pool = ConnectionPool.from_url(
    settings.redis_url,
    encoding="utf-8",
    decode_responses=True,
    max_connections=10,
)

redis_client = Redis(connection_pool=_pool)


async def get_redis() -> Redis:
    """Return the active Redis client for use as a FastAPI dependency."""
    return redis_client


async def close_redis() -> None:
    """Disconnect the connection pool. Called at application shutdown."""
    await _pool.disconnect()