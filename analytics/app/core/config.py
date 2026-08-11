from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    jwt_secret: str
    jwt_algorithm: str
    database_url: str
    redis_url: str

    fastapi_cors_allowed_origins: str

    @property
    def cors_origins_list(self) -> list[str]:
        return [origin.strip() for origin in self.fastapi_cors_allowed_origins.split(",") if origin.strip()]


settings = Settings()  # type: ignore  because .env is required to have values