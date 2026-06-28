import numpy as np
import pandas as pd
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


async def get_movement_history(db: AsyncSession, sku_id: str | None) -> list[dict]:
    """
    Fetches daily stock movement totals from stock_movements.
    Filters to GOODS_RECEIPT and ADJUSTMENT movement types only —
    these reflect real demand signals, not internal transfers.
    Optionally scoped to a single SKU.
    """
    base_query = """
        SELECT
            sm.sku_id::text,
            s.sku_code,
            s.name,
            DATE_TRUNC('day', sm.moved_at)  AS movement_date,
            SUM(ABS(sm.quantity))           AS total_moved
        FROM stock_movements sm
        JOIN skus s ON s.id = sm.sku_id
        WHERE sm.movement_type IN ('GOODS_RECEIPT', 'ADJUSTMENT')
    """
    params: dict = {}
    if sku_id:
        base_query += " AND sm.sku_id = :sku_id"
        params["sku_id"] = sku_id

    base_query += """
        GROUP BY sm.sku_id, s.sku_code, s.name, DATE_TRUNC('day', sm.moved_at)
        ORDER BY sm.sku_id, movement_date
    """
    result = await db.execute(text(base_query), params)
    rows = result.fetchall()
    return [
        {
            "sku_id": row.sku_id,
            "sku_code": row.sku_code,
            "name": row.name,
            "movement_date": row.movement_date,
            "total_moved": int(row.total_moved),
        }
        for row in rows
    ]


def _forecast_sku(records: list[dict], days_ahead: int) -> dict:
    """
    Runs a simple linear regression on daily movement totals for a single SKU.
    Returns projected demand for the next N days.
    Requires at least 2 data points — returns None projection if insufficient data.
    """
    from sklearn.linear_model import LinearRegression

    df = pd.DataFrame(records)
    df["movement_date"] = pd.to_datetime(df["movement_date"])
    df = df.sort_values("movement_date").reset_index(drop=True)
    df["day_index"] = (
        df["movement_date"] - df["movement_date"].min()
    ).dt.days

    sku_code = df["sku_code"].iloc[0]
    sku_name = df["name"].iloc[0]
    sku_id = df["sku_id"].iloc[0]

    if len(df) < 2:
        return {
            "sku_id": sku_id,
            "sku_code": sku_code,
            "name": sku_name,
            "data_points": len(df),
            "forecast": None,
            "message": "Insufficient data — need at least 2 days of movement history",
        }

    X = df[["day_index"]].values
    y = df["total_moved"].values

    model = LinearRegression()
    model.fit(X, y)

    last_day = int(df["day_index"].max())
    future_indices = np.array(
        [[last_day + i] for i in range(1, days_ahead + 1)]
    )
    projections = model.predict(future_indices)

    forecast = [
        {
            "day": i,
            "projected_units": max(0, round(float(p), 2)),
        }
        for i, p in enumerate(projections, start=1)
    ]

    return {
        "sku_id": sku_id,
        "sku_code": sku_code,
        "name": sku_name,
        "data_points": len(df),
        "trend_slope": round(float(model.coef_[0]), 4),
        "forecast": forecast,
        "message": "OK",
    }


async def get_demand_forecast(
    db: AsyncSession,
    days_ahead: int,
    sku_id: str | None,
) -> list[dict]:
    """
    Fetches movement history and runs linear regression per SKU.
    Groups records by SKU and forecasts each independently.
    """
    records = await get_movement_history(db, sku_id)
    if not records:
        return []

    df = pd.DataFrame(records)
    results = []
    for _, group in df.groupby("sku_id"):
        group_records = group.to_dict(orient="records")
        results.append(_forecast_sku(group_records, days_ahead))

    return results