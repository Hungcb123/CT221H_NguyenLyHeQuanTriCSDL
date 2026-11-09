"""Offline model comparison using actual SQL/API data and separate development/holdout queries.
No SQL data or existing Qdrant points are modified.
"""
import argparse
import json
import re
import time
import unicodedata
import urllib.parse
import urllib.request
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

ROOT = Path(__file__).resolve().parent


def normalize(text):
    text = ''.join(c for c in unicodedata.normalize('NFD', text.lower().replace('đ', 'd'))
                   if unicodedata.category(c) != 'Mn')
    return ' '.join(re.findall(r'[^\W_]+', text))


def encode(model, tokenizer_path, texts):
    options = ort.SessionOptions()
    options.intra_op_num_threads = 2
    options.inter_op_num_threads = 1
    session = ort.InferenceSession(str(model), sess_options=options, providers=['CPUExecutionProvider'])
    tokenizer = Tokenizer.from_file(str(tokenizer_path))
    tokenizer.enable_truncation(max_length=128)
    tokenizer.enable_padding(pad_id=tokenizer.token_to_id('<pad>') or 0)
    results = []
    started = time.perf_counter()
    for start in range(0, len(texts), 8):
        encoded = tokenizer.encode_batch(texts[start:start + 8])
        arrays = {'input_ids': np.array([e.ids for e in encoded], dtype=np.int64),
                  'attention_mask': np.array([e.attention_mask for e in encoded], dtype=np.int64),
                  'token_type_ids': np.array([e.type_ids for e in encoded], dtype=np.int64)}
        output = session.run(None, {i.name: arrays[i.name] for i in session.get_inputs()})[0]
        mask = arrays['attention_mask'][..., None]
        pooled = (output * mask).sum(axis=1) / np.maximum(mask.sum(axis=1), 1)
        pooled /= np.maximum(np.linalg.norm(pooled, axis=1, keepdims=True), 1e-12)
        results.extend(pooled)
    return np.asarray(results), round(time.perf_counter() - started, 3)


def fuse(products, keyword, vector, query):
    kranks = {pid: rank for rank, pid in enumerate(keyword[:50], 1)}
    vranks = {pid: rank for rank, pid in enumerate(vector[:50], 1)}
    by_id = {p['maSP']: p for p in products}
    def key(pid):
        exact = f' {normalize(query)} ' in f" {normalize(by_id[pid]['tenSP'])} "
        score = (0.5 / (60 + kranks[pid]) if pid in kranks else 0) + (
            0.5 / (60 + vranks[pid]) if pid in vranks else 0)
        return (-int(exact), -score, pid)
    return sorted(set(kranks) | set(vranks), key=key)


def metrics(ids, relevant):
    if not relevant:
        return {'no_match_correct': int(not ids), 'returned_count': len(ids)}
    return {'hit_at_3': int(bool(set(ids[:3]) & relevant)),
            'mrr_at_5': next((1 / rank for rank, pid in enumerate(ids[:5], 1) if pid in relevant), 0),
            'precision_at_3': len(set(ids[:3]) & relevant) / 3}


