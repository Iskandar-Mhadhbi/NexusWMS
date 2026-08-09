FROM python:3.12-slim AS base

WORKDIR /app

# Container-friendly Python behavior:
# - PYTHONUNBUFFERED: stdout/stderr flush immediately, so logs show up in
#   `docker logs`/K8s log aggregation in real time instead of being buffered
# - PYTHONDONTWRITEBYTECODE: skip .pyc files, no benefit in a short-lived container
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Non-root user — required for K8s securityContext.runAsNonRoot
RUN groupadd -r analytics && useradd -r -g analytics analytics \
    && chown -R analytics:analytics /app

USER analytics

EXPOSE 8001

# Container-level check — belt-and-suspenders alongside K8s's own liveness probe.
# K8s itself should probe /health directly via its own probe config, not rely
# on this HEALTHCHECK instruction (K8s ignores it by default anyway).
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/health')" || exit 1

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]