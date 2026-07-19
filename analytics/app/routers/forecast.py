from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.services import forecast_service

router = APIRouter()


@router.get("/demand")
async def demand_forecast(
    days_ahead: int = Query(default=7, ge=1, le=90),
    sku_id: str | None = Query(default=None),
    db: AsyncSession = Depends(get_db),
):
    """
    Demand forecast using linear regression on stock movement history.
    Returns projected daily demand per SKU for the next N days.
    Optionally scoped to a single SKU via sku_id query param.
    """
    forecasts = await forecast_service.get_demand_forecast(db, days_ahead, sku_id)
    return {
        "days_ahead": days_ahead,
        "forecasts": forecasts,
    }