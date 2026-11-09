"""Evaluate confirmed results separately from related suggestions against the live API."""
import argparse
import json
import urllib.parse
import urllib.request
from pathlib import Path
from compare_models import normalize, summarize

ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://localhost:8085')
    parser.add_argument('--output', type=Path, default=ROOT / 'hybrid-main-report.json')
    args = parser.parse_args()
    snapshot = json.loads((ROOT / 'catalog-search-snapshot.json').read_text())
    rows = json.loads((ROOT / 'search-queries-v2.json').read_text())
    details = []
    main_rankings = []
    baseline_rankings = []
    for row, old in zip(rows, snapshot['queries']):
        assert row['query'] == old['query']
        row['relevantIds'] = [p['maSP'] for p in snapshot['products'] if any(
            normalize(name) in normalize(p['tenSP']) for name in row['relevantNames'])]
        url = args.base_url.rstrip('/') + '/api/v1/search/semantic?' + urllib.parse.urlencode(
            dict(query=row['query'], limit=100, minScore=0.3))
        with urllib.request.urlopen(url, timeout=30) as response:
            payload = json.load(response)
        assert payload['success']
        result = payload['data']
        main_ids = [p['maSP'] for p in result['products']]
        related_ids = [p['maSP'] for p in result['relatedProducts']]
        assert not set(main_ids) & set(related_ids), 'Suggestions must not duplicate primary matches'
        assert len(main_ids) == len(set(main_ids)) and len(related_ids) == len(set(related_ids))
        main_rankings.append(main_ids)
        baseline_rankings.append([p['maSP'] for p in old['hybrid']])
        details.append({**row, 'main': [p['tenSP'] for p in result['products']],
                        'related': [p['tenSP'] for p in result['relatedProducts']], 'mode': result['mode']})
    summary = {}
    for split in ['development', 'holdout']:
        indices = [i for i, row in enumerate(rows) if row['split'] == split]
        cases = [rows[i] for i in indices]
        summary[split] = {'before': summarize(cases, [baseline_rankings[i] for i in indices]),
                          'after': summarize(cases, [main_rankings[i] for i in indices])}
    report = {'note': 'Post-policy regression evaluation. The policy was added after inspecting model comparison and receiving the user preference; this is not an independent new holdout validation.',
              'policy': 'Hybrid/RRF: named product types must agree; lexical evidence or a clear semantic winner can enter primary results; rejected candidates remain suggestions.',
              'label_source': 'Assistant-proposed; user/domain review pending.', 'summary': summary, 'queries': details}
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
