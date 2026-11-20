package controller

import (
	"errors"
	"net/http"

	"github.com/labstack/echo/v5"
)

// TratarErrosHTTP é o echo.HTTPErrorHandler centralizado.
// Converte erros do handler em ResponseDTO, mantendo o contrato
// de erro da API ({"sucesso": false, "mensagem": "..."}).
//
// Privacidade: respostas 5xx usam sempre mensagem genérica (nunca revelam
// err.Error(), que poderia carregar XML/dados fiscais). Erros 4xx mantêm a
// mensagem de validação original, que é de apresentação para o cliente.
// Falhas ao escrever a resposta de erro são registradas sem expor o erro cru.
func TratarErrosHTTP(c *echo.Context, err error) {

	response, unwrapErr := echo.UnwrapResponse(c.Response())
	if unwrapErr != nil {
		return
	}

	if response.Committed {
		return
	}

	status := http.StatusInternalServerError
	mensagem := http.StatusText(status)

	var httpError *echo.HTTPError
	if errors.As(err, &httpError) {
		status = httpError.Code
		if status >= http.StatusInternalServerError {
			// 5xx genérico: não expõe o erro interno no corpo da resposta.
			mensagem = http.StatusText(status)
		} else {
			mensagem = httpError.Message
		}
	} else if statusCode := echo.StatusCode(err); statusCode > 0 {
		status = statusCode
		if status >= http.StatusInternalServerError {
			// 5xx genérico: não expõe o erro interno no corpo da resposta.
			mensagem = http.StatusText(status)
		} else if text := http.StatusText(status); text != "" {
			mensagem = text
		} else {
			mensagem = err.Error()
		}
	} else {
		// Erro sem status implícito: responde 500 genérico, sem vazar o erro.
		status = http.StatusInternalServerError
		mensagem = http.StatusText(status)
	}

	if !response.Committed {
		if writeErr := c.JSON(status, ErroResponse{
			Sucesso:  false,
			Mensagem: mensagem,
		}); writeErr != nil {
			// Falha ao escrever a resposta de erro (ex.: cliente desconectou):
			// registra de forma redigida, sem o erro cru potencialmente sensível.
			// O request_id é o gerado pelo servidor (cabeçalho da resposta).
			c.Logger().Error("falha ao escrever resposta de erro HTTP",
				"request_id", c.Response().Header().Get(echo.HeaderXRequestID),
				"status", status,
			)
		}
	}
}
