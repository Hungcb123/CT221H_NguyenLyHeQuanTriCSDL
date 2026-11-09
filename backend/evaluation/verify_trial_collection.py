"""Verify multilingual inference against a separate Qdrant collection; never touch the live collection."""
import json
import urllib.request
from pathlib import Path
import numpy as np
from compare_models import encode

root = Path(__file__).resolve().parent
snapshot = json.loads((root / 'catalog-search-snapshot.json').read_text())
products = snapshot['products']
texts = [p['tenSP'] + '. ' + (p.get('moTa') or '') + '. ' + (p.get('tenDanhMuc') or '') for p in products]
query = 'nguyên liệu nấu canh chua'
vectors, _ = encode(root / 'models/multilingual-minilm-int8.onnx', root / 'models/multilingual-tokenizer.json', texts + [query])
collection = 'fresh_grocery_multilingual_trial_e8f8c211_raw_v1'
base = 'http://localhost:6333/collections/' + collection

def request(method, suffix, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(base + suffix, data=data, headers={'Content-Type': 'application/json'}, method=method)
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)

# PUT only this versioned trial collection. Existing collection is reused without deleting data.
try:
    request('GET', '')
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise
    request('PUT', '', {'vectors': {'size': 384, 'distance': 'Cosine'}})
request('PUT', '/points?wait=true', {'points': [
    {'id': p['maSP'], 'vector': v.tolist(), 'payload': {'tenSP': p['tenSP'],
      'model': 'paraphrase-multilingual-MiniLM-L12-v2-int8', 'documentPolicy': 'raw-sql-v1'}}
    for p, v in zip(products, vectors[:-1])]})
result = request('POST', '/points/search', {'vector': vectors[-1].tolist(), 'limit': len(products), 'with_payload': True})['result']
expected = {p['maSP']: float(v @ vectors[-1]) for p, v in zip(products, vectors[:-1])}
for point in result:
    assert abs(point['score'] - expected[point['id']]) < 0.0001
report = {'collection': collection, 'points': len(result), 'query': query,
          'max_score_error': max(abs(p['score'] - expected[p['id']]) for p in result),
          'top_3': [{'name': p['payload']['tenSP'], 'score': p['score']} for p in result[:3]],
          'live_collection': snapshot['baseline_status']['collectionName'], 'live_collection_modified': False}
(root / 'trial-collection-report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(json.dumps(report, ensure_ascii=False, indent=2))
