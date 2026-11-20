package renderer

import "testing"

func TestRenderer_StylesCellStyles(t *testing.T) {
	if estiloCampo.recuoEsquerdoCM != fieldTextInsetCM || estiloCampo.recuoDireitoCM != fieldTextInsetCM {
		t.Fatalf("campo inset = %.3f/%.3f, want %.3f", estiloCampo.recuoEsquerdoCM, estiloCampo.recuoDireitoCM, fieldTextInsetCM)
	}
	if estiloIdentificacao.sombreado || !estiloIdentificacaoSombreada.sombreado {
		t.Fatal("identification shading variants are inverted")
	}
	if estiloIdentificacao.recuoEsquerdoCM != fieldTextInsetCM || estiloIdentificacaoSombreada.recuoEsquerdoCM != fieldTextInsetCM {
		t.Fatalf("identification inset is not centralized: %.3f/%.3f", estiloIdentificacao.recuoEsquerdoCM, estiloIdentificacaoSombreada.recuoEsquerdoCM)
	}
	if estiloIdentificacao.tamanhoRotuloPt != titleSizePt || estiloIdentificacao.tamanhoValorPt != valueSizePt {
		t.Fatal("identification typography is not centralized")
	}
}
