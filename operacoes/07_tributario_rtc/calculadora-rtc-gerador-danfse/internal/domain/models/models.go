package models

import "encoding/xml"

type NFSe struct {
	Versao                string `xml:"versao,attr"`
	MarcaDagua            string
	ExibicaoDescServ      string
	ExibicaoCompl         string
	ExibicaoLinhaTributos string
	VExclusoesReducoesBC  float64
	TextoDescServOriginal string
	TextoComplOriginal    string
	XMLName               xml.Name `xml:"NFSe"`
	InfNFSe               InfNFSe  `xml:"infNFSe"`
}

type InfNFSe struct {
	Id             string   `xml:"Id,attr"`
	Situacao       string   `xml:"situacao,attr"`
	Qrcode         string   `xml:"qrcode,attr"`
	DhProc         string   `xml:"dhProc"`
	NNFSe          string   `xml:"nNFSe"`
	AmbGer         string   `xml:"ambGer"`
	CStat          string   `xml:"cStat"`
	XLocEmi        string   `xml:"xLocEmi"`
	CLocIncid      string   `xml:"cLocIncid"`
	XLocIncid      string   `xml:"xLocIncid"`
	XUFIncid       string   `xml:"xUFIncid"`
	XPaisIncid     string   `xml:"xPaisIncid"`
	XTribNac       string   `xml:"xTribNac"`
	XTribMun       string   `xml:"xTribMun"`
	XLocPrestacao  string   `xml:"xLocPrestacao"`
	CPaisPrestacao string   `xml:"cPaisPrestacao"`
	DPS            DPS      `xml:"DPS"`
	Emit           Emitente `xml:"emit"`
	Valores        Valores  `xml:"valores"`
	IBSCBS         *IBSCBS  `xml:"IBSCBS"`
}

type DPS struct {
	InfDPS InfDPS `xml:"infDPS"`
}

type InfDPS struct {
	Id      string     `xml:"Id,attr"`
	NDPS    string     `xml:"nDPS"`
	Serie   string     `xml:"serie"`
	DhEmi   string     `xml:"dhEmi"`
	DCompet string     `xml:"dCompet"`
	CLocEmi string     `xml:"cLocEmi"`
	XUFEmi  string     `xml:"xUFEmi"`
	TpEmit  int        `xml:"tpEmit"`
	TpAmb   int        `xml:"tpAmb"`
	Prest   *Prestador `xml:"prest"`
	Toma    *Tomador   `xml:"toma"`
	Interm  *Interm    `xml:"interm"`
	Serv    Servico    `xml:"serv"`
	Valores ValoresDPS `xml:"valores"`
	Subst   Subst      `xml:"subst"`
	IBSCBS  *IBSCBSDPS `xml:"IBSCBS"`
}

// --- EMITENTE ---
type Emitente struct {
	CNPJ     string   `xml:"CNPJ"`
	CPF      string   `xml:"CPF"`
	NIF      string   `xml:"NIF"`
	CNaoNIF  int      `xml:"cNaoNIF"`
	IM       string   `xml:"IM"`
	XNome    string   `xml:"xNome"`
	XFant    string   `xml:"xFant"`
	Fone     string   `xml:"fone"`
	Email    string   `xml:"email"`
	EnderNac Endereco `xml:"enderNac"`
	EnderExt *EndExt  `xml:"enderExt"`
}

// --- TOMADOR ---
type Tomador struct {
	CNPJ    string           `xml:"CNPJ"`
	CPF     string           `xml:"CPF"`
	NIF     string           `xml:"NIF"`
	CNaoNIF int              `xml:"cNaoNIF"`
	IM      string           `xml:"IM"`
	XNome   string           `xml:"xNome"`
	Fone    string           `xml:"fone"`
	Email   string           `xml:"email"`
	End     *EnderecoTomador `xml:"end"`
}

type EnderecoTomador struct {
	XLgr    string  `xml:"xLgr"`
	Nro     string  `xml:"nro"`
	XCpl    string  `xml:"xCpl"`
	XBairro string  `xml:"xBairro"`
	EndNac  *EndNac `xml:"endNac"`
	EndExt  *EndExt `xml:"endExt"`
}

type EndNac struct {
	XLgr    string `xml:"xLgr"`
	Nro     string `xml:"nro"`
	XCpl    string `xml:"xCpl"`
	XBairro string `xml:"xBairro"`
	CMun    string `xml:"cMun"`
	XMun    string `xml:"xMun"`
	UF      string `xml:"UF"`
	CEP     string `xml:"CEP"`
}

