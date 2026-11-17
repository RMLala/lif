import fs from 'node:fs';
import { parse } from 'acorn';

fs.mkdirSync('www', { recursive: true });
fs.mkdirSync('dist', { recursive: true });
const read = path => fs.readFileSync('src/' + path, 'utf8');
const extra = ['features.js', 'model-v6.js', 'screens-v6.js', 'planning-v6.js', 'focus-v6.js', 'enhancements-v6.js', 'cloud-v3.js'].map(read).join('\n');
const app = read('app.js').replace('/*BOOT*/', extra + '\ninit();\nbootV6();');
parse(app, { ecmaVersion: 'latest' });

for (const oldPwaFile of ['manifest.webmanifest', 'sw.js', 'icon-192.png', 'icon-512.png']) {
  fs.rmSync('www/' + oldPwaFile, { force: true });
}
for (const file of ['index.html', 'styles.css', 'icon.svg']) {
  fs.copyFileSync('src/' + file, 'www/' + file);
}
fs.writeFileSync('www/app.js', app);
const html = read('index.html')
  .replace('<link rel="stylesheet" href="styles.css">', '<style>' + read('styles.css') + '</style>')
  .replace('<link rel="icon" href="icon.svg">', '<link rel="icon" href="data:image/svg+xml,' + encodeURIComponent(read('icon.svg')) + '">');
fs.writeFileSync('dist/LifeOS_Premium_V6.html', html);
console.log('HTML standalone construit et assets Android synchronisés (sans PWA).');
