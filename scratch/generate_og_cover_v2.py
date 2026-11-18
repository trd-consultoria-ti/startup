from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1200, 630

# Load keyvisual
kv_path = "C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/9eb6f428-2b94-48f9-8194-60390591d5a1/trd_og_cover_keyvisual_1794971726841.jpg"
kv = Image.open(kv_path)

# Crop/scale keyvisual to fit 1200x630
kv_resized = kv.resize((1200, 685), Image.Resampling.LANCZOS)
top_crop = (685 - 630) // 2
bg = kv_resized.crop((0, top_crop, 1200, top_crop + 630))

# Create a gradient overlay from left to right (dark navy #080D1A on the left, fading to transparent on the right)
overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
draw_ov = ImageDraw.Draw(overlay)

for x in range(W):
    if x < 480:
        alpha = 248
    elif x < 960:
        ratio = (x - 480) / (960 - 480)
        alpha = int(248 * (1.0 - ratio))
    else:
        alpha = 0
    draw_ov.line([(x, 0), (x, H)], fill=(8, 13, 26, alpha))

bg.paste(overlay, (0, 0), overlay)

draw = ImageDraw.Draw(bg)

# Fonts
font_bold = "C:/Windows/Fonts/segoeuib.ttf"
font_reg = "C:/Windows/Fonts/segoeui.ttf"

f_brand_trd = ImageFont.truetype(font_bold, 28)
f_brand_sub = ImageFont.truetype(font_bold, 11)
f_pill = ImageFont.truetype(font_bold, 12)
f_h1 = ImageFont.truetype(font_bold, 44)
f_sub = ImageFont.truetype(font_reg, 19)
f_trust_title = ImageFont.truetype(font_bold, 11)
f_trust_item = ImageFont.truetype(font_bold, 12)

# 1. Micro-Brand (Keystone + TRD CONSULTORIA DE TI)
ks_x, ks_y = 70, 52
ks_pts = [
    (ks_x + 18, ks_y),
    (ks_x + 36, ks_y + 10),
    (ks_x + 36, ks_y + 31),
    (ks_x + 18, ks_y + 41),
    (ks_x, ks_y + 31),
    (ks_x, ks_y + 10)
]
draw.polygon(ks_pts, fill=(248, 250, 252))
draw.line([(ks_x + 18, ks_y + 20), (ks_x + 18, ks_y)], fill=(8, 13, 26), width=3)
draw.line([(ks_x + 18, ks_y + 20), (ks_x, ks_y + 31)], fill=(8, 13, 26), width=3)
draw.line([(ks_x + 18, ks_y + 20), (ks_x + 36, ks_y + 31)], fill=(8, 13, 26), width=3)

draw.text((ks_x + 48, ks_y - 2), "TRD", font=f_brand_trd, fill=(248, 250, 252))
draw.text((ks_x + 50, ks_y + 26), "CONSULTORIA DE TI", font=f_brand_sub, fill=(156, 163, 175))

# Top Pill: Anthropic Certified & Mission Critical
pill_text = "ENGENHARIA DE MISSÃO CRÍTICA · ANTHROPIC ACADEMY CERTIFIED"
bbox = f_pill.getbbox(pill_text)
pill_w = bbox[2] - bbox[0] + 36
pill_h = 32
pill_x = 70
pill_y = 120

draw.rounded_rectangle([pill_x, pill_y, pill_x + pill_w, pill_y + pill_h], radius=16, fill=(245, 158, 11, 28), outline=(245, 158, 11, 90), width=1)
# Small amber dot
draw.ellipse([pill_x + 12, pill_y + 12, pill_x + 20, pill_y + 20], fill=(245, 158, 11))
draw.text((pill_x + 26, pill_y + 7), pill_text, font=f_pill, fill=(251, 191, 36))

