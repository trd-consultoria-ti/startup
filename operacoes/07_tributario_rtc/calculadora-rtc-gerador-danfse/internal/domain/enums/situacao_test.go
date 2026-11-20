package enums

import "testing"

func TestSituacaoNotaPorSTC(t *testing.T) {
	tests := []struct {
		name  string
		stc   int
		want  SituacaoNotaTipo
		found bool
	}{
		{name: "valida", stc: 0, want: VALIDA, found: true},
		{name: "substituida", stc: 1, want: SUBSTITUIDA, found: true},
		{name: "cancelada", stc: 2, want: CANCELADA, found: true},
		{name: "desconhecida", stc: 3, want: VALIDA, found: false},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			got, found := SituacaoNotaPorSTC(tc.stc)
			if got != tc.want || found != tc.found {
				t.Fatalf("SituacaoNotaPorSTC(%d) = (%q, %v), want (%q, %v)", tc.stc, got, found, tc.want, tc.found)
			}
		})
	}
}

func TestSituacaoNotaTipo_MarcaDagua(t *testing.T) {
	tests := []struct {
		situacao SituacaoNotaTipo
		want     string
	}{
		{situacao: VALIDA, want: ""},
		{situacao: SUBSTITUIDA, want: "SUBSTITUÍDA"},
		{situacao: CANCELADA, want: "CANCELADA"},
		{situacao: SituacaoNotaTipo("desconhecida"), want: ""},
	}

	for _, tc := range tests {
		if got := tc.situacao.MarcaDagua(); got != tc.want {
			t.Errorf("%q.MarcaDagua() = %q, want %q", tc.situacao, got, tc.want)
		}
	}
}
