package service

import (
	"danfse-service/internal/domain/models"
	"danfse-service/internal/domain/presentation"
	"fmt"
)

// montarLinhaTotaisTributos gera a linha fixa de totais aproximados de tributos
// conforme Lei nº 12.741/2012. A prioridade é: qualquer total monetário positivo
// => saída monetária; caso contrário, qualquer percentual positivo => saída
// percentual; caso contrário, três hífens.
func montarLinhaTotaisTributos(nota *models.NFSe) string {
	totaisTributos := nota.InfNFSe.DPS.InfDPS.Valores.Trib.TotTrib

	valorFederal := totaisTributos.VTotTrib.VTotTribFed
	valorEstadual := totaisTributos.VTotTrib.VTotTribEst
	valorMunicipal := totaisTributos.VTotTrib.VTotTribMun

	if valorFederal > 0 || valorEstadual > 0 || valorMunicipal > 0 {
		return fmt.Sprintf(
			"Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: %s; Estaduais: %s; Municipais: %s;",
			presentation.FormatarMoeda(valorFederal),
			presentation.FormatarMoeda(valorEstadual),
			presentation.FormatarMoeda(valorMunicipal),
		)
	}

	percentualFederal := totaisTributos.PTotTrib.PTotTribFed
	percentualEstadual := totaisTributos.PTotTrib.PTotTribEst
	percentualMunicipal := totaisTributos.PTotTrib.PTotTribMun

	if percentualFederal > 0 || percentualEstadual > 0 || percentualMunicipal > 0 {
		return fmt.Sprintf(
			"Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: %s; Estaduais: %s; Municipais: %s;",
			presentation.FormatarPercentual(percentualFederal),
			presentation.FormatarPercentual(percentualEstadual),
			presentation.FormatarPercentual(percentualMunicipal),
		)
	}

	return "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 0,00; Estaduais: R$ 0,00; Municipais: R$ 0,00;"
}