# H1 Headline
h1_line1 = "Sistemas Sob Medida"
h1_line2 = "& Sustentação com SLA"
draw.text((70, 175), h1_line1, font=f_h1, fill=(248, 250, 252))
draw.text((70, 230), h1_line2, font=f_h1, fill=(245, 158, 11))

# Subheadline / Value Proposition
sub_line1 = "Criação ágil de CRMs, Hubs de Gestão e Mini-ERPs em 2 a 4 semanas."
sub_line2 = "Código 100% seu (sem mensalidade por usuário) e SLA P1 em até 2 horas."
draw.text((70, 310), sub_line1, font=f_sub, fill=(203, 213, 225))
draw.text((70, 342), sub_line2, font=f_sub, fill=(148, 163, 184))

# Key Metrics Cards / Highlights
b1_x, b1_y = 70, 395
b1_w, b1_h = 295, 105
draw.rounded_rectangle([b1_x, b1_y, b1_x + b1_w, b1_y + b1_h], radius=10, fill=(15, 23, 42, 230), outline=(51, 65, 85, 200), width=1)
f_card_tag = ImageFont.truetype(font_bold, 11)
f_card_h = ImageFont.truetype(font_bold, 15)
f_card_sub = ImageFont.truetype(font_reg, 12)
draw.text((b1_x + 16, b1_y + 14), "CRIAÇÃO SOB MEDIDA", font=f_card_tag, fill=(245, 158, 11))
draw.text((b1_x + 16, b1_y + 36), "Sistemas Starter & Pro", font=f_card_h, fill=(248, 250, 252))
draw.text((b1_x + 16, b1_y + 60), "A partir de R$ 9.800 · Até 12x no Cartão\nEntrega em 2 a 4 semanas · Código 100% seu", font=f_card_sub, fill=(203, 213, 225))

# Box 2: Sustentação de Missão Crítica
b2_x = b1_x + b1_w + 15
draw.rounded_rectangle([b2_x, b1_y, b2_x + b1_w, b1_y + b1_h], radius=10, fill=(15, 23, 42, 230), outline=(51, 65, 85, 200), width=1)
draw.text((b2_x + 16, b1_y + 14), "SUSTENTAÇÃO CORPORATIVA", font=f_card_tag, fill=(56, 189, 248))
draw.text((b2_x + 16, b1_y + 36), "SLA P1 em até 2h Úteis", font=f_card_h, fill=(248, 250, 252))
draw.text((b2_x + 16, b1_y + 60), "Performance Tuning SQL (-99,7% I/O)\nRetaguarda sênior a partir de R$ 1.800/mês", font=f_card_sub, fill=(203, 213, 225))

# Trustbar at bottom
draw.text((70, 545), "EXPERIÊNCIA COMPROVADA:", font=f_trust_title, fill=(100, 116, 139))
f_trust_item = ImageFont.truetype(font_bold, 10)
clients = ["IMPRENSA NACIONAL (DOU)", "INEP · ENADE", "MINISTÉRIO DA JUSTIÇA", "SERASA EXPERIAN", "CVM", "SENAR"]
cx = 250
for c in clients:
    c_bbox = f_trust_item.getbbox(c)
    cw = c_bbox[2] - c_bbox[0] + 18
    if cx + cw > 1150:
        break
    draw.rounded_rectangle([cx, 538, cx + cw, 566], radius=6, fill=(15, 23, 42, 200), outline=(51, 65, 85, 180), width=1)
    draw.text((cx + 9, 546), c, font=f_trust_item, fill=(226, 232, 240))
    cx += cw + 10

# Save output to both brain and site/assets/og-cover.png
out_brain = "C:/Users/Thiago Duarte/.gemini/antigravity-cli/brain/6bbf3b1b-3f2a-4754-9903-a79b7c5a2507/og_cover_v2.png"
out_site = "site/assets/og-cover.png"
bg.save(out_brain, format="PNG")
bg.save(out_site, format="PNG")
print("Saved og_cover_v2.png to site/assets/og-cover.png!")
