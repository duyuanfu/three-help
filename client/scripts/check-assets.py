import os, re

base = 'dist/build/mp-weixin'
missing = []
assets = open(os.path.join(base, 'common/assets.js'), encoding='utf-8').read()
for p in re.findall(r'"(/static/[^"]+)"', assets):
    fp = os.path.join(base, p.lstrip('/'))
    if not os.path.exists(fp):
        missing.append(p)

# 页面内联引用（如有）
for root, dirs, files in os.walk(os.path.join(base, 'pages')):
    for f in files:
        if f.endswith(('.wxml', '.js', '.wxss')):
            txt = open(os.path.join(root, f), encoding='utf-8', errors='ignore').read()
            for p in re.findall(r'/static/[A-Za-z0-9_\-/\.]+', txt):
                fp = os.path.join(base, p.lstrip('/'))
                if not os.path.exists(fp):
                    missing.append(p + '  <- ' + os.path.join(root, f))

print('MISSING ASSETS:', len(set(missing)))
for m in sorted(set(missing)):
    print(' ', m)
