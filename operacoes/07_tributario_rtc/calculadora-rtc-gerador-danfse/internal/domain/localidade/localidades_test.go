package localidade

import (
	"encoding/json"
	"testing"
)

func TestCarrega_Valido(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	if c == nil {
		t.Fatal("Carrega() retornou nil")
	}
}

func TestCarrega_ContagemEstados(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	n := c.ContagemEstados()
	if n != 28 {
		t.Errorf("ContagemEstados() = %d, esperado 28", n)
	}
}

func TestCarrega_ContagemMunicipios(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	n := c.ContagemMunicipios()
	if n != 5525 {
		t.Errorf("ContagemMunicipios() = %d, esperado 5525", n)
	}
}

func TestCarrega_PresencaSaoPaulo(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	m, ok := c.MunicipioPorCodigo(3550308)
	if !ok {
		t.Fatal("Municipio 3550308 (São Paulo) não encontrado")
	}
	if m.Nome != "São Paulo" {
		t.Errorf("Municipio 3550308 nome = %q, esperado %q", m.Nome, "São Paulo")
	}
	if m.UFCodigo != 35 {
		t.Errorf("Municipio 3550308 ufCodigo = %d, esperado 35", m.UFCodigo)
	}
}

func TestCarrega_PresencaBoaEsperanca(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	m, ok := c.MunicipioPorCodigo(5101837)
	if !ok {
		t.Fatal("Municipio 5101837 (Boa Esperança do Norte) não encontrado")
	}
	if m.Nome != "Boa Esperança do Norte" {
		t.Errorf("Municipio 5101837 nome = %q, esperado %q", m.Nome, "Boa Esperança do Norte")
	}
	if m.UFCodigo != 51 {
		t.Errorf("Municipio 5101837 ufCodigo = %d, esperado 51", m.UFCodigo)
	}
}

func TestCarrega_PresencaExterior(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	m, ok := c.MunicipioPorCodigo(9999999)
	if !ok {
		t.Fatal("Municipio 9999999 (EXTERIOR) não encontrado")
	}
	if m.Nome != "EXTERIOR" {
		t.Errorf("Municipio 9999999 nome = %q, esperado %q", m.Nome, "EXTERIOR")
	}
	if m.UFCodigo != 99 {
		t.Errorf("Municipio 9999999 ufCodigo = %d, esperado 99", m.UFCodigo)
	}
}

func TestCarrega_RelacionamentoValido(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	// SP -> UF 35
	m, ok := c.MunicipioPorCodigo(3550308)
	if !ok {
		t.Fatal("Municipio 3550308 não encontrado")
	}
	e, ok := c.EstadoPorCodigo(m.UFCodigo)
	if !ok {
		t.Fatalf("UF %d não encontrada", m.UFCodigo)
	}
	if e.Sigla != "SP" {
		t.Errorf("UF de 3550308 sigla = %q, esperado %q", e.Sigla, "SP")
	}
	// EXTERIOR -> UF 99
	m2, ok := c.MunicipioPorCodigo(9999999)
	if !ok {
		t.Fatal("Municipio 9999999 não encontrado")
	}
	e2, ok := c.EstadoPorCodigo(m2.UFCodigo)
	if !ok {
		t.Fatalf("UF %d não encontrada", m2.UFCodigo)
	}
	if e2.Sigla != "EX" {
		t.Errorf("UF de 9999999 sigla = %q, esperado %q", e2.Sigla, "EX")
	}
}

func TestCarrega_UFExistente(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	if !c.EstadoExiste(35) {
		t.Error("UF 35 (SP) deveria existir")
	}
	if !c.EstadoExiste(99) {
		t.Error("UF 99 (EX) deveria existir")
	}
	if c.EstadoExiste(999) {
		t.Error("UF 999 não deveria existir")
	}
}

func TestCarrega_MunicipioExistente(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	if !c.MunicipioExiste(3550308) {
		t.Error("Municipio 3550308 deveria existir")
	}
	if !c.MunicipioExiste(9999999) {
		t.Error("Municipio 9999999 deveria existir")
	}
	if c.MunicipioExiste(0) {
		t.Error("Municipio 0 não deveria existir")
	}
}

