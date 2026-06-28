from datetime import date

from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.services import worker_service

router = APIRouter()


@router.get("/performance")
async def worker_performance(
    target_date: date = Query(default_factory=date.today),
    db: AsyncSession = Depends(get_db),
):
    """
    Picker and packer performance metrics for the given date.
    Returns items picked per picker and tasks completed per packer.
    """
    pickers = await worker_service.get_picker_performance(db, target_date)
    packers = await worker_service.get_packer_performance(db, target_date)
    return {
        "date": target_date.isoformat(),
        "pickers": pickers,
        "packers": packers,
    }