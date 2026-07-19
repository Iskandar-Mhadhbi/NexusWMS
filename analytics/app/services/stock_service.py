from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


async def get_stock_valuation(db: AsyncSession) -> list[dict]:
    """
    Returns total inventory value per SKU.
    Valuation = SUM(quantity) * unit_price from the last purchase order line.
    Falls back to 0 if no purchase order exists for the SKU.
    """
    result = await db.execute(
        text("""
            SELECT
                s.sku_code,
                s.name,
                s.unit,
                COALESCE(SUM(sl.quantity), 0)                          AS total_quantity,
                COALESCE(SUM(sl.reserved_quantity), 0)                 AS reserved_quantity,
                COALESCE(SUM(sl.quantity - sl.reserved_quantity), 0)   AS available_quantity,
                COALESCE(
                    (
                        SELECT pol.unit_price
                        FROM purchase_order_lines pol
                        WHERE pol.sku_id = s.id
                        ORDER BY pol.id DESC
                        LIMIT 1
                    ), 0
                )                                                       AS unit_price,
                COALESCE(SUM(sl.quantity), 0) * COALESCE(
                    (
                        SELECT pol.unit_price
                        FROM purchase_order_lines pol
                        WHERE pol.sku_id = s.id
                        ORDER BY pol.id DESC
                        LIMIT 1
                    ), 0
                )                                                       AS total_value
            FROM skus s
            LEFT JOIN sku_locations sl ON sl.sku_id = s.id
            GROUP BY s.id, s.sku_code, s.name, s.unit
            ORDER BY total_value DESC
        """)
    )
    rows = result.fetchall()
    return [
        {
            "sku_code": row.sku_code,
            "name": row.name,
            "unit": row.unit,
            "total_quantity": int(row.total_quantity),
            "reserved_quantity": int(row.reserved_quantity),
            "available_quantity": int(row.available_quantity),
            "unit_price": float(row.unit_price),
            "total_value": float(row.total_value),
        }
        for row in rows
    ]


async def get_total_inventory_value(db: AsyncSession) -> dict[str, int]:
    """Returns the aggregate inventory value across all SKUs."""
    result = await db.execute(
        text("""
            SELECT
                COUNT(DISTINCT s.id)           AS total_skus,
                COALESCE(SUM(sl.quantity), 0)  AS total_units
            FROM skus s
            LEFT JOIN sku_locations sl ON sl.sku_id = s.id
        """)
    )
    row = result.fetchone()
    if row is None:
        return {"total_skus": 0, "total_units": 0}
    return {
        "total_skus": int(row.total_skus) if row.total_skus is not None else 0,
        "total_units": int(row.total_units) if row.total_units is not None else 0,
    }