func TestCarrega_EstadoPorSigla(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	e, ok := c.EstadoPorSigla("SP")
	if !ok {
		t.Fatal("EstadoPorSigla('SP') não encontrou")
	}
	if e.Codigo != 35 {
		t.Errorf("EstadoPorSigla('SP') codigo = %d, esperado 35", e.Codigo)
	}
	if e.Nome != "SÃO PAULO" {
		t.Errorf("EstadoPorSigla('SP') nome = %q, esperado %q", e.Nome, "SÃO PAULO")
	}
	_, ok = c.EstadoPorSigla("XX")
	if ok {
		t.Error("EstadoPorSigla('XX') deveria retornar false")
	}
}

func TestCarrega_EstadoPorMunicipio(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	e, ok := c.EstadoPorMunicipio(3550308)
	if !ok {
		t.Fatal("EstadoPorMunicipio(3550308) não encontrou")
	}
	if e.Sigla != "SP" {
		t.Errorf("EstadoPorMunicipio(3550308) sigla = %q, esperado %q", e.Sigla, "SP")
	}
}

func TestCarrega_MunicipiosPorUF(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	munis := c.MunicipiosPorUF(35)
	if len(munis) == 0 {
		t.Fatal("MunicipiosPorUF(35) retornou lista vazia")
	}
	// Verifica que São Paulo está entre os municípios de SP
	found := false
	for _, m := range munis {
		if m.Codigo == 3550308 {
			found = true
			break
		}
	}
	if !found {
		t.Error("São Paulo (3550308) não encontrado na lista de municípios de SP")
	}
}

func TestDecodificaJSON_JSONInvalido(t *testing.T) {
	_, err := DecodificaJSON([]byte("not json"))
	if err == nil {
		t.Fatal("DecodificaJSON com JSON inválido deveria retornar erro")
	}
}

