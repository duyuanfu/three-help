from PIL import Image
import numpy as np

ui = Image.open('docs/ui/登录页.png').convert('RGB')
W, H = ui.size
ui_a = np.asarray(ui, dtype=np.float32) / 255.0

base_img = Image.open('docs/_static_backup/login/bg-clean.jpg').convert('RGB')
# aspectFill cover 375x812
bw, bh = base_img.size
scale = max(W / bw, H / bh)
nw, nh = int(round(bw * scale)), int(round(bh * scale))
base_img = base_img.resize((nw, nh), Image.LANCZOS)
left = (nw - W) // 2
top = (nh - H) // 2
base_img = base_img.crop((left, top, left + W, top + H))
base = np.asarray(base_img, dtype=np.float32) / 255.0

# 掩掉所有 UI 元素区域（相对 375x812）
mask = np.ones((H, W), dtype=bool)
def rect(x0, y0, x1, y1):
    mask[y0:y1, x0:x1] = False

rect(0, 0, W, 40)        # 状态栏
rect(120, 55, 260, 205)  # logo + 标题
rect(25, 250, 350, 345)  # 输入框
rect(25, 375, 350, 490)  # 按钮
rect(30, 500, 345, 535)  # 辅助链接
rect(30, 540, 345, 570)  # 其它登录方式
rect(55, 580, 320, 700)  # 第三方图标
rect(0, 780, W, 812)     # home indicator

def hard_light(b, c):
    return np.where(c <= 0.5, 2 * b * c, 1 - 2 * (1 - b) * (1 - c))

best = None
for r in range(10, 45, 5):
    for g in range(85, 135, 5):
        for bl in range(225, 256, 5):
            c = np.array([r, g, bl], dtype=np.float32) / 255.0
            for a in [0.20, 0.22, 0.25, 0.28, 0.30, 0.33]:
                out = base * (1 - a) + hard_light(base, c) * a
                diff = (out - ui_a)[mask]
                mse = float(np.mean(diff * diff))
                if best is None or mse < best[0]:
                    best = (mse, r, g, bl, a)
print('best MSE', best[0], 'color', best[1:4], 'alpha', best[4])
