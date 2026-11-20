package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
)

// Estruturas de entrada correspondentes ao catálogo JSON.
type estado struct {
	Codigo int    `json:"codigo"`
	Nome   string `json:"nome"`
	Sigla  string `json:"sigla"`
}

type municipio struct {
	Codigo   int    `json:"codigo"`
	Nome     string `json:"nome"`
	UFCodigo int    `json:"ufCodigo"`
}

type payload struct {
	Estados    []estado    `json:"estados"`
	Municipios []municipio `json:"municipios"`
}

func main() {
	inputPath := flag.String("input", "", "caminho para localidades.json")
	outputPath := flag.String("output", "", "caminho para o arquivo Go gerado")
	flag.Parse()

	if *inputPath == "" || *outputPath == "" {
		fmt.Fprintln(os.Stderr, "uso: gen-localidades -input <json> -output <go>")
		os.Exit(1)
	}

	data, err := os.ReadFile(*inputPath)
	if err != nil {
		fmt.Fprintf(os.Stderr, "leitura da entrada: %v\n", err)
		os.Exit(1)
	}

	var p payload
	if err := json.Unmarshal(data, &p); err != nil {
		fmt.Fprintf(os.Stderr, "análise do JSON: %v\n", err)
		os.Exit(1)
	}

	// --- Validar ---
	if len(p.Estados) == 0 {
		fmt.Fprintln(os.Stderr, "validação: estados vazio")
		os.Exit(1)
	}
	if len(p.Municipios) == 0 {
		fmt.Fprintln(os.Stderr, "validação: municípios vazio")
		os.Exit(1)
	}

	// Cria um índice dos estados para validar as referências de UF.
	stateCodes := make(map[int]struct{}, len(p.Estados))
	for _, e := range p.Estados {
		if e.Codigo == 0 {
			fmt.Fprintln(os.Stderr, "validação: estado com código zero")
			os.Exit(1)
		}
		if e.Nome == "" {
			fmt.Fprintf(os.Stderr, "validação: estado %d com nome vazio\n", e.Codigo)
			os.Exit(1)
		}
		if e.Sigla == "" {
			fmt.Fprintf(os.Stderr, "validação: estado %d com sigla vazia\n", e.Codigo)
			os.Exit(1)
		}
		if _, dup := stateCodes[e.Codigo]; dup {
			fmt.Fprintf(os.Stderr, "validação: código de estado duplicado %d\n", e.Codigo)
			os.Exit(1)
		}
		stateCodes[e.Codigo] = struct{}{}
	}

	for _, m := range p.Municipios {
		if m.Codigo == 0 {
			fmt.Fprintln(os.Stderr, "validação: município com código zero")
			os.Exit(1)
		}
		if m.Nome == "" {
			fmt.Fprintf(os.Stderr, "validação: município %d com nome vazio\n", m.Codigo)
			os.Exit(1)
		}
		if m.UFCodigo == 0 {
			fmt.Fprintf(os.Stderr, "validação: município %d com ufCodigo zero\n", m.Codigo)
			os.Exit(1)
		}
		if _, dup := stateCodes[m.UFCodigo]; !dup {
			fmt.Fprintf(os.Stderr, "validação: município %d referencia UF inexistente %d\n", m.Codigo, m.UFCodigo)
			os.Exit(1)
		}
	}

	// --- Ordenar por código ---
	sort.Slice(p.Estados, func(i, j int) bool { return p.Estados[i].Codigo < p.Estados[j].Codigo })
	sort.Slice(p.Municipios, func(i, j int) bool { return p.Municipios[i].Codigo < p.Municipios[j].Codigo })

	// --- Gerar Go ---
	var sb strings.Builder
	sb.WriteString("// Código gerado por cmd/gen-localidades; NÃO EDITAR.\n\n")
	sb.WriteString("package localidade\n\n")

	// Emite os valores dos estados (não exportados).
	sb.WriteString("// estadosData é a lista ordenada de estados (UFs).\n")
	sb.WriteString("var estadosData = []Estado{\n")
	for _, e := range p.Estados {
		sb.WriteString(fmt.Sprintf("\t{Codigo: %d, Nome: %q, Sigla: %q},\n", e.Codigo, e.Nome, e.Sigla))
	}
	sb.WriteString("}\n\n")

	// Emite os valores dos municípios (não exportados).
	sb.WriteString("// municipiosData é a lista ordenada de municípios.\n")
	sb.WriteString("var municipiosData = []Municipio{\n")
	for _, m := range p.Municipios {
		sb.WriteString(fmt.Sprintf("\t{Codigo: %d, Nome: %q, UFCodigo: %d},\n", m.Codigo, m.Nome, m.UFCodigo))
	}
	sb.WriteString("}\n")

	// Escreve em arquivo temporário e renomeia para garantir atomicidade.
	dir := filepath.Dir(*outputPath)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		fmt.Fprintf(os.Stderr, "criação do diretório de saída: %v\n", err)
		os.Exit(1)
	}

	tmpFile, err := os.CreateTemp(dir, "gen-localidades-*.go.tmp")
	if err != nil {
		fmt.Fprintf(os.Stderr, "criação do arquivo temporário: %v\n", err)
		os.Exit(1)
	}
	tmpPath := tmpFile.Name()

	if _, err := tmpFile.WriteString(sb.String()); err != nil {
		tmpFile.Close()
		os.Remove(tmpPath)
		fmt.Fprintf(os.Stderr, "escrita do arquivo temporário: %v\n", err)
		os.Exit(1)
	}
	if err := tmpFile.Close(); err != nil {
		os.Remove(tmpPath)
		fmt.Fprintf(os.Stderr, "fechamento do arquivo temporário: %v\n", err)
		os.Exit(1)
	}

	if err := os.Rename(tmpPath, *outputPath); err != nil {
		os.Remove(tmpPath)
		fmt.Fprintf(os.Stderr, "renomeação do arquivo temporário para a saída: %v\n", err)
		os.Exit(1)
	}

	fmt.Printf("gerado %s (%d estados, %d municípios)\n", *outputPath, len(p.Estados), len(p.Municipios))
}