// --- INTERMEDIARIO ---
type Interm struct {
	CNPJ    string      `xml:"CNPJ"`
	CPF     string      `xml:"CPF"`
	NIF     string      `xml:"NIF"`
	CNaoNIF int         `xml:"cNaoNIF"`
	IM      string      `xml:"IM"`
	XNome   string      `xml:"xNome"`
	Fone    string      `xml:"fone"`
	Email   string      `xml:"email"`
	End     EnderecoDPS `xml:"end"`
}

// --- DESTINATÁRIO ---
type Destinatario struct {
	CNPJ  string           `xml:"CNPJ"`
	CPF   string           `xml:"CPF"`
	NIF   string           `xml:"NIF"`
	XNome string           `xml:"xNome"`
	Fone  string           `xml:"fone"`
	Email string           `xml:"email"`
	End   *EnderecoTomador `xml:"end"`
}

// --- ENDEREÇOS ---
type Endereco struct {
	XLgr    string `xml:"xLgr"`
	Nro     string `xml:"nro"`
	XCpl    string `xml:"xCpl"`
	XBairro string `xml:"xBairro"`
	XMun    string `xml:"xMun"`
	CMun    string `xml:"cMun"`
	UF      string `xml:"UF"`
	CEP     string `xml:"CEP"`
}

type EnderecoDPS struct {
	XLgr    string    `xml:"xLgr"`
	Nro     string    `xml:"nro"`
	XCpl    string    `xml:"xCpl"`
	XBairro string    `xml:"xBairro"`
	EndNac  *Endereco `xml:"endNac"`
	EndExt  *EndExt   `xml:"endExt"`
}

type EndExt struct {
	XCidade  string `xml:"xCidade"`
	CPais    string `xml:"cPais"`
	CEndPost string `xml:"cEndPost"`
}

// --- SERVICO ---
type Prestador struct {
	RegTrib RegTrib `xml:"regTrib"`
}

type RegTrib struct {
	OpSimpNac   int `xml:"opSimpNac"`
	RegApTribSN int `xml:"regApTribSN"`
	RegEspTrib  int `xml:"regEspTrib"`
}

type Servico struct {
	CServ     CServ     `xml:"cServ"`
	Obra      Obra      `xml:"obra"`
	InfoCompl InfoCompl `xml:"infoCompl"`
	LocPrest  LocPrest  `xml:"locPrest"`
	AtvEvento AtvEvento `xml:"atvEvento"`
}

type LocPrest struct {
	CLocPrestacao  string `xml:"cLocPrestacao"`
	CPaisPrestacao string `xml:"cPaisPrestacao"`
	UF             string `xml:"UF"`
	Pais           string `xml:"pais"`
}

type CServ struct {
	CTribNac  string `xml:"cTribNac"`
	CTribMun  string `xml:"cTribMun"`
	XDescServ string `xml:"xDescServ"`
	CNBS      string `xml:"cNBS"`
}

type Obra struct {
	CObra        string `xml:"cObra"`
	InscImobFisc string `xml:"inscImobFisc"`
}

type InfoCompl struct {
	XInfComp string    `xml:"xInfComp"`
	IdDocTec string    `xml:"idDocTec"`
	DocRef   string    `xml:"docRef"`
	XPed     string    `xml:"xPed"`
	XOutInf  string    `xml:"xOutInf"`
	GItemPed []ItemPed `xml:"gItemPed"`
}

type ItemPed struct {
	XItemPed string `xml:"xItemPed"`
}

type AtvEvento struct {
	IdAtvEvt string `xml:"idAtvEvt"`
}

type Subst struct {
	ChSubstda string `xml:"chSubstda"`
}

type Valores struct {
	VLiq       float64  `xml:"vLiq"`
	VBC        float64  `xml:"vBC"`
	VISSQN     float64  `xml:"vISSQN"`
	PAliqAplic *float64 `xml:"pAliqAplic"`
	VCalcBM    float64  `xml:"vCalcBM"`
	TpBM       int      `xml:"tpBM"`
	VCalcDR    float64  `xml:"vCalcDR"`
	VTotalRet  float64  `xml:"vTotalRet"`
}

type ValoresDPS struct {
	VServPrest      VServPrest      `xml:"vServPrest"`
	VDescCondIncond VDescCondIncond `xml:"vDescCondIncond"`
	VDedRed         VDedRed         `xml:"vDedRed"`
	Trib            Trib            `xml:"trib"`
}

type VServPrest struct {
	VServ float64 `xml:"vServ"`
}

type VDescCondIncond struct {
	VDescCond   float64 `xml:"vDescCond"`
	VDescIncond float64 `xml:"vDescIncond"`
}

type VDedRed struct {
	PDR float64 `xml:"pDR"`
	VDR float64 `xml:"vDR"`
}

type Trib struct {
	TribMun *TribMun `xml:"tribMun"`
	TribFed *TribFed `xml:"tribFed"`
	TotTrib TotTrib  `xml:"totTrib"`
}

