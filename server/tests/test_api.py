import importlib
import os
import tempfile
from pathlib import Path
from fastapi.testclient import TestClient

os.environ["NAVA_DB"] = str(Path(tempfile.mkdtemp()) / "test.sqlite3")
os.environ["NAVA_ADMIN_TOKEN"] = "test-api-token-must-be-32-chars-long-1234567"
from server import main

client = TestClient(main.app)
auth = {"Authorization": "Bearer " + os.environ["NAVA_ADMIN_TOKEN"]}
base = dict(id="test-1", title="نمونه آزمایشی", artist="قاری نمونه", category="قرآن",
            streamUrl="https://example.org/media.mp3",sourceUrl="https://example.org/item",
            sourceName="example", license="CC BY 4.0", rightsProofUrl="https://example.org/license",
            downloadAllowed=True)

def test_public_catalog_excludes_drafts():
    assert client.put('/admin/items/test-1', json=base, headers=auth).status_code == 200
    assert not client.get('/v1/catalog').json()['items']
    assert client.post('/admin/items/test-1/publish', headers=auth).status_code == 200
    items = client.get('/v1/catalog').json()['items']
    assert len(items) == 1
    assert 'rightsProofUrl' not in items[0]

def test_unauthorized():
    assert client.get('/admin/items').status_code == 401

def test_invalid_http_link():
    invalid = {**base, 'id':'test-2', 'streamUrl':'http://example.org/b.mp3'}
    assert client.put('/admin/items/test-2',json=invalid,headers=auth).status_code == 422
