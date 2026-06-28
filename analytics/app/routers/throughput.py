from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.services import throughput_service

router = APIRouter()


@router.get("")
async def hourly_throughput(
    target_date: date = Query(default_factory=date.today),
    db: AsyncSession = Depends(get_db),
):
    """Orders completed per hour for the given date (defaults to today)."""
    hourly = await throughput_service.get_hourly_throughput(db, target_date)
    summary = await throughput_service.get_order_status_summary(db, target_date)
    return {
        "date": target_date.isoformat(),
        "hourly_breakdown": hourly,
        "status_summary": summary,
    }