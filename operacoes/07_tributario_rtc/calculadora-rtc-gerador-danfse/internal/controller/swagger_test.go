package controller

import (
	"encoding/json"
	"os"
	"testing"
)

// TestSwagger_BasePathERotas verifica o contrato OpenAPI gerado em
// api/swagger/swagger.json: basePath "/" com paths absolutos (/api/danfse,
// /health/live, /health/ready) e endpoints de health documentados com a tag
// Health e os status corretos. Requer `task swagger` para regenerar.
func TestSwagger_BasePathERotas(t *testing.T) {
	doc := loadSwaggerDoc(t)

	if got := doc["basePath"]; got != "/" {
		t.Fatalf("basePath = %q, esperava %q", got, "/")
	}

	paths, ok := doc["paths"].(map[string]any)
	if !ok {
		t.Fatalf("paths ausente/inválido no swagger.json")
	}
	for _, p := range []string{"/api/danfse", "/health/live", "/health/ready"} {
		if _, ok := paths[p]; !ok {
			t.Errorf("path %q ausente no swagger.json", p)
		}
	}

	live := getOperation(t, paths, "/health/live", "get")
	if !hasTag(live, "Health") {
		t.Errorf("/health/live deve ter a tag Health")
	}
	respLive, _ := live["responses"].(map[string]any)
	if _, ok := respLive["200"]; !ok {
		t.Errorf("/health/live deve documentar 200")
	}

	ready := getOperation(t, paths, "/health/ready", "get")
	if !hasTag(ready, "Health") {
		t.Errorf("/health/ready deve ter a tag Health")
	}
	respReady, _ := ready["responses"].(map[string]any)
	if _, ok := respReady["200"]; !ok {
		t.Errorf("/health/ready deve documentar 200")
	}
	if _, ok := respReady["503"]; !ok {
		t.Errorf("/health/ready deve documentar 503")
	}

	// O corpo documentado aponta para o DTO HealthStatus (consistente com o
	// corpo real {"status":"ok"}/{"status":"unavailable"}).
	for _, op := range []map[string]any{live, ready} {
		for _, r := range op["responses"].(map[string]any) {
			rm, _ := r.(map[string]any)
			schema, _ := rm["schema"].(map[string]any)
			if schema != nil && schema["$ref"] != "#/definitions/controller.HealthStatus" {
				t.Errorf("schema da resposta health deve referenciar controller.HealthStatus, obteve %v", schema)
			}
		}
	}
}

// TestSwagger_DefinicaoHealthStatus confirma que a definição do DTO carrega o
// campo status usado no corpo real dos probes de health.
func TestSwagger_DefinicaoHealthStatus(t *testing.T) {
	doc := loadSwaggerDoc(t)
	defs, ok := doc["definitions"].(map[string]any)
	if !ok {
		t.Fatalf("definitions ausente no swagger.json")
	}
	hs, ok := defs["controller.HealthStatus"].(map[string]any)
	if !ok {
		t.Fatalf("definição controller.HealthStatus ausente no swagger.json")
	}
	props, _ := hs["properties"].(map[string]any)
	if _, ok := props["status"]; !ok {
		t.Errorf("propriedade \"status\" ausente em controller.HealthStatus")
	}
}

// loadSwaggerDoc lê api/swagger/swagger.json gerado por `task swagger`.
func loadSwaggerDoc(t *testing.T) map[string]any {
	t.Helper()
	raw, err := os.ReadFile("../../api/swagger/swagger.json")
	if err != nil {
		t.Fatalf("ler swagger.json (rode `task swagger` para regenerar): %v", err)
	}
	var doc map[string]any
	if err := json.Unmarshal(raw, &doc); err != nil {
		t.Fatalf("parse swagger.json: %v", err)
	}
	return doc
}

// getOperation retorna a operação HTTP dentro de paths[path][method].
func getOperation(t *testing.T, paths map[string]any, path, method string) map[string]any {
	t.Helper()
	p, ok := paths[path].(map[string]any)
	if !ok {
		t.Fatalf("path %q não encontrado no swagger.json", path)
	}
	op, ok := p[method].(map[string]any)
	if !ok {
		t.Fatalf("método %s ausente no path %q", method, path)
	}
	return op
}

// hasTag verifica se a operação carrega a tag informada.
func hasTag(op map[string]any, tag string) bool {
	tags, _ := op["tags"].([]any)
	for _, t := range tags {
		if t == tag {
			return true
		}
	}
	return false
}
