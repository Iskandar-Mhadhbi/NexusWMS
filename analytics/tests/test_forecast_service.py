from datetime import datetime, timedelta

import pytest

from app.services.forecast_service import _forecast_sku


def _records(sku_id="sku-1", sku_code="SKU-001", name="Widget", daily_totals=None):
    base = datetime(2026, 1, 1)
    daily_totals = daily_totals or []
    return [
        {
            "sku_id": sku_id,
            "sku_code": sku_code,
            "name": name,
            "movement_date": base + timedelta(days=i),
            "total_moved": total,
        }
        for i, total in enumerate(daily_totals)
    ]


def test_insufficient_data_returns_none_forecast():
    records = _records(daily_totals=[10])
    result = _forecast_sku(records, days_ahead=3)

    assert result["data_points"] == 1
    assert result["forecast"] is None
    assert "Insufficient data" in result["message"]
    assert result["sku_code"] == "SKU-001"
    assert "trend_slope" not in result


def test_flat_trend_projects_same_value():
    records = _records(daily_totals=[50, 50, 50, 50])
    result = _forecast_sku(records, days_ahead=2)

    assert result["message"] == "OK"
    assert result["data_points"] == 4
    assert result["trend_slope"] == pytest.approx(0.0, abs=0.01)
    assert len(result["forecast"]) == 2
    for day in result["forecast"]:
        assert day["projected_units"] == pytest.approx(50.0, abs=0.5)


def test_increasing_trend_has_positive_slope_and_projects_upward():
    records = _records(daily_totals=[10, 20, 30, 40])
    result = _forecast_sku(records, days_ahead=3)

    assert result["trend_slope"] > 0
    projected = [d["projected_units"] for d in result["forecast"]]
    assert projected[0] == pytest.approx(50.0, abs=0.5)
    assert projected[1] > projected[0]
    assert projected[2] > projected[1]


def test_forecast_never_projects_negative_units():
    records = _records(daily_totals=[100, 50, 0])
    result = _forecast_sku(records, days_ahead=5)

    assert result["message"] == "OK"
    for day in result["forecast"]:
        assert day["projected_units"] >= 0


def test_forecast_day_numbering_starts_at_one():
    records = _records(daily_totals=[5, 10])
    result = _forecast_sku(records, days_ahead=3)

    days = [d["day"] for d in result["forecast"]]
    assert days == [1, 2, 3]


def test_unsorted_input_is_sorted_before_regression():
    base = datetime(2026, 1, 1)
    records = [
        {"sku_id": "sku-1", "sku_code": "SKU-001", "name": "Widget",
         "movement_date": base + timedelta(days=2), "total_moved": 30},
        {"sku_id": "sku-1", "sku_code": "SKU-001", "name": "Widget",
         "movement_date": base, "total_moved": 10},
        {"sku_id": "sku-1", "sku_code": "SKU-001", "name": "Widget",
         "movement_date": base + timedelta(days=1), "total_moved": 20},
    ]
    result = _forecast_sku(records, days_ahead=1)

    assert result["message"] == "OK"
    assert result["trend_slope"] > 0