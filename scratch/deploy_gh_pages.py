import subprocess
import shutil
import os

print("Starting gh-pages deployment...")

# Read updated files from site/
with open("site/index.html", "r", encoding="utf-8") as f:
    index_content = f.read()

with open("site/styles.css", "r", encoding="utf-8") as f:
    styles_content = f.read()

# Checkout gh-pages
subprocess.run(["git", "checkout", "gh-pages"], check=True)

# Write updated files to root
with open("index.html", "w", encoding="utf-8") as f:
    f.write(index_content)

with open("styles.css", "w", encoding="utf-8") as f:
    f.write(styles_content)

# Stage, commit and push
subprocess.run(["git", "add", "index.html", "styles.css"], check=True)

diff_proc = subprocess.run(["git", "diff", "--staged"], capture_output=True, text=True)
if diff_proc.stdout.strip():
    subprocess.run(["git", "commit", "-m", "deploy(live): responsividade mobile definitiva, zero scroll lateral no nav, terminal e cards de SLA"], check=True)
    subprocess.run(["git", "push", "origin", "gh-pages"], check=True)
    print("gh-pages pushed successfully!")
else:
    print("No changes to commit on gh-pages.")

# Checkout main back
subprocess.run(["git", "checkout", "main"], check=True)
print("Back on main branch. Deploy complete!")
