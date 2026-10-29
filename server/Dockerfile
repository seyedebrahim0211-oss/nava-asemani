FROM python:3.12-slim
WORKDIR /opt/nava
COPY server/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY server/ ./server/
COPY app/src/main/assets/catalog.json ./app/src/main/assets/catalog.json
ENV NAVA_DB=/data/catalog.sqlite3
RUN useradd -r -u 10001 nava && mkdir /data && chown nava /data
USER nava
EXPOSE 8000
CMD ["uvicorn", "server.main:app", "--host", "0.0.0.0", "--port", "8000"]
