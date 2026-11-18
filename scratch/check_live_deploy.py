import urllib.request
import time

url = "https://trdconsultoria.com.br/styles.css"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0", "Cache-Control": "no-cache"})

try:
    with urllib.request.urlopen(req) as response:
        css = response.read().decode("utf-8")
        has_nowrap = "white-space: nowrap" in css
        has_cards = "badge-sla" in css
        print(f"Live CSS updated? has_nowrap: {has_nowrap}, has_cards: {has_cards}")
except Exception as e:
    print(f"Error fetching live CSS: {e}")
