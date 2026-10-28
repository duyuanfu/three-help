from PIL import Image
import numpy as np

ui = Image.open('docs/ui/登录页.png').convert('RGB')
W, H = ui.size
ui_a = np.asarray(ui, dtype=np.float32) / 255.0

img = Image.open('docs/_static_backup/login/bg-clean.jpg').convert('RGB')
bw, bh = img.size
s = max(W / bw, H / bh)
img = img.resize((int(bw * s), int(bh * s)), Image.LANCZOS)
l = (img.size[0] - W) // 2
t = (img.size[1] - H) // 2
top = np.asarray(img.crop((l, t, l + W, t + H)), dtype=np.float32) / 255.0

mask = np.ones((H, W), dtype=bool)
def rect(x0, y0, x1, y1):
    mask[y0:y1, x0:x1] = False
rect(0, 0, W, 40)
rect(120, 55, 260, 205)
rect(25, 250, 350, 345)
rect(25, 375, 350, 490)
rect(30, 500, 345, 535)
rect(30, 540, 345, 570)
rect(55, 580, 320, 700)
rect(0, 780, W, 812)
mask[::3, ::3] = False  # 下采样，加速

T = top[mask]           # N,3
U = ui_a[mask]

def hard_light(base, top):
    return np.where(top <= 0.5, 2 * base * top, 1 - 2 * (1 - base) * (1 - top))

best = None
for r in range(0, 40, 4):
    for g in range(80, 130, 4):
        for b in range(235, 256, 5):
            c = np.array([r, g, b], dtype=np.float32) / 255.0
            hl = hard_light(c, T)
            for a in [0.2, 0.25, 0.3, 0.35, 0.4, 0.5, 0.6]:
                out = c * (1 - a) + hl * a
                diff = out - U
                mse = float(np.mean(diff * diff))
                if best is None or mse < best[0]:
                    best = (round(mse, 5), r, g, b, a)
print('best', best)
