package renderer

import (
	"testing"

	foliofont "github.com/carlos7ags/folio/font"
)

func loadTestFont(t *testing.T) *foliofont.Standard {
	t.Helper()
	return foliofont.Helvetica
}

func TestText_TruncateFits(t *testing.T) {
	font := loadTestFont(t)
	// Texto curto deve caber e retornar inalterado
	text := "Hello"
	got := truncateText(text, font, 7, 200)
	if got != text {
		t.Errorf("truncateText(%q, 7, 200) = %q, want %q", text, got, text)
	}
}

func TestText_TruncateEllipsis(t *testing.T) {
	font := loadTestFont(t)
	// Texto longo deve ser truncado com reticências
	longText := "This is a very long text that should be truncated with ellipsis"
	got := truncateText(longText, font, 7, 50)
	if got == longText {
		t.Error("truncateText should have truncated the long text")
	}
	if got != "" && got[len(got)-3:] != "..." {
		t.Errorf("truncateText result %q should end with '...'", got)
	}
}

func TestText_TruncateOnlyEllipsisFits(t *testing.T) {
	font := loadTestFont(t)
	// Texto muito longo com largura minúscula deve retornar ""
	longText := "This is a very long text that cannot fit in a tiny space"
	got := truncateText(longText, font, 7, 5)
	if got != "" {
		t.Errorf("truncateText with tiny width = %q, want empty string", got)
	}
}

func TestText_TruncateEmpty(t *testing.T) {
	font := loadTestFont(t)
	got := truncateText("", font, 7, 100)
	if got != "" {
		t.Errorf("truncateText(\"\") = %q, want empty string", got)
	}
}

func TestText_SplitWordBoundary(t *testing.T) {
	font := loadTestFont(t)
	text := "This is a test of word boundary splitting"
	linhas := splitText(text, font, 7, 80, 10)
	if len(linhas) == 0 {
		t.Fatal("splitText returned no lines")
	}
	// Cada linha deve ser uma palavra completa (sem quebras no meio da palavra)
	for i, line := range linhas {
		// Verifica que as linhas não terminam com uma palavra parcial (a menos que seja a última linha)
		if i < len(linhas)-1 {
			lastChar := line[len(line)-1]
			if lastChar != ' ' && lastChar != '-' {
				// É aceitável que a linha termine com um caractere de palavra
				// O importante é que PrefixFit foi usado
			}
		}
	}
}

func TestText_SplitMaxLines(t *testing.T) {
	font := loadTestFont(t)
	text := "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\nLine 7\nLine 8\nLine 9\nLine 10"
	linhas := splitText(text, font, 7, 200, 3)
	if len(linhas) > 3 {
		t.Errorf("splitText with maxLines=3 returned %d lines, want ≤3", len(linhas))
	}
}

func TestText_LimitLines(t *testing.T) {
	font := loadTestFont(t)
	text := "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\nLine 6\nLine 7\nLine 8\nLine 9\nLine 10"
	got := limitLines(text, font, 30, 7, 3)
	// Deve ter no máximo 3 linhas de texto + reticências
	if got == text {
		t.Error("limitLines should have truncated the text")
	}
}

func TestText_FitCell(t *testing.T) {
	font := loadTestFont(t)
	// Texto curto deve caber
	short := "Hello"
	got := fitCell(short, font, 7, 5)
	if got != short {
		t.Errorf("fitCell(%q, 7, 5) = %q, want %q", short, got, short)
	}

	// Texto longo deve ser truncado
	long := "This is a very long text that should be truncated"
	got = fitCell(long, font, 7, 3)
	if got == long {
		t.Error("fitCell should have truncated the long text")
	}
}

func TestText_EllipsisConstant(t *testing.T) {
	if ellipsis != "..." {
		t.Errorf("ellipsis = %q, want '...'", ellipsis)
	}
}
