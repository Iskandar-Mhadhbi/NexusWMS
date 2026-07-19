from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import api_router  
from fastapi.openapi.utils import get_openapi
from contextlib import asynccontextmanager
from app.core.redis_client import close_redis 

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    await close_redis()
app = FastAPI(
    title="NexusWMS Analytics Service",
    description="Read-only analytics and forecasting for NexusWMS",
    version="1.0.0",
)

# CORS 
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200"],
    allow_credentials=True,
    allow_methods=["GET"],
    allow_headers=["*"],
)
# Include the entire API with your global prefix
app.include_router(api_router, prefix="/api/v1/analytics")

@app.get("/health", tags=["Health"])
async def health():
    return {"status": "ok", "service": "nexuswms-analytics"}

def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema
    schema = get_openapi(
        title=app.title,
        version=app.version,
        description=app.description,
        routes=app.routes,
    )
    schema["components"]["securitySchemes"] = {
        "BearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
        }
    }
    for path in schema["paths"].values():
        for operation in path.values():
            operation["security"] = [{"BearerAuth": []}]
    app.openapi_schema = schema
    return schema


app.openapi = custom_openapi