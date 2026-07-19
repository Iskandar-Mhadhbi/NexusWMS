from datetime import date

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession


async def get_picker_performance(db: AsyncSession, target_date: date) -> list[dict]:
    """
    Returns total items picked per worker for the given date.
    Worker is resolved via pick_lists.assigned_to since pick_list_items
    does not store the individual picker — the whole list is assigned to one worker.
    """
    result = await db.execute(
        text("""
            SELECT
                u.employee_id,
                u.name,
                COUNT(pli.id)            AS items_picked,
                SUM(pli.quantity_picked) AS units_picked
            FROM pick_list_items pli
            JOIN pick_lists pl ON pl.id = pli.pick_list_id
            JOIN users u       ON u.id  = pl.assigned_to
            WHERE pli.status = 'PICKED'
              AND pli.picked_at::date = :target_date
            GROUP BY u.id, u.employee_id, u.name
            ORDER BY units_picked DESC
        """),
        {"target_date": target_date},
    )
    rows = result.fetchall()
    return [
        {
            "employee_id": row.employee_id,
            "name": row.name,
            "items_picked": row.items_picked,
            "units_picked": int(row.units_picked),
        }
        for row in rows
    ]


async def get_packer_performance(db: AsyncSession, target_date: date) -> list[dict]:
    """
    Returns total packing tasks completed per worker for the given date.
    Joins packing_tasks with users to resolve worker name.
    """
    result = await db.execute(
        text("""
            SELECT
                u.employee_id,
                u.name,
                COUNT(pt.id) AS tasks_completed
            FROM packing_tasks pt
            JOIN users u ON u.id = pt.assigned_to
            WHERE pt.status = 'COMPLETED'
              AND pt.completed_at::date = :target_date
            GROUP BY u.id, u.employee_id, u.name
            ORDER BY tasks_completed DESC
        """),
        {"target_date": target_date},
    )
    rows = result.fetchall()
    return [
        {
            "employee_id": row.employee_id,
            "name": row.name,
            "tasks_completed": row.tasks_completed,
        }
        for row in rows
    ]