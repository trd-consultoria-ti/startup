import re

with open("site/index.html", "r", encoding="utf-8") as f:
    html = f.read()

with open("site/styles.css", "r", encoding="utf-8") as f:
    css = f.read()

html_classes = set()
for m in re.findall(r'class="([^"]+)"', html):
    for c in m.split():
        html_classes.add(c)

css_classes = set(re.findall(r'\.([a-zA-Z0-9_\-]+)', css))

missing = html_classes - css_classes
print("Total HTML classes:", len(html_classes))
print("Missing classes in CSS:", sorted(list(missing)))