def summarize(rows, rankings):
    positives = [i for i, row in enumerate(rows) if row['relevantIds']]
    negatives = [i for i, row in enumerate(rows) if not row['relevantIds']]
    scores = [metrics(ids, set(row['relevantIds'])) for row, ids in zip(rows, rankings)]
    mrr = sum(scores[i]['mrr_at_5'] for i in positives) / len(positives) if positives else 0
    no_match = sum(scores[i]['no_match_correct'] for i in negatives) / len(negatives) if negatives else 0
    return {'positive_queries': len(positives), 'negative_queries': len(negatives),
            'hit_at_3': round(sum(scores[i]['hit_at_3'] for i in positives) / len(positives), 4) if positives else None,
            'mrr_at_5': round(mrr, 4) if positives else None,
            'no_match_accuracy': round(no_match, 4) if negatives else None,
            'selection_objective': round((mrr + no_match) / 2, 4)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://localhost:8085')
    parser.add_argument('--output', type=Path, default=ROOT / 'model-comparison.json')
    parser.add_argument('--refresh-snapshot', action='store_true')
    args = parser.parse_args()
    rows = json.loads((ROOT / 'search-queries-v2.json').read_text())

    def get(route, **params):
        url = args.base_url.rstrip('/') + '/api/v1/' + route + '?' + urllib.parse.urlencode(params)
        with urllib.request.urlopen(url, timeout=30) as response:
            payload = json.load(response)
        if not payload['success']:
            raise RuntimeError(payload['message'])
        return payload['data']

    cache = ROOT / 'catalog-search-snapshot.json'
    if args.refresh_snapshot or not cache.exists():
        catalog = get('products', page=1, size=100)
        products = catalog['items']
        for page in range(2, catalog['totalPages'] + 1):
            products += get('products', page=page, size=100)['items']
        if len(products) > 50:
            raise RuntimeError('This experimental runner requires a catalog <= 50 products')
        api_rows = []
        for row in rows:
            result = get('search/semantic', query=row['query'], limit=100, minScore=0.3)
            if 'relatedProducts' in result:
                raise RuntimeError('Use the frozen pre-gate snapshot for the historical baseline; evaluate_result_contract.py tests the current split API')
            if result['mode'] != 'HYBRID':
                raise RuntimeError('Existing vector baseline is unavailable')
            api_rows.append({'query': row['query'], 'hybrid': result['products'],
                             'like': get('products', page=1, size=100, keyword=row['query'])['items']})
        snapshot = {'products': products, 'queries': api_rows,
                    'baseline_status': get('search/qdrant-status')}
        cache.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + '\n')
    else:
        snapshot = json.loads(cache.read_text())
    products = snapshot['products']
    if [row['query'] for row in rows] != [row['query'] for row in snapshot['queries']]:
        raise RuntimeError('Query set changed; refresh the snapshot explicitly')
    for row in rows:
        row['relevantIds'] = [p['maSP'] for p in products if any(
            normalize(name) in normalize(p['tenSP']) for name in row['relevantNames'])]
        if row['relevantNames'] and not row['relevantIds']:
            raise RuntimeError(f"Review missing judgments: {row['query']}")

    documents = [p['tenSP'] + '. ' + (p.get('moTa') or '') + '. ' + (p.get('tenDanhMuc') or '')
                 for p in products]
    texts = documents + [row['query'] for row in rows]
    baseline_keyword = [[p['maSP'] for p in sorted(api['hybrid'], key=lambda p: (-p['keywordScore'], p['maSP']))
                         if p['keywordScore'] > 0] for api in snapshot['queries']]
    rankings = {'LIKE': [[p['maSP'] for p in api['like']] for api in snapshot['queries']],
                'BM25': baseline_keyword,
                'CURRENT_HYBRID': [[p['maSP'] for p in api['hybrid']] for api in snapshot['queries']]}
    selection = {}
    latency = {}
    dev_indices = [i for i, row in enumerate(rows) if row['split'] == 'development']
    dev_rows = [rows[i] for i in dev_indices]
    for name, model, tokenizer in [
        ('ENGLISH_RAW', ROOT.parent / 'src/main/resources/models/all-MiniLM-L6-v2.onnx', 'english-tokenizer.json'),
        ('MULTILINGUAL_RAW', ROOT / 'models/multilingual-minilm-int8.onnx', 'multilingual-tokenizer.json'),
    ]:
        vectors, latency[name] = encode(model, ROOT / 'models' / tokenizer, texts)
        similarities = vectors[len(products):] @ vectors[:len(products)].T
        candidates = []
        for threshold in [0.3, 0.4, 0.5, 0.6, 0.7, 0.8]:
            vector_rankings = [[products[j]['maSP'] for j in sorted(range(len(products)),
                               key=lambda j: (-similarities[i, j], products[j]['maSP']))
                               if similarities[i, j] >= threshold] for i in range(len(rows))]
            dev_scores = summarize(dev_rows, [vector_rankings[i] for i in dev_indices])
            candidates.append((dev_scores['selection_objective'], threshold, vector_rankings, dev_scores))
        # Choose thresholds on development data only; never select using holdout scores.
        _, threshold, vector_rankings, dev_scores = max(candidates, key=lambda value: (value[0], -value[1]))
        selection[name] = {'threshold': threshold, 'development': dev_scores,
                           'threshold_sweep': [{'threshold': c[1], **c[3]} for c in candidates]}
        rankings[name + '_VECTOR'] = vector_rankings
        rankings[name + '_HYBRID'] = [fuse(products, keyword, vector, row['query'])
                                      for keyword, vector, row in zip(baseline_keyword, vector_rankings, rows)]
    # Freeze the preferred method using only development judgments before reporting holdout.
    dev_summary = {method: summarize(dev_rows, [values[i] for i in dev_indices])
                   for method, values in rankings.items()}
    preferred = max(dev_summary, key=lambda method: dev_summary[method]['selection_objective'])
    report = {'note': 'Assistant-proposed provisional judgments. Needs user/domain review. No production rollout.',
              'catalog_size': len(products), 'query_count': len(rows), 'model_manifest': json.loads((ROOT / 'model-manifest.json').read_text()),
              'document_policy': 'Same raw SQL names, descriptions and categories for both experimental models. Current API baseline retains its legacy document enrichment.',
              'selection': selection, 'preferred_on_development': preferred,
              'embedding_seconds_for_documents_and_all_queries': latency, 'summary': {}, 'queries': []}
    for split in ['development', 'holdout']:
        indices = [i for i, row in enumerate(rows) if row['split'] == split]
        report['summary'][split] = {method: summarize([rows[i] for i in indices], [values[i] for i in indices])
                                    for method, values in rankings.items()}
    names = {p['maSP']: p['tenSP'] for p in products}
    for i, row in enumerate(rows):
        report['queries'].append({**row, 'methods': {method: {'metrics': metrics(values[i], set(row['relevantIds'])),
                                    'top_3': [names[pid] for pid in values[i][:3]], 'ids': values[i]}
                                    for method, values in rankings.items()}})
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({'preferred_on_development': preferred, 'selection': selection,
                      'summary': report['summary'], 'embedding_seconds': latency}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
