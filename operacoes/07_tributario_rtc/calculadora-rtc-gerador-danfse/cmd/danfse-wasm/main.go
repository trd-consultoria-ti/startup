//go:build js && wasm

package main

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strings"
	"syscall/js"

	pdfapp "danfse-service/internal/app"
	"danfse-service/internal/domain/enums"
	"danfse-service/internal/domain/localidade"
	"danfse-service/internal/renderer"
	"danfse-service/internal/service"
)

// danfseRequest representa o conteúdo JSON recebido do JavaScript.
type danfseRequest struct {
	XML     string `json:"xml"`
	STC     int    `json:"stc"`
	Canhoto *bool  `json:"canhoto"`
}

// danfseResponse é o objeto JSON retornado em caso de sucesso.
type danfseResponse struct {
	PDF  string `json:"pdf"`
	Size int    `json:"size"`
}

// app mantém as dependências reutilizáveis compartilhadas entre as chamadas WASM.
type app struct {
	renderizador service.RenderizadorPdfDireto
	apresentador service.ApresentadorDanfse
	err          error
}

func newApp() (*app, error) {
	renderizador, err := renderer.NewRenderer()
	if err != nil {
		return nil, fmt.Errorf("renderer: %w", err)
	}

	apresentador := service.NovoServicoApresentacaoDanfse()

	return &app{
		renderizador: renderizador,
		apresentador: apresentador,
	}, nil
}

func (a *app) buildService() *service.ServicoGeracaoPdfDanfseDireto {
	return pdfapp.NewPdfServiceWithDependencies(localidade.BuscarMunicipioGerado, a.renderizador, a.apresentador)
}

func main() {
	app, err := newApp()
	if err != nil {
		js.Global().Set("danfseWasm", js.ValueOf(map[string]any{
			"ready":    false,
			"error":    err.Error(),
			"gerarPdf": js.Undefined(),
		}))
		select {}
	}

	ready := true
	readyVal := js.ValueOf(ready)

	gerarPdfFn := js.FuncOf(func(this js.Value, args []js.Value) any {
		if len(args) < 1 {
			return js.ValueOf(map[string]any{
				"error": "missing request JSON argument",
			})
		}

		var req danfseRequest
		if err := json.Unmarshal([]byte(args[0].String()), &req); err != nil {
			return js.ValueOf(map[string]any{
				"error": fmt.Sprintf("invalid request JSON: %v", err),
			})
		}

		xml := strings.TrimSpace(req.XML)
		if xml == "" {
			return js.ValueOf(map[string]any{
				"error": "xml is required",
			})
		}

		// Validação específica do WASM para o tamanho em bytes (o serviço também
		// valida esse limite como regra comum).
		if len(xml) > service.MaxXMLInputSize {
			return js.ValueOf(map[string]any{
				"error": fmt.Sprintf("xml exceeds maximum size of %d bytes", service.MaxXMLInputSize),
			})
		}

		situacao, ok := enums.SituacaoNotaPorSTC(req.STC)
		if !ok {
			return js.ValueOf(map[string]any{
				"error": fmt.Sprintf("invalid stc value: %d (expected 0, 1, or 2)", req.STC),
			})
		}

		canhoto := true
		if req.Canhoto != nil {
			canhoto = *req.Canhoto
		}

		pdfService := app.buildService()

		ctx := context.Background()
		pdfBytes, err := pdfService.Executar(ctx, []byte(xml), situacao, canhoto)
		if err != nil {
			return js.ValueOf(map[string]any{
				"error": fmt.Sprintf("PDF generation failed: %v", err),
			})
		}

		pdfBase64 := base64.StdEncoding.EncodeToString(pdfBytes)

		resp := danfseResponse{
			PDF:  pdfBase64,
			Size: len(pdfBytes),
		}

		respJSON, err := json.Marshal(resp)
		if err != nil {
			return js.ValueOf(map[string]any{
				"error": fmt.Sprintf("failed to marshal response: %v", err),
			})
		}

		return js.Global().Get("JSON").Call("parse", string(respJSON))
	})

	// Mantém a função viva durante toda a vida da página.
	// A js.Func é armazenada no objeto global para não ser coletada pelo GC.

	js.Global().Set("danfseWasm", js.ValueOf(map[string]any{
		"ready":    readyVal,
		"error":    "",
		"gerarPdf": gerarPdfFn,
	}))

	select {}
}
