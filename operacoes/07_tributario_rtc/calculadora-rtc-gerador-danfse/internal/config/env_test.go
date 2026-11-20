package config

import (
	"testing"

	"danfse-service/internal/service"
)

func TestEnvCarregarUsaDefaults(t *testing.T) {
	t.Setenv("SERVER_PORT", "")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.Server.Port != 8080 {
		t.Fatalf("defaults inesperados: %+v", got)
	}
}

func TestEnvCarregarLeVariaveis(t *testing.T) {
	t.Setenv("SERVER_PORT", "9090")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.Server.Port != 9090 {
		t.Fatalf("configuração inesperada: %+v", got)
	}
}

func TestEnvCarregarRejeitaPortaInvalida(t *testing.T) {
	t.Setenv("SERVER_PORT", "abc")

	var got Env
	if err := got.Carregar(); err == nil {
		t.Fatal("esperava erro para SERVER_PORT inválido")
	}
}

func TestEnvCarregarXMLMaxSizeDefault(t *testing.T) {
	t.Setenv("XML_MAX_SIZE_BYTES", "")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.XMLMaxSizeBytes != service.MaxXMLInputSize {
		t.Fatalf("XMLMaxSizeBytes default inesperado: %d, esperava %d (5 MiB)", got.XMLMaxSizeBytes, service.MaxXMLInputSize)
	}
}

func TestEnvCarregarLeXMLMaxSize(t *testing.T) {
	t.Setenv("XML_MAX_SIZE_BYTES", "1048576")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.XMLMaxSizeBytes != 1048576 {
		t.Fatalf("XMLMaxSizeBytes inesperado: %d, esperava 1048576", got.XMLMaxSizeBytes)
	}
}

func TestEnvCarregarRejeitaXMLMaxSizeMalformada(t *testing.T) {
	t.Setenv("XML_MAX_SIZE_BYTES", "abc")

	var got Env
	if err := got.Carregar(); err == nil {
		t.Fatal("esperava erro para XML_MAX_SIZE_BYTES malformado")
	}
}

func TestEnvCarregarRejeitaXMLMaxSizeZero(t *testing.T) {
	t.Setenv("XML_MAX_SIZE_BYTES", "0")

	var got Env
	if err := got.Carregar(); err == nil {
		t.Fatal("esperava erro para XML_MAX_SIZE_BYTES=0")
	}
}

func TestEnvCarregarRejeitaXMLMaxSizeNegativo(t *testing.T) {
	t.Setenv("XML_MAX_SIZE_BYTES", "-1")

	var got Env
	if err := got.Carregar(); err == nil {
		t.Fatal("esperava erro para XML_MAX_SIZE_BYTES negativo")
	}
}

func TestEnvCarregarRejeitaXMLMaxSizeExcedente(t *testing.T) {
	t.Setenv("XML_MAX_SIZE_BYTES", "999999999999")

	var got Env
	if err := got.Carregar(); err == nil {
		t.Fatal("esperava erro para XML_MAX_SIZE_BYTES acima do teto aceito")
	}
}

func TestEnvCarregarLogLevelDefault(t *testing.T) {
	t.Setenv("LOG_LEVEL", "")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.LogLevel != "info" {
		t.Fatalf("LogLevel default inesperado: %q, esperava \"info\"", got.LogLevel)
	}
}

func TestEnvCarregarLeLogLevel(t *testing.T) {
	t.Setenv("LOG_LEVEL", "debug")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.LogLevel != "debug" {
		t.Fatalf("LogLevel inesperado: %q, esperava \"debug\"", got.LogLevel)
	}
}

func TestEnvCarregarLogLevelMaiusculoNormaliza(t *testing.T) {
	t.Setenv("LOG_LEVEL", "ERROR")

	var got Env
	if err := got.Carregar(); err != nil {
		t.Fatal(err)
	}
	if got.LogLevel != "error" {
		t.Fatalf("LogLevel inesperado: %q, esperava \"error\"", got.LogLevel)
	}
}

func TestEnvCarregarRejeitaLogLevelInvalido(t *testing.T) {
	t.Setenv("LOG_LEVEL", "verbose")

	var got Env
	if err := got.Carregar(); err == nil {
		t.Fatal("esperava erro para LOG_LEVEL inválido")
	}
}
