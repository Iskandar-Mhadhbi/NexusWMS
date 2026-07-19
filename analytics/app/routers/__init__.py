from fastapi import APIRouter, Depends

from app.core.dependencies import require_manager
from . import forecast, reports, stock, throughput, worker

api_router = APIRouter(dependencies=[Depends(require_manager)])

api_router.include_router(throughput.router, prefix="/throughput", tags=["Throughput"])
api_router.include_router(worker.router, prefix="/worker", tags=["Workers"])
api_router.include_router(stock.router, prefix="/stock", tags=["Stock"])
api_router.include_router(forecast.router, prefix="/forecast", tags=["Forecast"])
api_router.include_router(reports.router, prefix="/reports", tags=["Reports"])