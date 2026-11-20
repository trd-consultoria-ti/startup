package utils_strings

import (
	"math"
	"regexp"
	"strings"
)

var reEntidadeXML = regexp.MustCompile(`^&(?:amp|lt|gt|quot|apos|#[0-9]+|#x[0-9a-fA-F]+);`)

func SanitizarAmpersand(xmlBytes []byte) []byte {

	s := string(xmlBytes)
	if !strings.Contains(s, "&") {
		return xmlBytes
	}

	var b strings.Builder
	b.Grow(len(s) + 16)

	for i := 0; i < len(s); i++ {
		if s[i] == '&' && !reEntidadeXML.MatchString(s[i:]) {
			b.WriteString("&amp;")
			continue
		}
		b.WriteByte(s[i])
	}

	return []byte(b.String())
}
func TruncarTexto(texto string, maxLinhas, maxCars int) string {

	if texto == "" || maxLinhas <= 0 || maxCars <= 0 {
		return ""
	}

	segmentos := strings.Split(texto, "\n")
	var out []string
	usadas := 0
	truncou := false

	for _, seg := range segmentos {

		if usadas >= maxLinhas {
			truncou = true
			break
		}

		runes := []rune(seg)
		necess := 1
		if len(runes) > 0 {
			necess = int(math.Ceil(float64(len(runes)) / float64(maxCars)))
		}

		if usadas+necess <= maxLinhas {
			out = append(out, seg)
			usadas += necess
			continue
		}

		limite := (maxLinhas - usadas) * maxCars
		if len(runes) > limite {
			out = append(out, string(runes[:limite]))
		} else {
			out = append(out, seg)
		}

		usadas = maxLinhas
		truncou = true
		break
	}

	res := strings.Join(out, "\n")
	if truncou {
		res = strings.TrimRight(res, " \n") + "..."
	}

	return res
}

// TruncarPorTamanho corta o texto para no máximo maxCars caracteres (preservando quebras de linha
// internas) e adiciona reticências quando precisar cortar, reservando 3 caracteres para elas.
func TruncarPorTamanho(texto string, maxCars int) string {

	runes := []rune(texto)
	if len(runes) <= maxCars {
		return texto
	}

	limite := maxCars - 3
	if limite < 0 {
		limite = 0
	}

	return strings.TrimRight(string(runes[:limite]), " \n") + "..."
}

// TruncarComReticencias corta o conteúdo em maxConteudo caracteres e reserva
// três caracteres adicionais para as reticências quando houver corte.
func TruncarComReticencias(texto string, maxConteudo int) string {
	runes := []rune(texto)
	if len(runes) <= maxConteudo {
		return texto
	}
	if maxConteudo < 0 {
		maxConteudo = 0
	}
	return strings.TrimRight(string(runes[:maxConteudo]), " \n") + "..."
}
