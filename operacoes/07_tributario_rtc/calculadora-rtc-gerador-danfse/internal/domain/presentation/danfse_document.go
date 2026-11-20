package presentation

// DocumentoDanfse é um contrato puro de apresentação para o PDF do DANFSe.
// Não contém referências a models.NFSe ou tipos do Folio.

// Campo representa um único par rótulo-valor renderizado no documento.
type Campo struct {
	Rotulo    string
	Valor     string
	Sombreado bool
}

// SecaoParte representa um bloco de pessoa (Prestador, Tomador, Destinatário, Intermediário).
type SecaoParte struct {
	Titulo             string
	CNPJCPFNIF         string
	InscricaoMunicipal string
	Telefone           string
	Nome               string
	MunicipioUF        string
	CodigoIBGECEP      string
	Endereco           string
	Email              string
	Visivel            bool
	MensagemSupressao  string
	SimplesNacional    string
	RegimeApuracaoSN   string
}

// Secao representa uma faixa horizontal com título (Serviço, Tributação, etc.).
type Secao struct {
	Titulo   string
	AlturaCM float64
	Campos   []Campo
	Visivel  bool
}

// DadosCabecalho contém as informações da faixa superior.
type DadosCabecalho struct {
	MarcaDagua            string
	ExibicaoDescServ      string
	ExibicaoCompl         string
	ExibicaoLinhaTributos string
	Municipio             string
	UF                    string
	AmbienteGerador       string
	TipoAmbiente          string
}

// DadosAcesso contém a chave de acesso e a linha de numeração.
type DadosAcesso struct {
	ChaveAcesso         string
	CodigoQR            string
	NumeroNFSe          string
	Competencia         string
	DataHoraEmissaoNFSe string
	NumeroDPS           string
	SerieDPS            string
	DataHoraEmissaoDPS  string
	TipoEmitente        string
	Situacao            string
	Finalidade          string
}

// DadosPartes contém as quatro seções de partes.
type DadosPartes struct {
	Emitente      SecaoParte
	Tomador       SecaoParte
	Destinatario  SecaoParte
	Intermediario SecaoParte
}

// DadosServico contém o bloco de descrição do serviço.
type DadosServico struct {
	CodigoTribNacMun string
	CodigoNBS        string
	LocalPrestacaoUF string
	XTribNac         string
	DescServ         string
	DescServOriginal string
}

// DadosTributacaoMunicipal contém o bloco do ISSQN.
type DadosTributacaoMunicipal struct {
	TribISSQN            string
	MunicipioUFPAISIncid string
	RegEspTrib           string
	TpImunidade          string
	SuspensaoExig        string
	NProcessoSusp        string
	BeneficioMunicipal   string
	CalculoBM            string
	TotalDeducoes        string
	DescontoIncond       string
	BCISSQN              string
	AliqAplic            string
	RetISSQN             string
	ISSQNApurado         string
	Visivel              bool
	MensagemSupressao    string
}

// DadosTributacaoFederal contém o bloco dos impostos federais.
type DadosTributacaoFederal struct {
	IRRF             string
	ContribPrevidRet string
	ContribSocRet    string
	PISDebito        string
	COFINSDebito     string
	DescContribRet   string
	ExibirPisCofins  bool
	Visivel          bool
}

// DadosTributacaoIBSCBS contém o bloco do IBS/CBS.
type DadosTributacaoIBSCBS struct {
	CSTcClassTrib            string
	IndOpCodIBGELocalidadeUF string
	ExclusoesReducoesBC      string
	BCPosExclusoes           string
	RedAliqIBSRedAliqCBS     string
	AliqIBSUFIBSMun          string
	AliqEfetMunIBS           string
	ValorMunIBS              string
	AliqEfetEstIBS           string
	ValorEstIBS              string
	ValorTotalIBS            string
	AliqCBS                  string
	AliqEfetCBS              string
	ValorTotalCBS            string
	Visivel                  bool
}

// DadosTotais contém o bloco dos totais.
type DadosTotais struct {
	ValorOperacaoServico string
	DescontoIncond       string
	DescontoCond         string
	TotalRetencoes       string
	ValorLiquido         string
	TotalIBSCBS          string
	ValorLiquidoIBSCBS   string
}

// DadosInformacoesComplementares contém o bloco de informações complementares.
type DadosInformacoesComplementares struct {
	Conteudo         string
	ConteudoOriginal string
	LinhaTributos    string
	Visivel          bool
}

// DadosCanhoto contém o canhoto (recibo).
type DadosCanhoto struct {
	Visivel bool
}

// MarcaDagua contém a configuração da marca d'água.
type MarcaDagua struct {
	Texto        string
	TamanhoFonte float64
	ColorR       float64
	ColorG       float64
	ColorB       float64
	Angulo       float64
	Opacidade    float64
}

// DocumentoDanfse é o contrato de apresentação de nível superior.
type DocumentoDanfse struct {
	MarcaDagua  MarcaDagua
	Cabecalho   DadosCabecalho
	Acesso      DadosAcesso
	Partes      DadosPartes
	Servico     DadosServico
	Tributacoes struct {
		Municipal DadosTributacaoMunicipal
		Federal   DadosTributacaoFederal
		IBSCBS    DadosTributacaoIBSCBS
	}
	Totais                    DadosTotais
	InformacoesComplementares DadosInformacoesComplementares
	Canhoto                   DadosCanhoto
}
