"""
System endpoints.
"""

# --- IMPORTS ---
from fastapi import APIRouter
from fastapi.responses import JSONResponse
from opty_api.app import health
from opty_api.app import info
from opty_api.models import Health
from opty_api.models import Info


# --- GLOBAL ---
# Router instance
router = APIRouter()


# --- CODE ---
# Health endpoint
@router.get('/health', response_model = Health)
def get_health() -> JSONResponse:
    """
    Returns the current system health status.
    """
    return JSONResponse(health.dict())


# Info endpoint
@router.get('/info', response_model = Info)
def get_info() -> JSONResponse:
    """
    Returns system information.
    """
    return JSONResponse(info.dict())
