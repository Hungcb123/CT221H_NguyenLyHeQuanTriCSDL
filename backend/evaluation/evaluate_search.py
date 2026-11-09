"""Compare actual SQL LIKE, BM25, vector and hybrid rankings against starter judgments."""
import argparse
import json
import unicodedata
import urllib.parse
import urllib.request
from pathlib import Path


def normalize(text):
    return ''.join(c for c in unicodedata.normalize('NFD', text.lower().replace('đ', 'd'))
                   if unicodedata.category(c) != 'Mn')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://localhost:8085')
    parser.add_argument('--output', type=Path)
    parser.add_argument('--snapshot', type=Path, help='Frozen unsplit API snapshot for historical comparison')
    args = parser.parse_args()

    def get(route, **params):
        url = args.base_url.rstrip('/') + '/api/v1/' + route + '?' + urllib.parse.urlencode(params)
        with urllib.request.urlopen(url, timeout=30) as response:
            payload = json.load(response)
        if not payload['success']:
            raise RuntimeError(payload['message'])
        return payload['data']

    frozen = json.loads(args.snapshot.read_text()) if args.snapshot else None
    catalog = {'items': frozen['products'], 'totalPages': 1} if frozen else get('products', page=1, size=100)
    products = list(catalog['items'])
    for page in range(2, catalog['totalPages'] + 1):
        products.extend(get('products', page=page, size=100)['items'])
    judgments = json.loads(Path(__file__).with_name('search-queries.json').read_text())
    details = []
    for case in judgments:
        relevant = {p['maSP'] for p in products if any(
            normalize(name) in normalize(p['tenSP']) for name in case['relevantNames'])}
        if not relevant:
            raise RuntimeError(f"Review judgments for this catalog: {case['query']}")
        old = next((q for q in frozen['queries'] if q['query'] == case['query']), None) if frozen else None
        result = {'products': old['hybrid'], 'mode': 'HYBRID'} if old else get('search/semantic', query=case['query'], limit=100, minScore=0.3)
        if 'relatedProducts' in result:
            raise RuntimeError('The live API now separates suggestions. Use --snapshot evaluation/catalog-search-snapshot.json for the historical comparison, or evaluate_result_contract.py for the current API.')
        if result['mode'] != 'HYBRID':
            raise RuntimeError('Vector unavailable; cannot compare all four methods fairly')
        candidates = result['products']
        rankings = {
            'LIKE': old['like'] if old else get('products', page=1, size=100, keyword=case['query'])['items'],
            'BM25': sorted((p for p in candidates if p['keywordScore'] > 0),
                           key=lambda p: (-p['keywordScore'], p['maSP'])),
            'VECTOR': sorted((p for p in candidates if p['similarityScore'] >= 0.3),
                             key=lambda p: (-p['similarityScore'], p['maSP'])),
            'HYBRID': candidates,
        }
        scores = {}
        for method, ranking in rankings.items():
            ids = [p['maSP'] for p in ranking]
            scores[method] = {
                'hit_at_3': int(bool(set(ids[:3]) & relevant)),
                'mrr_at_5': next((1 / rank for rank, pid in enumerate(ids[:5], 1) if pid in relevant), 0),
                'precision_at_3': len(set(ids[:3]) & relevant) / 3,
                'top_3': [p['tenSP'] for p in ranking[:3]],
            }
        details.append(dict(query=case['query'], kind=case['kind'], scores=scores))
    summary = {}
    for group in ['all', 'name', 'intent']:
        rows = [row for row in details if group == 'all' or row['kind'] == group]
        summary[group] = {method: {metric: round(sum(row['scores'][method][metric] for row in rows)
                                                 / len(rows), 4)
                                   for metric in ['hit_at_3', 'mrr_at_5', 'precision_at_3']}
                          for method in ['LIKE', 'BM25', 'VECTOR', 'HYBRID']}
    report = dict(note='Starter assistant-proposed judgments, not a representative production benchmark.',
                  catalog_size=len(products), query_count=len(details), summary=summary, queries=details)
    if args.output:
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
