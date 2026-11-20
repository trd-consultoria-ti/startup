package renderer

// Labels fixos usados exclusivamente no renderer.go para títulos, rótulos de
// campos, canhoto e textos QR. Não são mensagens vindas de doc/presenter.

// --- Cabeçalho ---

const (
	labelDANFSeVersao          = "DANFSe v2.0"
	labelDocumentoAuxiliar     = "Documento Auxiliar da NFS-e"
	labelNFSemValidadeJuridica = "NFS-e SEM VALIDADE JURÍDICA"
)

// --- Info NFS-e (células superiores) ---

const (
	labelChaveAcessoNfse     = "CHAVE DE ACESSO DA NFS-E"
	labelNumeroNFSe          = "NÚMERO DA NFS-E"
	labelCompetenciaNFSe     = "COMPETÊNCIA DA NFS-E"
	labelDataHoraEmissaoNFSe = "DATA E HORA DA EMISSÃO DA NFS-E"
	labelNumeroDPS           = "NÚMERO DA DPS"
	labelSerieDPS            = "SÉRIE DA DPS"
	labelDataHoraEmissaoDPS  = "DATA E HORA DA EMISSÃO DA DPS"
)

// --- QR Code ---

const (
	labelQrLine1 = "A autenticidade desta NFS-e pode ser verificada"
	labelQrLine2 = "pela leitura deste código QR ou pela consulta da"
	labelQrLine3 = "chave de acesso no portal nacional da NFS-e"
)

// --- Partes (identificação) ---

const (
	labelEmitenteNfse = "EMITENTE DA NFS-e"
	labelSituacaoNfse = "SITUAÇÃO DA NFS-e"
	labelFinalidade   = "FINALIDADE"
)

// --- Seções de partes ---

const (
	labelPrestadorFornecedor   = "PRESTADOR / FORNECEDOR"
	labelTomadorAdquirente     = "TOMADOR / ADQUIRENTE"
	labelDestinatarioOperacao  = "DESTINATÁRIO DA OPERAÇÃO"
	labelIntermediarioOperacao = "INTERMEDIÁRIO DA OPERAÇÃO"
)

// --- Serviço prestado ---

const (
	labelServicoPrestado  = "SERVIÇO PRESTADO"
	labelCodigoTribNacMun = "Código de Tributação Nacional / Municipal"
	labelCodigoNBS        = "Código da NBS"
	labelLocalPrestacao   = "Local da Prestação / Sigla UF / País"
	labelDescricaoServico = "Descrição do Serviço"
)

// --- Tributação Municipal ---

const (
	labelTribMunicipal          = "TRIBUTAÇÃO MUNICIPAL (ISSQN)"
	labelTipoTribIssqn          = "Tipo de Tributação do ISSQN"
	labelMunicipioUFPAISSIssqn  = "Município / Sigla UF / País de Incidência do ISSQN"
	labelRegEspTribIssqn        = "Regime Especial de Tributação do ISSQN"
	labelTpImunidadeIssqn       = "Tipo de Imunidade do ISSQN"
	labelSuspensaoExigIssqn     = "Suspensão da Exigibilidade do ISSQN"
	labelNumeroProcessoSusp     = "Número Processo Suspensão"
	labelBeneficioMunicipal     = "Benefício Municipal"
	labelCalculoBM              = "Cálculo do BM"
	labelTotalDeducoes          = "Total Deduções/Reduções"
	labelDescontoIncondicionado = "Desconto Incondicionado"
	labelBCIssqn                = "BC ISSQN"
	labelAliqAplicada           = "Alíquota Aplicada"
	labelRetencaoIssqn          = "Retenção do ISSQN"
	labelIssqnApurado           = "ISSQN Apurado"
)

// --- Tributação Federal ---

const (
	labelTribFederal         = "TRIBUTAÇÃO FEDERAL (EXCETO CBS)"
	labelIRRF                = "IRRF"
	labelContribPrevidRet    = "Contribuição Previdenciária - Retida"
	labelContribSocRet       = "Contribuições Sociais - Retidas"
	labelPISDebito           = "PIS - Débito Apuração Própria"
	labelCOFINSDebito        = "COFINS - Débito Apuração Própria"
	labelDescricaoContribRet = "Descrição Contrib. Sociais - Retidas"
)

// --- Tributação IBS / CBS ---

const (
	labelTribIBSCBS               = "TRIBUTAÇÃO IBS / CBS"
	labelCSTcClassTrib            = "CST / cClassTrib"
	labelIndOpCodIBGELocalidadeUF = "Indicador de Operação / Código IBGE Incidência / Município Incidência / Sigla UF"
	labelExclusoesReducoesBC      = "Exclusões e Reduções da Base de Cálculo"
	labelBCPosExclusoes           = "Base de Cálculo Após Exclusões e Reduções"
	labelRedAliqIBSRedAliqCBS     = "Red. Alíquota IBS / Red. Alíquota CBS"
	labelAliqIBSUFIBSMun          = "Alíquota IBS UF / IBS Mun"
	labelAliqEfetMunIBS           = "Alíq. Efetiva Municipal - IBS"
	labelValorMunIBS              = "Valor Apurado Municipal - IBS"
	labelAliqEfetEstIBS           = "Alíq. Efetiva Estadual - IBS"
	labelValorEstIBS              = "Valor Apurado Estadual - IBS"
	labelValorTotalIBS            = "Valor Total Apurado - IBS"
	labelAliqCBS                  = "Alíquota - CBS"
	labelAliqEfetCBS              = "Alíquota Efetiva - CBS"
	labelValorTotalCBS            = "Valor Total Apurado - CBS"
)

// --- Totais ---

const (
	labelValorTotalNfse       = "VALOR TOTAL DA NFS-E"
	labelValorOperacaoServico = "VALOR DA OPERAÇÃO / SERVIÇO"
	labelDescontoCondicionado = "Desconto Condicionado"
	labelTotalRetencoes       = "Total das Retenções (ISSQN / Federais)"
	labelValorLiquidoNfse     = "VALOR LÍQUIDO DA NFS-e"
	labelTotalIBSCBS          = "Total do IBS/CBS"
	labelValorLiquidoIBSCBS   = "VALOR LÍQUIDO DA NFS-e + IBS/CBS"
)

// --- Informações Complementares ---

const (
	labelInformacoesComplementares = "INFORMAÇÕES COMPLEMENTARES"
)

// --- Canhoto ---

const (
	labelDataCientificacao       = "DATA CIENTIFICAÇÃO:"
	labelIdentificacaoAssinatura = "IDENTIFICAÇÃO E ASSINATURA"
	labelNumeroNFSeChave         = "Nº NFS-e / CHAVE NFS-e"
)

// --- Campos de parte (SecaoParte) ---

const (
	labelCNPJCPFNIF          = "CNPJ / CPF / NIF"
	labelIndicadorMunicipal  = "Indicador Municipal (Inscrição)"
	labelTelefone            = "Telefone"
	labelNomeNomeEmpresarial = "Nome / Nome Empresarial"
	labelMunicipioUF         = "Município / Sigla UF"
	labelCodigoIBGECEP       = "Código IBGE / CEP"
	labelEndereco            = "Endereço"
	labelEmail               = "E-mail"
	labelSimplesNacional     = "Simples Nacional na Data de Competência"
	labelRegimeApuracaoSN    = "Regime de Apuração Tributária pelo SN"
)
