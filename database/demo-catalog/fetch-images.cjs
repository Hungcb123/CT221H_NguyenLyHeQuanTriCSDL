// Restore the reviewed local illustrations; attribution is in image-sources.json.
const fs = require('node:fs');
const path = require('node:path');
const sources = require('./image-sources.json');
const directory = path.resolve(__dirname, '../../backend/.local/product-images');
fs.mkdirSync(directory, { recursive: true });
(async () => {
  for (const source of sources) {
    const target = path.join(directory, source.filename);
    if (fs.existsSync(target)) continue;
    const response = await fetch(source.source, {
      headers: { 'User-Agent': 'DailyMart-CourseDemo/1.0 (educational image attribution)' },
      signal: AbortSignal.timeout(30000),
    });
    if (!response.ok || !response.headers.get('content-type')?.startsWith('image/')) {
      throw new Error(`Cannot download ${source.filename}: HTTP ${response.status}`);
    }
    fs.writeFileSync(target, Buffer.from(await response.arrayBuffer()));
    console.log(`Downloaded ${source.filename}`);
  }
  console.log(`Ready: ${sources.length} reviewed illustrations.`);
})().catch(error => { console.error(error.message); process.exitCode = 1; });
