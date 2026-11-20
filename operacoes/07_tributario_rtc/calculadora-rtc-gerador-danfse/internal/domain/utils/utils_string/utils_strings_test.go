package utils_strings

import (
	"testing"
)

func TestSanitizarAmpersand_ValidEntitiesPreserved(t *testing.T) {
	input := []byte(`&amp; &lt; &gt; &quot; &apos; &#65; &#x41;`)
	result := SanitizarAmpersand(input)
	got := string(result)
	if got != `&amp; &lt; &gt; &quot; &apos; &#65; &#x41;` {
		t.Fatalf("valid XML entities should be preserved, got %q", got)
	}
}

func TestSanitizarAmpersand_LooseAmpersandEscaped(t *testing.T) {
	input := []byte(`A & B`)
	result := SanitizarAmpersand(input)
	got := string(result)
	if got != `A &amp; B` {
		t.Fatalf("loose ampersand should be escaped, got %q", got)
	}
}

func TestSanitizarAmpersand_NoAmpersand(t *testing.T) {
	input := []byte(`no ampersand here`)
	result := SanitizarAmpersand(input)
	got := string(result)
	if got != `no ampersand here` {
		t.Fatalf("input without ampersand should be unchanged, got %q", got)
	}
}

func TestSanitizarAmpersand_Mixed(t *testing.T) {
	input := []byte(`safe &amp; unsafe & evil`)
	result := SanitizarAmpersand(input)
	got := string(result)
	if got != `safe &amp; unsafe &amp; evil` {
		t.Fatalf("mixed input: expected safe &amp; unsafe &amp; evil, got %q", got)
	}
}