func TestDecodificaJSON_CatalogoVazio(t *testing.T) {
	data := []byte(`{"estados":[],"municipios":[]}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com catálogo vazio deveria retornar erro")
	}
}

func TestDecodificaJSON_UFDuplicada(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"A","sigla":"AA"},{"codigo":1,"nome":"B","sigla":"BB"}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":1}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com UF duplicada deveria retornar erro")
	}
}

func TestDecodificaJSON_MunicipioDuplicado(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"A","sigla":"AA"}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":1},{"codigo":1,"nome":"M2","ufCodigo":1}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com município duplicado deveria retornar erro")
	}
}

func TestDecodificaJSON_UFInexistente(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"A","sigla":"AA"}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":999}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com UF inexistente deveria retornar erro")
	}
}

func TestDecodificaJSON_CodigoVazio(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":0,"nome":"A","sigla":"AA"}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":1}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com UF código vazio deveria retornar erro")
	}
}

func TestDecodificaJSON_NomeVazio(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"","sigla":"AA"}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":1}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com UF nome vazio deveria retornar erro")
	}
}

func TestDecodificaJSON_SiglaVazia(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"A","sigla":""}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":1}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com UF sigla vazia deveria retornar erro")
	}
}

func TestDecodificaJSON_MunicipioNomeVazio(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"A","sigla":"AA"}],
		"municipios":[{"codigo":1,"nome":"","ufCodigo":1}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com município nome vazio deveria retornar erro")
	}
}

func TestDecodificaJSON_MunicipioUFCodigoVazio(t *testing.T) {
	data := []byte(`{
		"estados":[{"codigo":1,"nome":"A","sigla":"AA"}],
		"municipios":[{"codigo":1,"nome":"M","ufCodigo":0}]
	}`)
	_, err := DecodificaJSON(data)
	if err == nil {
		t.Fatal("DecodificaJSON com município ufCodigo vazio deveria retornar erro")
	}
}

func TestDecodificaJSON_Valido(t *testing.T) {
	data, err := json.Marshal(map[string]interface{}{
		"estados": []map[string]interface{}{
			{"codigo": 35, "nome": "SÃO PAULO", "sigla": "SP"},
		},
		"municipios": []map[string]interface{}{
			{"codigo": 3550308, "nome": "São Paulo", "ufCodigo": 35},
		},
	})
	if err != nil {
		t.Fatalf("falha ao montar JSON de teste: %v", err)
	}
	c, err := DecodificaJSON(data)
	if err != nil {
		t.Fatalf("DecodificaJSON com dados válidos erro: %v", err)
	}
	if c.ContagemEstados() != 1 {
		t.Errorf("ContagemEstados() = %d, esperado 1", c.ContagemEstados())
	}
	if c.ContagemMunicipios() != 1 {
		t.Errorf("ContagemMunicipios() = %d, esperado 1", c.ContagemMunicipios())
	}
}

func TestCarrega_TodosEstados(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	estados := c.TodosEstados()
	if len(estados) != 28 {
		t.Errorf("TodosEstados() retornou %d, esperado 28", len(estados))
	}
}

func TestCarrega_EstadoPorNome(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	e, ok := c.EstadoPorNome("SÃO PAULO")
	if !ok {
		t.Fatal("EstadoPorNome('SÃO PAULO') não encontrou")
	}
	if e.Sigla != "SP" {
		t.Errorf("EstadoPorNome('SÃO PAULO') sigla = %q, esperado %q", e.Sigla, "SP")
	}
}

func TestCarrega_MunicipioPorNomeEUF(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	m, ok := c.MunicipioPorNomeEUF("São Paulo", 35)
	if !ok {
		t.Fatal("MunicipioPorNomeEUF('São Paulo', 35) não encontrou")
	}
	if m.Codigo != 3550308 {
		t.Errorf("MunicipioPorNomeEUF codigo = %d, esperado 3550308", m.Codigo)
	}
}

func TestCarrega_EstadoPorMunicipioSigla(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	sigla, ok := c.EstadoPorMunicipioSigla(3550308)
	if !ok {
		t.Fatal("EstadoPorMunicipioSigla(3550308) não encontrou")
	}
	if sigla != "SP" {
		t.Errorf("EstadoPorMunicipioSigla(3550308) = %q, esperado %q", sigla, "SP")
	}
}

func TestCarrega_EstadoPorMunicipioNome(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	nome, ok := c.EstadoPorMunicipioNome(3550308)
	if !ok {
		t.Fatal("EstadoPorMunicipioNome(3550308) não encontrou")
	}
	if nome != "SÃO PAULO" {
		t.Errorf("EstadoPorMunicipioNome(3550308) = %q, esperado %q", nome, "SÃO PAULO")
	}
}

func TestCarrega_NaoMutavel(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	// Tentar modificar o catálogo via MunicipiosPorUF não deve afetar o interno
	munis := c.MunicipiosPorUF(35)
	originalLen := len(munis)
	munis = append(munis, Municipio{Codigo: 999999, Nome: "Fake", UFCodigo: 35})
	if len(c.MunicipiosPorUF(35)) != originalLen {
		t.Error("MunicipiosPorUF retornou fatia que modifica o catálogo interno")
	}
}

func TestBuscarMunicipio_Existente(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	nome, uf, encontrado, err := c.BuscarMunicipio(3550308)
	if err != nil {
		t.Fatalf("erro inesperado: %v", err)
	}
	if !encontrado {
		t.Fatal("esperava encontrado=true para 3550308")
	}
	if nome != "São Paulo" {
		t.Errorf("nome = %q, esperado %q", nome, "São Paulo")
	}
	if uf != "SP" {
		t.Errorf("uf = %q, esperado %q", uf, "SP")
	}
}

func TestBuscarMunicipio_Inexistente(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	nome, uf, encontrado, err := c.BuscarMunicipio(1)
	if err != nil {
		t.Fatalf("erro inesperado: %v", err)
	}
	if encontrado {
		t.Fatal("esperava encontrado=false para código inexistente")
	}
	if nome != "" || uf != "" {
		t.Errorf("esperava strings vazias, obteve nome=%q uf=%q", nome, uf)
	}
}

func TestBuscarMunicipio_Exterior(t *testing.T) {
	c, err := Carrega()
	if err != nil {
		t.Fatalf("Carrega() erro inesperado: %v", err)
	}
	nome, uf, encontrado, err := c.BuscarMunicipio(9999999)
	if err != nil {
		t.Fatalf("erro inesperado: %v", err)
	}
	if !encontrado {
		t.Fatal("esperava encontrado=true para 9999999")
	}
	if nome != "EXTERIOR" {
		t.Errorf("nome = %q, esperado %q", nome, "EXTERIOR")
	}
	if uf != "EX" {
		t.Errorf("uf = %q, esperado %q", uf, "EX")
	}
}

func TestBuscarMunicipio_UFInvalida(t *testing.T) {
	c := &Localidades{
		estados:    map[int]Estado{1: {Codigo: 1, Nome: "A", Sigla: "AA"}},
		municipios: map[int]Municipio{1: {Codigo: 1, Nome: "M", UFCodigo: 999}},
	}

	_, _, encontrado, err := c.BuscarMunicipio(1)
	if err == nil {
		t.Fatal("esperava erro para município que referencia UF inexistente")
	}
	if encontrado {
		t.Fatal("município com UF inválida não deveria ser considerado encontrado")
	}
}
