from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.services import stock_service

router = APIRouter()


@router.get("/valuation")
async def stock_valuation(
    db: AsyncSession = Depends(get_db),
):
    """
    Current inventory valuation per SKU.
    Unit price sourced from most recent purchase order line.
    """
    skus = await stock_service.get_stock_valuation(db)
    summary = await stock_service.get_total_inventory_value(db)
    total_value = sum(sku["total_value"] for sku in skus)
    return {
        "summary": {
            **summary,
            "total_inventory_value": round(total_value, 2),
        },
        "skus": skus,
    }