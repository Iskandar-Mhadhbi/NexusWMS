from datetime import date

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


async def get_hourly_throughput(db: AsyncSession, target_date: date) -> list[dict]:
    result = await db.execute(
        text("""
            SELECT
                DATE_TRUNC('hour', updated_at) AS hour,
                COUNT(*)                        AS orders_completed
            FROM orders
            WHERE status = 'DISPATCHED'
              AND updated_at::date = :target_date
            GROUP BY DATE_TRUNC('hour', updated_at)
            ORDER BY hour
        """),
        {"target_date": target_date},
    )
    rows = result.fetchall()
    return [
        {
            "hour": row.hour.isoformat(),
            "orders_completed": row.orders_completed,
        }
        for row in rows
    ]


async def get_order_status_summary(db: AsyncSession, target_date: date) -> dict[str, int]:
    result = await db.execute(
        text("""
            SELECT status, COUNT(*) AS total
            FROM orders
            WHERE created_at::date = :target_date
            GROUP BY status
        """),
        {"target_date": target_date},
    )
    rows = result.fetchall()
    return {row.status: row.total for row in rows}