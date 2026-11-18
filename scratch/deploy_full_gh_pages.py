import subprocess
import shutil
import os

print("=== DEPLOYING TO GH-PAGES ===")

# 1. Read files and list assets from main
site_dir = "site"
with open(os.path.join(site_dir, "index.html"), "r", encoding="utf-8") as f:
    index_content = f.read()

with open(os.path.join(site_dir, "styles.css"), "r", encoding="utf-8") as f:
    styles_content = f.read()

# Copy all assets from site/assets to temporary scratch folder so we have them when switching branches
scratch_assets = "scratch/temp_deploy_assets"
if os.path.exists(scratch_assets):
    shutil.rmtree(scratch_assets)
shutil.copytree(os.path.join(site_dir, "assets"), scratch_assets)
print(f"Copied {len(os.listdir(scratch_assets))} assets to temp folder")

# 2. Checkout gh-pages
subprocess.run(["git", "checkout", "gh-pages"], check=True)

# 3. Update root files
with open("index.html", "w", encoding="utf-8") as f:
    f.write(index_content)

with open("styles.css", "w", encoding="utf-8") as f:
    f.write(styles_content)

# Copy assets to assets/ in gh-pages
os.makedirs("assets", exist_ok=True)
for item in os.listdir(scratch_assets):
    s = os.path.join(scratch_assets, item)
    d = os.path.join("assets", item)
    if os.path.isfile(s):
        shutil.copy2(s, d)
    elif os.path.isdir(s):
        if os.path.exists(d):
            shutil.rmtree(d)
        shutil.copytree(s, d)

# Ensure CNAME exists
if not os.path.exists("CNAME"):
    with open("CNAME", "w", encoding="utf-8") as f:
        f.write("trdconsultoria.com.br\n")

# Remove any old senar files if they exist on gh-pages
for old_file in ["assets/logo-senar.svg", "assets/logo-senar-green.svg"]:
    if os.path.exists(old_file):
        os.remove(old_file)
        print(f"Removed {old_file}")

# 4. Stage and commit
subprocess.run(["git", "add", "index.html", "styles.css", "assets/", "CNAME"], check=True)

diff_proc = subprocess.run(["git", "diff", "--staged", "--stat"], capture_output=True, text=True)
print("Staged diff on gh-pages:")
print(diff_proc.stdout)

if diff_proc.stdout.strip():
    subprocess.run(["git", "commit", "-m", "deploy(live): carrossel infinito kimi-style, logos em alta definicao e founder card atualizado"], check=True)
    subprocess.run(["git", "push", "origin", "gh-pages"], check=True)
    print("=== GH-PAGES PUSHED SUCCESSFULLY! ===")
else:
    print("No changes to commit on gh-pages.")

# 5. Switch back to main
subprocess.run(["git", "checkout", "main"], check=True)
print("=== BACK ON MAIN BRANCH ===")
