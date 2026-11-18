import os

def find_images(base_dir):
    res = []
    for root, dirs, files in os.walk(base_dir):
        if ".venv" in root or "node_modules" in root or ".git" in root:
            continue
        for f in files:
            ext = os.path.splitext(f)[1].lower()
            if ext in [".png", ".jpg", ".jpeg", ".svg", ".webp"]:
                p = os.path.join(root, f)
                size = os.path.getsize(p)
                res.append((p, size))
    return res

print("Images in E:\\Projeto Moa:")
for p, s in find_images(r"E:\Projeto Moa"):
    print(f" - {p} ({s} bytes)")

print("\nImages in E:\\projetos\\Animal World:")
for p, s in find_images(r"E:\projetos\Animal World"):
    print(f" - {p} ({s} bytes)")
