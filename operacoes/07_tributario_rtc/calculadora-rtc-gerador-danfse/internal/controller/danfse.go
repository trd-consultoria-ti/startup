package controller

import (
	"bytes"
	"danfse-service/internal/domain/enums"
	"danfse-service/internal/service"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v5"
)

type DanfseController struct {
	servicoGeracaoPdf service.ServicoGeracaoPdf
}

// GerarPdf godoc
// @Summary      Gerar DANFSe em PDF com base no xml da nota.
// @Description  Gera um DANFSe em PDF a partir do XML bruto da NFS-e nacional. Envie o conteúdo completo do XML com Content-Type application/xml.
// @Tags         DANFSe
// @Accept       application/xml
// @Produce      application/pdf
// @Param        stc      query   string  false  "Situação da nota: 0=Válida, 1=Substituída, 2=Cancelada" Enums(0,1,2) default(0)
// @Param        canhoto  query   bool    false  "Exibir canhoto (recibo) no PDF. Ausente ou true habilita; false oculta." default(true)
// @Param        xml      body    string  true   "XML completo da NFS-e nacional (elemento raiz NFSe). Envie o XML bruto com Content-Type application/xml."
// @Success      200  {file}    file            "DANFSe em PDF"
// @Failure      400  {object}  controller.ErroResponse
// @Router       /api/danfse [post]
func (d *DanfseController) GerarPdf(c *echo.Context) error {
	dadosXML, situacao, canhoto, err := lerEntrada(c)
	if err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err.Error())
	}

	ctx := c.Request().Context()

	pdf, err := d.servicoGeracaoPdf.Executar(ctx, dadosXML, situacao, canhoto)
	if err != nil {
		return echo.NewHTTPError(http.StatusBadRequest, err.Error())
	}

	c.Response().Header().Set(echo.HeaderContentDisposition, `attachment; filename="danfse.pdf"`)
	return c.Blob(http.StatusOK, "application/pdf", pdf)
}

func lerEntrada(c *echo.Context) ([]byte, enums.SituacaoNotaTipo, bool, error) {
	situacao, err := resolverSituacao(c)
	if err != nil {
		return nil, enums.VALIDA, true, err
	}

	canhoto, err := resolverCanhoto(c)
	if err != nil {
		return nil, situacao, true, err
	}

	dadosXML, err := io.ReadAll(c.Request().Body)
	if err != nil {
		return nil, situacao, canhoto, err
	}

	if len(bytes.TrimSpace(dadosXML)) == 0 {
		return nil, situacao, canhoto, errors.New("Xml não informado.")
	}

	return dadosXML, situacao, canhoto, nil
}

func resolverSituacao(c *echo.Context) (enums.SituacaoNotaTipo, error) {

	if stc := c.QueryParam("stc"); stc != "" {
		valorSTC, err := strconv.Atoi(stc)
		if err != nil {
			return enums.VALIDA, fmt.Errorf("parâmetro stc inválido: deve ser 0, 1 ou 2")
		}

		situacao, ok := enums.SituacaoNotaPorSTC(valorSTC)
		if !ok {
			return enums.VALIDA, fmt.Errorf("parâmetro stc inválido: deve ser 0, 1 ou 2")
		}
		return situacao, nil
	}

	return enums.VALIDA, nil
}

func resolverCanhoto(c *echo.Context) (bool, error) {
	val := c.QueryParam("canhoto")
	if val == "" {
		return true, nil
	}
	valorMinusculo := strings.ToLower(val)
	if valorMinusculo == "true" {
		return true, nil
	}
	if valorMinusculo == "false" {
		return false, nil
	}
	return true, fmt.Errorf("parâmetro canhoto inválido: deve ser true ou false")
}

func NewDanfseController(servicoGeracaoPdf service.ServicoGeracaoPdf) *DanfseController {
	return &DanfseController{
		servicoGeracaoPdf: servicoGeracaoPdf,
	}
}
