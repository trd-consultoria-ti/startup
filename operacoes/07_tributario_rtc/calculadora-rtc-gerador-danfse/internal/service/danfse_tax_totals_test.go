package service

import (
	"danfse-service/internal/domain/models"
	"testing"
)

func TestMontarLinhaTotaisTributos_Monetario(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{
									VTotTribFed: 150.00,
									VTotTribEst: 80.00,
									VTotTribMun: 50.00,
								},
							},
						},
					},
				},
			},
		},
	}

	got := montarLinhaTotaisTributos(nota)
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 150,00; Estaduais: R$ 80,00; Municipais: R$ 50,00;"
	if got != want {
		t.Fatalf("monetario: esperado %q, obtido %q", want, got)
	}
}

func TestMontarLinhaTotaisTributos_MonetarioComValoresGrandes(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{
									VTotTribFed: 1234.56,
									VTotTribEst: 98765.43,
									VTotTribMun: 0,
								},
							},
						},
					},
				},
			},
		},
	}

	got := montarLinhaTotaisTributos(nota)
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 1.234,56; Estaduais: R$ 98.765,43; Municipais: R$ 0,00;"
	if got != want {
		t.Fatalf("monetario grande: esperado %q, obtido %q", want, got)
	}
}

func TestMontarLinhaTotaisTributos_PorcentagemFallback(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{
									VTotTribFed: 0,
									VTotTribEst: 0,
									VTotTribMun: 0,
								},
								PTotTrib: models.PTotTrib{
									PTotTribFed: 5.50,
									PTotTribEst: 7.25,
									PTotTribMun: 2.00,
								},
							},
						},
					},
				},
			},
		},
	}

	got := montarLinhaTotaisTributos(nota)
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: 5,50%; Estaduais: 7,25%; Municipais: 2,00%;"
	if got != want {
		t.Fatalf("porcentagem: esperado %q, obtido %q", want, got)
	}
}

func TestMontarLinhaTotaisTributos_EmptyFallback(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{},
								PTotTrib: models.PTotTrib{},
							},
						},
					},
				},
			},
		},
	}

	got := montarLinhaTotaisTributos(nota)
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 0,00; Estaduais: R$ 0,00; Municipais: R$ 0,00;"
	if got != want {
		t.Fatalf("vazio: esperado %q, obtido %q", want, got)
	}
}

func TestMontarLinhaTotaisTributos_MixedZeroPositive(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{
									VTotTribFed: 0,
									VTotTribEst: 0,
									VTotTribMun: 25.00,
								},
								PTotTrib: models.PTotTrib{
									PTotTribFed: 10.00,
									PTotTribEst: 0,
									PTotTribMun: 0,
								},
							},
						},
					},
				},
			},
		},
	}

	// Deve priorizar monetário (tem um valor > 0)
	got := montarLinhaTotaisTributos(nota)
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 0,00; Estaduais: R$ 0,00; Municipais: R$ 25,00;"
	if got != want {
		t.Fatalf("mix zero/positive: esperado %q, obtido %q", want, got)
	}
}

func TestMontarLinhaTotaisTributos_PorcentagemSomenteUm(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{},
								PTotTrib: models.PTotTrib{
									PTotTribFed: 0,
									PTotTribEst: 0,
									PTotTribMun: 15.50,
								},
							},
						},
					},
				},
			},
		},
	}

	got := montarLinhaTotaisTributos(nota)
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: 0,00%; Estaduais: 0,00%; Municipais: 15,50%;"
	if got != want {
		t.Fatalf("porcentagem um: esperado %q, obtido %q", want, got)
	}
}

func TestMontarLinhaTotaisTributos_MonetarioPrecendePorcentagem(t *testing.T) {
	nota := &models.NFSe{
		InfNFSe: models.InfNFSe{
			DPS: models.DPS{
				InfDPS: models.InfDPS{
					Valores: models.ValoresDPS{
						Trib: models.Trib{
							TotTrib: models.TotTrib{
								VTotTrib: models.VTotTrib{
									VTotTribFed: 100.00,
									VTotTribEst: 50.00,
									VTotTribMun: 25.00,
								},
								PTotTrib: models.PTotTrib{
									PTotTribFed: 10.00,
									PTotTribEst: 5.00,
									PTotTribMun: 2.50,
								},
							},
						},
					},
				},
			},
		},
	}

	got := montarLinhaTotaisTributos(nota)
	// Deve usar monetário porque há valores monetários > 0
	want := "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 100,00; Estaduais: R$ 50,00; Municipais: R$ 25,00;"
	if got != want {
		t.Fatalf("precedencia monetario: esperado %q, obtido %q", want, got)
	}
}
