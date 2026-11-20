package controller

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v5"
)

func TestTratarErrosHTTP_HTTPError(t *testing.T) {
	recorder := httptest.NewRecorder()
	c := echo.New().NewContext(httptest.NewRequest("GET", "/", nil), recorder)

	TratarErrosHTTP(c, echo.NewHTTPError(404, "recurso não encontrado"))

	assertErrorResponse(t, recorder, 404, "recurso não encontrado")
}

func TestTratarErrosHTTP_GenericError(t *testing.T) {
	recorder := httptest.NewRecorder()
	c := echo.New().NewContext(httptest.NewRequest("GET", "/", nil), recorder)

	TratarErrosHTTP(c, errors.New("falha interna"))

	// Contrato intencional: respostas 5xx são genéricas e NÃO revelam o
	// err.Error() (que poderia carregar XML/dados fiscais).
	assertErrorResponse(t, recorder, 500, http.StatusText(http.StatusInternalServerError))
}

func TestTratarErrosHTTP_HTTPError500Generico(t *testing.T) {
	recorder := httptest.NewRecorder()
	c := echo.New().NewContext(httptest.NewRequest("GET", "/", nil), recorder)

	// HTTPError 5xx com mensagem sensível: o corpo deve usar mensagem genérica.
	TratarErrosHTTP(c, echo.NewHTTPError(500, "segredo interno com XML"))

	assertErrorResponse(t, recorder, 500, http.StatusText(http.StatusInternalServerError))
}

func TestTratarErrosHTTP_CommittedResponse(t *testing.T) {
	recorder := httptest.NewRecorder()
	c := echo.New().NewContext(httptest.NewRequest("GET", "/", nil), recorder)
	if err := c.String(200, "já enviado"); err != nil {
		t.Fatal(err)
	}

	TratarErrosHTTP(c, errors.New("não deve substituir"))

	if recorder.Code != 200 || recorder.Body.String() != "já enviado" {
		t.Fatalf("resposta committed foi alterada: status=%d body=%q", recorder.Code, recorder.Body.String())
	}
}

func TestTratarErrosHTTP_JSONBody(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/api/danfse", strings.NewReader("<xml>ok</xml>"))
	rec := httptest.NewRecorder()
	c := e.NewContext(req, rec)

	err := echo.NewHTTPError(http.StatusBadRequest, "Xml não informado.")

	TratarErrosHTTP(c, err)

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("esperava status 400, obteve %d", rec.Code)
	}

	var resp ErroResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &resp); err != nil {
		t.Fatalf("corpo JSON inválido: %v; body: %s", err, rec.Body.String())
	}

	if resp.Sucesso != false {
		t.Fatalf("esperava sucesso=false, obteve %v", resp.Sucesso)
	}
	if resp.Mensagem != "Xml não informado." {
		t.Fatalf("esperava mensagem 'Xml não informado.', obteve %q", resp.Mensagem)
	}
	if resp.Conteudo != nil {
		t.Fatalf("esperava content nulo, obteve %v", resp.Conteudo)
	}

	bodyStr := rec.Body.String()
	if strings.Contains(bodyStr, `"content"`) {
		t.Fatalf("corpo não deveria conter a tag 'content' quando nil: %s", bodyStr)
	}
}

func assertErrorResponse(t *testing.T, recorder *httptest.ResponseRecorder, wantStatus int, wantMessage string) {
	t.Helper()
	if recorder.Code != wantStatus {
		t.Fatalf("status = %d, want %d", recorder.Code, wantStatus)
	}
	var got ErroResponse
	if err := json.NewDecoder(recorder.Body).Decode(&got); err != nil {
		t.Fatalf("decodificar resposta: %v", err)
	}
	if got.Sucesso || got.Mensagem != wantMessage {
		t.Fatalf("resposta = %+v, want sucesso=false mensagem=%q", got, wantMessage)
	}
}
