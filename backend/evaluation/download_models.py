"""Download pinned official model assets, checking hashes recorded with the experiment."""
import hashlib
import json
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parent
manifest = json.loads((root / 'model-manifest.json').read_text())
models = root / 'models'
models.mkdir(exist_ok=True)
assets = []
for name, info in manifest['files'].items():
    assets.append((name, info, manifest['repository'], manifest['revision'], info['source']))
assets.append(('english-tokenizer.json', manifest['englishFiles']['english-tokenizer.json'],
               'sentence-transformers/all-MiniLM-L6-v2', manifest['englishTokenizerRevision'], 'tokenizer.json'))
for name, info, repository, revision, source in assets:
    path = models / name
    if not path.exists():
        temporary = path.with_suffix(path.suffix + '.part')
        print(f'Downloading {repository}/{source}', flush=True)
        urllib.request.urlretrieve(f'https://huggingface.co/{repository}/resolve/{revision}/{source}', temporary)
        if hashlib.sha256(temporary.read_bytes()).hexdigest() != info['sha256']:
            temporary.unlink()
            raise RuntimeError(f'Checksum mismatch: {name}')
        temporary.rename(path)
    if hashlib.sha256(path.read_bytes()).hexdigest() != info['sha256']:
        raise RuntimeError(f'Checksum mismatch: {name}')
    print(f'Verified {name}: {path.stat().st_size} bytes')
