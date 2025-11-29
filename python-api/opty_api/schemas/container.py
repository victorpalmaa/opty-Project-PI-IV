"""
Dependency injection schemas.
"""

# --- TYPES ---
from opty_api.models import Config
from opty_api.mongo.repositories.users import UserRepository
from opty_api.mongo.setup.connection import MongoDBSetup
from openai import AsyncOpenAI
from supabase import AsyncClient
from typing import TypedDict


# --- CODE ---
class Container(TypedDict):
    """
    Dependency injection container schema.
    """
    config: Config
    supabase_client: AsyncClient
    mongodb: MongoDBSetup
    user_repository: UserRepository
    openai_client: AsyncOpenAI
