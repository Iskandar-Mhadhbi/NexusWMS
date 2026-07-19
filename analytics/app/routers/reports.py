from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.services import report_service

router = APIRouter()


@router.get("/daily")
async def daily_report(
    target_date: date = Query(default_factory=date.today),
    db: AsyncSession = Depends(get_db),
):
    """
    End-of-day operational report combining throughput, worker performance,
    stock valuation, and 7-day demand forecast.
    All sub-queries run concurrently via asyncio.gather.
    """
    report = await report_service.get_daily_report(db, target_date)
    return report