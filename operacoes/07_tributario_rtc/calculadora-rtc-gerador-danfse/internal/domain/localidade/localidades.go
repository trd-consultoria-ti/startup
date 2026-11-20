package localidade

import (
	"encoding/json"
	"fmt"
	"sort"
)

// Estado representa uma unidade federativa (UF) no catálogo.
type Estado struct {
	Codigo int    `json:"codigo"`
	Nome   string `json:"nome"`
	Sigla  string `json:"sigla"`
}

// Municipio representa um município no catálogo.
type Municipio struct {
	Codigo   int    `json:"codigo"`
	Nome     string `json:"nome"`
	UFCodigo int    `json:"ufCodigo"`
}

// Localidades é a coleção em memória de estados e municípios.
// É imutável após sua construção.
type Localidades struct {
	estados        map[int]Estado
	municipios     map[int]Municipio
	municipiosByUF map[int][]Municipio
}

// buildLocalidades constrói uma Localidades a partir das fatias informadas,
// aplicando validações de coleções vazias, códigos, nomes e siglas vazios,
// códigos duplicados e referências de UF dos municípios.
func buildLocalidades(estados []Estado, municipios []Municipio) (*Localidades, error) {
	if len(estados) == 0 {
		return nil, fmt.Errorf("catalogo: catálogo vazio — nenhuma UF encontrada")
	}

	if len(municipios) == 0 {
		return nil, fmt.Errorf("catalogo: catálogo vazio — nenhum município encontrado")
	}

	c := &Localidades{
		estados:        make(map[int]Estado, len(estados)),
		municipios:     make(map[int]Municipio, len(municipios)),
		municipiosByUF: make(map[int][]Municipio),
	}

	for _, e := range estados {
		if e.Codigo == 0 {
			return nil, fmt.Errorf("catalogo: UF com código vazio")
		}
		if e.Nome == "" {
			return nil, fmt.Errorf("catalogo: UF %d com nome vazio", e.Codigo)
		}
		if e.Sigla == "" {
			return nil, fmt.Errorf("catalogo: UF %d com sigla vazia", e.Codigo)
		}
		if _, dup := c.estados[e.Codigo]; dup {
			return nil, fmt.Errorf("catalogo: UF duplicada código %d", e.Codigo)
		}
		c.estados[e.Codigo] = e
	}

	for _, m := range municipios {
		if m.Codigo == 0 {
			return nil, fmt.Errorf("catalogo: município com código vazio")
		}
		if m.Nome == "" {
			return nil, fmt.Errorf("catalogo: município %d com nome vazio", m.Codigo)
		}
		if m.UFCodigo == 0 {
			return nil, fmt.Errorf("catalogo: município %d com UF código vazio", m.Codigo)
		}
		if _, dup := c.municipios[m.Codigo]; dup {
			return nil, fmt.Errorf("catalogo: município duplicado código %d", m.Codigo)
		}
		if _, ok := c.estados[m.UFCodigo]; !ok {
			return nil, fmt.Errorf("catalogo: município %d referencia UF inexistente %d", m.Codigo, m.UFCodigo)
		}
		c.municipios[m.Codigo] = m
		c.municipiosByUF[m.UFCodigo] = append(c.municipiosByUF[m.UFCodigo], m)
	}

	return c, nil
}

// Carregar constrói o catálogo a partir dos dados gerados (estadosData e
// municipiosData), valida e retorna um catálogo pronto para consultas.
func Carregar() (*Localidades, error) {
	return buildLocalidades(estadosData, municipiosData)
}

// Carrega mantém compatibilidade com os testes e consumidores legados.
// Depreciado: use Carregar.
func Carrega() (*Localidades, error) {
	return Carregar()
}

// EstadoPorCodigo retorna a UF correspondente ao código ou false se não existir.
func (c *Localidades) EstadoPorCodigo(codigo int) (Estado, bool) {
	e, ok := c.estados[codigo]
	return e, ok
}

// MunicipioPorCodigo retorna o município correspondente ao código ou false se não existir.
func (c *Localidades) MunicipioPorCodigo(codigo int) (Municipio, bool) {
	m, ok := c.municipios[codigo]
	return m, ok
}

// MunicipiosPorUF retorna a lista de municípios da UF indicada.
func (c *Localidades) MunicipiosPorUF(ufCodigo int) []Municipio {
	return c.municipiosByUF[ufCodigo]
}

// TodosEstados retorna uma cópia da fatia de todas as UFs.
func (c *Localidades) TodosEstados() []Estado {
	out := make([]Estado, 0, len(c.estados))
	for _, e := range c.estados {
		out = append(out, e)
	}
	return out
}

// ContagemEstados retorna o número de UFs no catálogo.
func (c *Localidades) ContagemEstados() int {
	return len(c.estados)
}

// ContagemMunicipios retorna o número de municípios no catálogo.
func (c *Localidades) ContagemMunicipios() int {
	return len(c.municipios)
}

// EstadoExiste retorna verdadeiro se a UF existe no catálogo.
func (c *Localidades) EstadoExiste(codigo int) bool {
	_, ok := c.estados[codigo]
	return ok
}