type TribMun struct {
	TribISSQN   int      `xml:"tribISSQN"`
	TpRetISSQN  int      `xml:"tpRetISSQN"`
	CPaisResult string   `xml:"cPaisResult"`
	TpImunidade int      `xml:"tpImunidade"`
	ExigSusp    ExigSusp `xml:"exigSusp"`
	BM          BM       `xml:"BM"`
}

type ExigSusp struct {
	TpSusp    int    `xml:"tpSusp"`
	NProcesso string `xml:"nProcesso"`
}

type BM struct {
	NBM      string  `xml:"nBM"`
	VRedBCBM float64 `xml:"vRedBCBM"`
}

type TribFed struct {
	VRetIRRF  float64    `xml:"vRetIRRF"`
	VRetCSLL  float64    `xml:"vRetCSLL"`
	VRetCP    float64    `xml:"vRetCP"`
	PisCofins *PisCofins `xml:"piscofins"`
}

type PisCofins struct {
	VBCPisCofins   float64 `xml:"vBCPisCofins"`
	VCofins        float64 `xml:"vCofins"`
	VPis           float64 `xml:"vPis"`
	TpRetPisCofins int     `xml:"tpRetPisCofins"`
}

type TotTrib struct {
	VTotTrib VTotTrib `xml:"vTotTrib"`
	PTotTrib PTotTrib `xml:"pTotTrib"`
}

type VTotTrib struct {
	VTotTribFed float64 `xml:"vTotTribFed"`
	VTotTribEst float64 `xml:"vTotTribEst"`
	VTotTribMun float64 `xml:"vTotTribMun"`
}

type PTotTrib struct {
	PTotTribFed float64 `xml:"pTotTribFed"`
	PTotTribEst float64 `xml:"pTotTribEst"`
	PTotTribMun float64 `xml:"pTotTribMun"`
}

type IBSCBSDPS struct {
	FinNFSe *int          `xml:"finNFSe"`
	CIndOp  string        `xml:"cIndOp"`
	Imovel  *Imovel       `xml:"imovel"`
	Dest    *Destinatario `xml:"dest"`
	Valores ValoresIBSCBS `xml:"valores"`
}

type IBSCBS struct {
	XLocalidadeIncid string            `xml:"xLocalidadeIncid"`
	UF               string            `xml:"uf"`
	CLocalidadeIncid string            `xml:"cLocalidadeIncid"`
	Valores          ValoresIBSCBSResp `xml:"valores"`
	TotCIBS          TotCIBS           `xml:"totCIBS"`
}

type ValoresIBSCBS struct {
	Trib TribIBSCBS `xml:"trib"`
}

type TribIBSCBS struct {
	GIBSCBS GIBSCBS `xml:"gIBSCBS"`
}

type GIBSCBS struct {
	CST        string `xml:"CST"`
	CClassTrib string `xml:"cClassTrib"`
}

type ValoresIBSCBSResp struct {
	VBC            float64    `xml:"vBC"`
	VCalcReeRepRes float64    `xml:"vCalcReeRepRes"`
	UF             AliqIBSUF  `xml:"uf"`
	Mun            AliqIBSMun `xml:"mun"`
	Fed            AliqCBS    `xml:"fed"`
}

type AliqIBSUF struct {
	PIBSUF      *float64 `xml:"pIBSUF"`
	PAliqEfetUF *float64 `xml:"pAliqEfetUF"`
	PRedAliqUF  *float64 `xml:"pRedAliqUF"`
}

type AliqIBSMun struct {
	PIBSMun      *float64 `xml:"pIBSMun"`
	PAliqEfetMun *float64 `xml:"pAliqEfetMun"`
	PRedAliqMun  *float64 `xml:"pRedAliqMun"`
}

type AliqCBS struct {
	PCBS         *float64 `xml:"pCBS"`
	PAliqEfetCBS *float64 `xml:"pAliqEfetCBS"`
	PRedAliqCBS  *float64 `xml:"pRedAliqCBS"`
}

type TotCIBS struct {
	GIBS   GIBSTot `xml:"gIBS"`
	GCBS   GCBSTot `xml:"gCBS"`
	VTotNF float64 `xml:"vTotNF"`
}

type GIBSTot struct {
	VIBSTot    float64    `xml:"vIBSTot"`
	GIBSMunTot GIBSMunTot `xml:"gIBSMunTot"`
	GIBSUFTot  GIBSUFTot  `xml:"gIBSUFTot"`
}

type GIBSMunTot struct {
	VIBSMun *float64 `xml:"vIBSMun"`
}

type GIBSUFTot struct {
	VIBSUF *float64 `xml:"vIBSUF"`
}

type GCBSTot struct {
	VCBS float64 `xml:"vCBS"`
}

type Imovel struct {
	InscImobFisc string `xml:"inscImobFisc"`
}
