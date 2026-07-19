from pydantic_settings import BaseSettings,SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
    jwt_secret: str
    jwt_algorithm: str 
    database_url: str 
    redis_url: str 


settings = Settings() # type: ignore  becasue .env is required to have values