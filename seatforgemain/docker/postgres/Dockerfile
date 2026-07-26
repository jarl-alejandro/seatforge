FROM postgres:17-alpine

LABEL org.opencontainers.image.title="SeatForge PostgreSQL"
LABEL org.opencontainers.image.description="PostgreSQL local para el laboratorio SeatForge"

# Valores únicamente para desarrollo local. Docker Compose puede sobrescribirlos.
ENV POSTGRES_DB=seatforge \
    POSTGRES_USER=seatforge \
    POSTGRES_PASSWORD=seatforge

EXPOSE 5432