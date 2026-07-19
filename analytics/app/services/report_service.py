from datetime import date

from sqlalchemy.ext.asyncio import AsyncSession

from app.services import forecast_service, stock_service, throughput_service, worker_service


async def get_daily_report(db: AsyncSession, target_date: date) -> dict:
    """
    Compiles a full end-of-day operational report.
    Queries run sequentially — SQLAlchemy async sessions are not
    safe for concurrent use across coroutines in the same session.
    """
    hourly = await throughput_service.get_hourly_throughput(db, target_date)
    status_summary = await throughput_service.get_order_status_summary(db, target_date)
    pickers = await worker_service.get_picker_performance(db, target_date)
    packers = await worker_service.get_packer_performance(db, target_date)
    stock = await stock_service.get_stock_valuation(db)
    inventory_summary = await stock_service.get_total_inventory_value(db)
    forecasts = await forecast_service.get_demand_forecast(db, days_ahead=7, sku_id=None)

    total_value = sum(sku["total_value"] for sku in stock)

    return {
        "report_date": target_date.isoformat(),
        "throughput": {
            "hourly_breakdown": hourly,
            "status_summary": status_summary,
            "total_dispatched": status_summary.get("DISPATCHED", 0),
        },
        "workforce": {
            "pickers": pickers,
            "packers": packers,
        },
        "inventory": {
            "summary": {
                **inventory_summary,
                "total_inventory_value": round(total_value, 2),
            },
            "skus": stock,
        },
        "demand_forecast": {
            "days_ahead": 7,
            "forecasts": forecasts,
        },
    } 