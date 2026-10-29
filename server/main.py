"""Nava Asemani catalog API. No scraping or media mirroring without permission."""
from __future__ import annotations

import json
import os
import re
import sqlite3
import threading
from pathlib import Path
from typing import Literal
from urllib.parse import urlparse

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field, field_validator, model_validator

CATEGORIES = ("مداحی", "روضه", "سخنرانی", "ادعیه", "مناجات", "قرآن")
DB = Path(os.environ.get("NAVA_DB", str(Path(__file__).parent / "catalog.sqlite3")))
LOCK = threading.Lock()

app = FastAPI(title="Nava Asemani catalog", version="0.1.0", docs_url=None, redoc_url=None)
origins = [o.strip() for o in os.getenv("NAVA_CORS_ORIGINS", "").split(",") if o.strip()]
app.add_middleware(CORSMiddleware, allow_origins=origins, allow_credentials=False,
                   allow_methods=["GET"], allow_headers=["Content-Type"])


def connect():
    DB.parent.mkdir(parents=True, exist_ok=True)
    db = sqlite3.connect(DB, timeout=15)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA journal_mode=WAL")
    db.execute("""CREATE TABLE IF NOT EXISTS items (
        id TEXT PRIMARY KEY, data TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'draft',
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    )""")
    db.commit()
    return db


class Audio(BaseModel):
    id: str = Field(min_length=2, max_length=80, pattern=r"^[a-zA-Z0-9_-]+$")
    title: str = Field(min_length=2, max_length=250)
    artist: str = Field(min_length=1, max_length=180)
    category: Literal["مداحی", "روضه", "سخنرانی", "ادعیه", "مناجات", "قرآن"]
    occasion: str = Field(default="", max_length=180)
    description: str = Field(default="", max_length=2000)
    streamUrl: str = ""
    sourceUrl: str = ""
    sourceName: str = Field(default="", max_length=150)
    license: str = Field(default="", max_length=500)
    rightsProofUrl: str = ""
    downloadAllowed: bool = False
    duration: str = ""
    tags: str = Field(default="", max_length=500)

    @field_validator("streamUrl", "sourceUrl", "rightsProofUrl")
    @classmethod
    def valid_link(cls, v: str) -> str:
        if v and (not v.startswith("https://") or not urlparse(v).hostname or len(v) > 1600):
            raise ValueError("Only valid HTTPS URLs are permitted")
        return v

    @model_validator(mode="after")
    def checks(self):
        if self.downloadAllowed and not self.streamUrl:
            raise ValueError("Download requires a media URL")
        return self


async def admin_auth(authorization: str | None = Header(default=None)):
    import hmac
    secret = os.getenv("NAVA_ADMIN_TOKEN", "")
    token = authorization.removeprefix("Bearer ") if authorization else ""
    if len(secret) < 32 or not hmac.compare_digest(token, secret):
        raise HTTPException(status_code=401, detail="Invalid admin token")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/v1/catalog")
def public_catalog():
    with connect() as db:
        rows = db.execute("SELECT data FROM items WHERE status='published' ORDER BY updated_at DESC").fetchall()
    data = []
    for row in rows:
        entry = json.loads(row["data"])
        entry.pop("rightsProofUrl", None)
        data.append(entry)
    return {"version": 1, "items": data}


@app.get("/admin/items", dependencies=[Depends(admin_auth)])
def list_items():
    with connect() as db:
        rows = db.execute("SELECT id, data, status, created_at, updated_at FROM items ORDER BY updated_at DESC").fetchall()
    return [{"item": json.loads(r["data"]), "status": r["status"],
             "createdAt": r["created_at"], "updatedAt": r["updated_at"]} for r in rows]


@app.put("/admin/items/{item_id}", dependencies=[Depends(admin_auth)])
def upsert(item_id: str, item: Audio):
    if item_id != item.id:
        raise HTTPException(status_code=400, detail="URL and body IDs differ")
    with LOCK, connect() as db:
        db.execute("""INSERT INTO items(id,data,status) VALUES(?,?,'draft')
            ON CONFLICT(id) DO UPDATE SET data=excluded.data,status='draft', updated_at=CURRENT_TIMESTAMP""",
                   (item.id, item.model_dump_json()))
        db.commit()
    return {"id": item.id, "status": "draft"}


@app.post("/admin/items/{item_id}/publish", dependencies=[Depends(admin_auth)])
def publish(item_id: str):
    with LOCK, connect() as db:
        row = db.execute("SELECT data FROM items WHERE id=?", (item_id,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Item not found")
        item = Audio.model_validate_json(row["data"])
        if not (item.streamUrl and item.sourceUrl and item.sourceName and item.license and item.rightsProofUrl):
            raise HTTPException(status_code=422, detail="Media/source, license and proof URL are required")
        db.execute("UPDATE items SET status='published', updated_at=CURRENT_TIMESTAMP WHERE id=?", (item_id,))
        db.commit()
    return {"id": item_id, "status": "published"}


@app.post("/admin/items/{item_id}/unpublish", dependencies=[Depends(admin_auth)])
def unpublish(item_id: str):
    with LOCK, connect() as db:
        result = db.execute("UPDATE items SET status='draft', updated_at=CURRENT_TIMESTAMP WHERE id=?", (item_id,))
        db.commit()
        if result.rowcount == 0:
            raise HTTPException(status_code=404, detail="Item not found")
    return {"id": item_id, "status": "draft"}


@app.get("/admin/", response_class=HTMLResponse)
def dashboard():
    return (Path(__file__).parent / "admin.html").read_text(encoding="utf-8")


@app.post("/admin/import-seed", dependencies=[Depends(admin_auth)])
def import_seed():
    """Import ONLY the curated CC-licensed audio examples; placeholders remain drafts."""
    seed = Path(__file__).parent.parent / "app/src/main/assets/catalog.json"
    if not seed.exists():
        raise HTTPException(status_code=404, detail="Bundled seed unavailable")
    count = 0
    with LOCK, connect() as db:
        for raw in json.loads(seed.read_text(encoding="utf-8"))["items"]:
            item = Audio.model_validate({**raw, "rightsProofUrl": raw.get("sourceUrl", "")})
            db.execute("INSERT OR IGNORE INTO items (id,data,status) VALUES (?,?,?)",
                       (item.id, item.model_dump_json(), "published" if item.streamUrl else "draft"))
            count += 1
        db.commit()
    return {"importedOrExisting": count}
