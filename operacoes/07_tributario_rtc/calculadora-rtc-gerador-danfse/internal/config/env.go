package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"

	"danfse-service/internal/service"
)

type server struct {
	Port int
}

// Níveis de log aceitos por LOG_LEVEL.
var logLevels = map[string]bool{
	"debug": true,
	"info":  true,
	"warn":  true,
	"error": true,
}

// Env reúne toda a configuração carregada do ambiente.
type Env struct {
	Server server
	// XMLMaxSizeBytes é o limite máximo (em bytes) aceito para o XML de entrada,
	// compartilhado entre o BodyLimit do router e o preparador.
	XMLMaxSizeBytes int
	// LogLevel é o nível do logger JSON estruturado (debug|info|warn|error).
	// Padrão: "info".
	LogLevel string
}

func (e *Env) Carregar() error {
	port, err := envInt("SERVER_PORT", 8080)
	if err != nil {
		return err
	}

	maxXMLSize, err := envInt("XML_MAX_SIZE_BYTES", service.MaxXMLInputSize)
	if err != nil {
		return err
	}
	if maxXMLSize <= 0 {
		return fmt.Errorf("XML_MAX_SIZE_BYTES inválido: deve ser positivo, obteve %d", maxXMLSize)
	}
	if maxXMLSize > service.MaxXMLInputSizeLimit {
		return fmt.Errorf("XML_MAX_SIZE_BYTES acima do teto aceito de %d bytes (32 MiB): %d", service.MaxXMLInputSizeLimit, maxXMLSize)
	}

	e.Server = server{
		Port: port,
	}
	e.XMLMaxSizeBytes = maxXMLSize

	logLevel := strings.ToLower(envString("LOG_LEVEL", "info"))
	if !logLevels[logLevel] {
		return fmt.Errorf("LOG_LEVEL inválido: deve ser debug|info|warn|error, obteve %q", logLevel)
	}
	e.LogLevel = logLevel

	return nil
}

func envString(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok && value != "" {
		return value
	}
	return fallback
}

func envInt(key string, fallback int) (int, error) {
	value := envString(key, strconv.Itoa(fallback))
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return 0, fmt.Errorf("%s inválido: deve ser um número inteiro: %w", key, err)
	}
	return parsed, nil
}

func NewEnv() *Env {
	return &Env{}
}
