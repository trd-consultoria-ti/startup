package enums

type SituacaoNotaTipo string

const (
	VALIDA      SituacaoNotaTipo = "valida"
	SUBSTITUIDA SituacaoNotaTipo = "substituida"
	CANCELADA   SituacaoNotaTipo = "cancelada"
)

func SituacaoNotaPorSTC(stc int) (SituacaoNotaTipo, bool) {
	switch stc {
	case 0:
		return VALIDA, true
	case 1:
		return SUBSTITUIDA, true
	case 2:
		return CANCELADA, true
	default:
		return VALIDA, false
	}
}

func (s SituacaoNotaTipo) MarcaDagua() string {
	switch s {
	case CANCELADA:
		return "CANCELADA"
	case SUBSTITUIDA:
		return "SUBSTITUÍDA"
	default:
		return ""
	}
}