// MunicipioExiste retorna verdadeiro se o município existe no catálogo.
func (c *Localidades) MunicipioExiste(codigo int) bool {
	_, ok := c.municipios[codigo]
	return ok
}

// EstadoPorSigla retorna a UF correspondente à sigla ou false se não existir.
func (c *Localidades) EstadoPorSigla(sigla string) (Estado, bool) {
	for _, e := range c.estados {
		if e.Sigla == sigla {
			return e, true
		}
	}
	return Estado{}, false
}

// EstadoPorNome retorna a UF correspondente ao nome ou false se não existir.
func (c *Localidades) EstadoPorNome(nome string) (Estado, bool) {
	for _, e := range c.estados {
		if e.Nome == nome {
			return e, true
		}
	}
	return Estado{}, false
}

// MunicipioPorNomeEUF retorna o município com o nome exato na UF indicada.
func (c *Localidades) MunicipioPorNomeEUF(nome string, ufCodigo int) (Municipio, bool) {
	for _, m := range c.municipiosByUF[ufCodigo] {
		if m.Nome == nome {
			return m, true
		}
	}
	return Municipio{}, false
}

// EstadoPorMunicipio retorna a UF de um município pelo código do município.
func (c *Localidades) EstadoPorMunicipio(municipioCodigo int) (Estado, bool) {
	m, ok := c.municipios[municipioCodigo]
	if !ok {
		return Estado{}, false
	}
	e, ok := c.estados[m.UFCodigo]
	return e, ok
}

// EstadoPorMunicipioSigla retorna a sigla da UF de um município.
func (c *Localidades) EstadoPorMunicipioSigla(municipioCodigo int) (string, bool) {
	m, ok := c.municipios[municipioCodigo]
	if !ok {
		return "", false
	}
	e, ok := c.estados[m.UFCodigo]
	if !ok {
		return "", false
	}
	return e.Sigla, true
}

// EstadoPorMunicipioNome retorna o nome da UF de um município.
func (c *Localidades) EstadoPorMunicipioNome(municipioCodigo int) (string, bool) {
	m, ok := c.municipios[municipioCodigo]
	if !ok {
		return "", false
	}
	e, ok := c.estados[m.UFCodigo]
	if !ok {
		return "", false
	}
	return e.Nome, true
}

// BuscarMunicipio retorna o nome e a sigla da UF de um município pelo código IBGE.
// Retorna encontrado=false com strings vazias quando o código não existe no catálogo.
// Retorna erro quando o município referencia uma UF inexistente.
func (c *Localidades) BuscarMunicipio(codigo int) (nome, uf string, encontrado bool, err error) {
	m, ok := c.municipios[codigo]
	if !ok {
		return "", "", false, nil
	}
	e, ok := c.estados[m.UFCodigo]
	if !ok {
		return "", "", false, fmt.Errorf("município %d (%s) referencia UF %d inexistente no catálogo", m.Codigo, m.Nome, m.UFCodigo)
	}
	return m.Nome, e.Sigla, true, nil
}

// BuscarMunicipioGerado realiza uma busca binária direta nas fatias geradas
// municipiosData e estadosData, que são ordenadas por código.
//
// A assinatura é compatível com Localidades.BuscarMunicipio, permitindo seu
// uso direto como service.ResolverMunicipio.
func BuscarMunicipioGerado(codigo int) (nome, uf string, encontrado bool, err error) {
	idx := sort.Search(len(municipiosData), func(i int) bool {
		return municipiosData[i].Codigo >= codigo
	})

	if idx >= len(municipiosData) || municipiosData[idx].Codigo != codigo {
		return "", "", false, nil
	}

	m := municipiosData[idx]

	// Busca a UF em estadosData (busca binária; são apenas 27 entradas).
	eIdx := sort.Search(len(estadosData), func(i int) bool {
		return estadosData[i].Codigo >= m.UFCodigo
	})

	if eIdx >= len(estadosData) || estadosData[eIdx].Codigo != m.UFCodigo {
		return "", "", false, fmt.Errorf("município %d (%s) referencia UF %d inexistente no catálogo", m.Codigo, m.Nome, m.UFCodigo)
	}

	return m.Nome, estadosData[eIdx].Sigla, true, nil
}

// DecodificarJSON decodifica o JSON do catálogo a partir de bytes,
// aplicando as mesmas validações de Carregar. Útil para testes
// que precisam injetar JSON alternativo sem alterar os dados gerados.
func DecodificarJSON(data []byte) (*Localidades, error) {
	var payload struct {
		Estados    []Estado    `json:"estados"`
		Municipios []Municipio `json:"municipios"`
	}

	if err := json.Unmarshal(data, &payload); err != nil {
		return nil, fmt.Errorf("catalogo: JSON inválido: %w", err)
	}

	return buildLocalidades(payload.Estados, payload.Municipios)
}

// DecodificaJSON mantém compatibilidade com consumidores legados.
// Depreciado: use DecodificarJSON.
func DecodificaJSON(data []byte) (*Localidades, error) {
	return DecodificarJSON(data)
}
