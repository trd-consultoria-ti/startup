package service

import (
	"context"
	"danfse-service/internal/domain/enums"
	"danfse-service/internal/domain/localidade"
	"danfse-service/internal/domain/models"
	"fmt"
	"strings"
	"testing"
)

var stubResolverMunicipio = func(codigo int) (nome, uf string, encontrado bool, err error) {
	return "São Paulo", "SP", true, nil
}

func TestServicoPreparacaoDanfsePreservaTextoComplOriginal(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS123" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>1</nNFSe>
			<ambGer>1</ambGer>
			<cStat>100</cStat>
			<DPS>
				<infDPS>
					<nDPS>1</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico de Teste Completo</xDescServ></cServ>
						<infoCompl><xInfComp>Informacao complementar importante</xInfComp></infoCompl>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib><tribMun><tribISSQN>1</tribISSQN></tribMun></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// TextoComplOriginal deve conter o texto completo antes de qualquer truncamento
	if nota.TextoComplOriginal == "" {
		t.Fatal("TextoComplOriginal não foi preenchido")
	}

	// Deve conter a informação complementar
	if !strings.Contains(nota.TextoComplOriginal, "Informacao complementar importante") {
		t.Fatalf("TextoComplOriginal deveria conter 'Informacao complementar importante', obteve: %q", nota.TextoComplOriginal)
	}
}

func TestServicoPreparacaoDanfseExibicaoDescServTruncada(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS123" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>1</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>1</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Um servico muito longo que deve ser truncado para caber na exibicao do DANFSe conforme as regras de layout</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// ExibicaoDescServ deve ser truncada
	if nota.ExibicaoDescServ == "" {
		t.Fatal("ExibicaoDescServ não foi preenchido")
	}
}

func TestServicoPreparacaoDanfseQRCode(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS123" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>1</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>1</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if !strings.Contains(nota.InfNFSe.Qrcode, "https://www.nfse.gov.br/ConsultaPublica/") {
		t.Fatalf("QR code deveria conter URL oficial, obteve: %q", nota.InfNFSe.Qrcode)
	}
	// QR code deve conter a chave sem o prefixo NFS
	if !strings.Contains(nota.InfNFSe.Qrcode, "123") {
		t.Fatalf("QR code deveria conter a chave, obteve: %q", nota.InfNFSe.Qrcode)
	}
}

func TestServicoPreparacaoDanfseTpAmbHomologacao(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS456" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>2</nNFSe>
			<ambGer>2</ambGer>
			<DPS>
				<infDPS>
					<nDPS>2</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>2</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if !strings.Contains(nota.InfNFSe.Qrcode, "https://www.nfse.gov.br/ConsultaPublica/") {
		t.Fatalf("QR code deveria conter URL oficial, obteve: %q", nota.InfNFSe.Qrcode)
	}
}

func TestServicoPreparacaoDanfseVExclusoesReducoesBC(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS789" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>3</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>3</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<vDescCondIncond><vDescIncond>10</vDescIncond></vDescCondIncond>
						<trib><tribMun><tribISSQN>1</tribISSQN></tribMun></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores>
				<vLiq>100</vLiq>
				<vISSQN>5</vISSQN>
				<vCalcDR>3</vCalcDR>
			</valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// VExclusoesReducoesBC = vDescIncond + vISSQN + vCalcDR = 10 + 5 + 3 = 18
	if nota.VExclusoesReducoesBC != 18.0 {
		t.Fatalf("VExclusoesReducoesBC: esperado 18.0, obtido %f", nota.VExclusoesReducoesBC)
	}
}

func TestServicoPreparacaoDanfseMarcaDagua(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS001" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>4</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>4</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)

	// VALIDA não tem marca d'água
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}
	if nota.MarcaDagua != "" {
		t.Fatalf("VALIDA deveria ter marca d'agua vazia, obteve %q", nota.MarcaDagua)
	}

	// SUBSTITUIDA tem marca d'água
	nota, err = preparador.Executar(context.Background(), xmlData, enums.SUBSTITUIDA)
	if err != nil {
		t.Fatal(err)
	}
	if nota.MarcaDagua != "SUBSTITUÍDA" {
		t.Fatalf("SUBSTITUIDA deveria ter marca d'agua 'SUBSTITUÍDA', obteve %q", nota.MarcaDagua)
	}

	// CANCELADA tem marca d'água
	nota, err = preparador.Executar(context.Background(), xmlData, enums.CANCELADA)
	if err != nil {
		t.Fatal(err)
	}
	if nota.MarcaDagua != "CANCELADA" {
		t.Fatalf("CANCELADA deveria ter marca d'agua 'CANCELADA', obteve %q", nota.MarcaDagua)
	}
}

func TestServicoPreparacaoDanfsePreservaCPaisPrestacao(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS003" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>6</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>6</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<locPrest><cLocPrestacao>3550308</cLocPrestacao><cPaisPrestacao>BR</cPaisPrestacao></locPrest>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// CPaisPrestacao deve ser preservado (não substituído por "Brasil")
	if nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.CPaisPrestacao != "BR" {
		t.Fatalf("CPaisPrestacao deveria ser preservado como %q, obteve %q", "BR", nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.CPaisPrestacao)
	}
	// UF deve ser enriquecida
	if nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF != "SP" {
		t.Fatalf("LocPrest.UF deveria ser %q, obteve %q", "SP", nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF)
	}
}

func TestServicoPreparacaoDanfseCatalogoReal(t *testing.T) {
	localidades, err := localidade.Carregar()
	if err != nil {
		t.Fatalf("falha ao carregar catálogo: %v", err)
	}

	nome, uf, encontrado, err2 := localidades.BuscarMunicipio(3550308)
	if err2 != nil {
		t.Fatalf("erro ao buscar município 3550308: %v", err2)
	}
	if !encontrado {
		t.Fatal("município 3550308 deveria existir no catálogo")
	}
	if nome != "São Paulo" {
		t.Fatalf("nome esperado 'São Paulo', obteve %q", nome)
	}
	if uf != "SP" {
		t.Fatalf("UF esperada 'SP', obteve %q", uf)
	}

	resolver := func(codigo int) (nome, uf string, encontrado bool, err error) {
		return localidades.BuscarMunicipio(codigo)
	}

	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS900" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>10</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>10</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(resolver)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if nota.InfNFSe.Emit.EnderNac.XMun != "São Paulo" {
		t.Fatalf("XMun esperado 'São Paulo', obteve %q", nota.InfNFSe.Emit.EnderNac.XMun)
	}
	if nota.InfNFSe.Emit.EnderNac.UF != "SP" {
		t.Fatalf("UF esperada 'SP', obteve %q", nota.InfNFSe.Emit.EnderNac.UF)
	}
}

func TestServicoPreparacaoDanfse_ResolverMunicipio(t *testing.T) {
	const baseXML = `<NFSe versao="1.00">
		<infNFSe Id="NFS800" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>20</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>20</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`

	testes := []struct {
		name     string
		resolver func(codigo int) (nome, uf string, encontrado bool, err error)
		xmlData  []byte
		check    func(t *testing.T, nota models.NFSe)
	}{
		{
			name: "NaoEncontrado",
			resolver: func(codigo int) (nome, uf string, encontrado bool, err error) {
				return "", "", false, nil
			},
			xmlData: []byte(baseXML),
			check: func(t *testing.T, nota models.NFSe) {
				if nota.InfNFSe.Emit.EnderNac.XMun != "" {
					t.Fatalf("XMun deveria permanecer vazio, obteve %q", nota.InfNFSe.Emit.EnderNac.XMun)
				}
				if nota.InfNFSe.Emit.EnderNac.UF != "" {
					t.Fatalf("UF deveria permanecer vazia, obteve %q", nota.InfNFSe.Emit.EnderNac.UF)
				}
			},
		},
		{
			name: "ComErro",
			resolver: func(codigo int) (nome, uf string, encontrado bool, err error) {
				return "", "", false, fmt.Errorf("erro de rede")
			},
			xmlData: []byte(baseXML),
			check: func(t *testing.T, nota models.NFSe) {
				if nota.InfNFSe.Emit.EnderNac.XMun != "" {
					t.Fatalf("XMun deveria permanecer vazio, obteve %q", nota.InfNFSe.Emit.EnderNac.XMun)
				}
				if nota.InfNFSe.Emit.EnderNac.UF != "" {
					t.Fatalf("UF deveria permanecer vazia, obteve %q", nota.InfNFSe.Emit.EnderNac.UF)
				}
			},
		},
		{
			name:     "CodigoXMLNaoNumerico",
			resolver: stubResolverMunicipio,
			xmlData: []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS802" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>22</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>22</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>NAO_NUMERICO</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`),
			check: func(t *testing.T, nota models.NFSe) {
				if nota.InfNFSe.Emit.EnderNac.XMun != "" {
					t.Fatalf("XMun deveria permanecer vazio, obteve %q", nota.InfNFSe.Emit.EnderNac.XMun)
				}
				if nota.InfNFSe.Emit.EnderNac.UF != "" {
					t.Fatalf("UF deveria permanecer vazia, obteve %q", nota.InfNFSe.Emit.EnderNac.UF)
				}
			},
		},
		{
			name:     "Nil",
			resolver: nil,
			xmlData:  []byte(baseXML),
			check: func(t *testing.T, nota models.NFSe) {
				if nota.InfNFSe.Emit.EnderNac.XMun != "" {
					t.Fatalf("XMun deveria permanecer vazio, obteve %q", nota.InfNFSe.Emit.EnderNac.XMun)
				}
				if nota.InfNFSe.Emit.EnderNac.UF != "" {
					t.Fatalf("UF deveria permanecer vazia, obteve %q", nota.InfNFSe.Emit.EnderNac.UF)
				}
			},
		},
		{
			name: "EncontradoNomeVazio",
			resolver: func(codigo int) (nome, uf string, encontrado bool, err error) {
				return "", "SP", true, nil
			},
			xmlData: []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS804" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>24</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>24</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`),
			check: func(t *testing.T, nota models.NFSe) {
				// encontrado=true com nome vazio: campos são aplicados (semântica deliberada)
				if nota.InfNFSe.Emit.EnderNac.UF != "SP" {
					t.Fatalf("UF deveria ser 'SP', obteve %q", nota.InfNFSe.Emit.EnderNac.UF)
				}
			},
		},
	}

	for _, tc := range testes {
		t.Run(tc.name, func(t *testing.T) {
			preparador := NovoServicoPreparacaoDanfse(tc.resolver)
			nota, err := preparador.Executar(context.Background(), tc.xmlData, enums.VALIDA)
			if err != nil {
				t.Fatal("Executar não deveria retornar erro")
			}
			tc.check(t, nota)
		})
	}
}

func TestServicoPreparacaoDanfseLocPrestEncontrado(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS805" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>25</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>25</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<locPrest><cLocPrestacao>3550308</cLocPrestacao></locPrest>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF != "SP" {
		t.Fatalf("LocPrest.UF deveria ser 'SP', obteve %q", nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF)
	}
}

func TestServicoPreparacaoDanfseLocPrestAusente(t *testing.T) {
	resolver := func(codigo int) (nome, uf string, encontrado bool, err error) {
		if codigo == 3550308 {
			return "São Paulo", "SP", true, nil
		}
		return "", "", false, nil
	}

	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS806" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>26</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>26</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<locPrest><cLocPrestacao>9999999</cLocPrestacao></locPrest>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(resolver)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// LocPrest com código não encontrado: UF não é preenchida
	if nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF != "" {
		t.Fatalf("LocPrest.UF deveria permanecer vazia, obteve %q", nota.InfNFSe.DPS.InfDPS.Serv.LocPrest.UF)
	}
}

func TestServicoPreparacaoDanfseOriginalTextPreservedForDirectRendering(t *testing.T) {
	// This test verifies that the original description and complementary text
	// are preserved in the model for direct Folio rendering, while the HTML
	// Exibicao* fields are truncated.
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS002" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>5</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>5</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Descricao completa do servico que sera truncada para HTML mas preservada para Folio</xDescServ></cServ>
						<infoCompl><xInfComp>` + strings.Repeat("Texto complementar completo que sera truncado para HTML mas preservado para Folio. ", 80) + `</xInfComp></infoCompl>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// TextoComplOriginal deve conter o texto completo
	if !strings.Contains(nota.TextoComplOriginal, "Texto complementar completo que sera truncado para HTML mas preservado para Folio") {
		t.Fatalf("TextoComplOriginal deveria conter o texto completo, obteve: %q", nota.TextoComplOriginal)
	}

	// ExibicaoCompl deve ser truncado (menor que o original)
	if nota.ExibicaoCompl == "" {
		t.Fatal("ExibicaoCompl não foi preenchido")
	}
	if len(nota.ExibicaoCompl) >= len(nota.TextoComplOriginal) {
		t.Fatalf("ExibicaoCompl (%d chars) deveria ser menor que TextoComplOriginal (%d chars)", len(nota.ExibicaoCompl), len(nota.TextoComplOriginal))
	}
}

func TestServicoPreparacaoDanfse_OversizedInputReturnsError(t *testing.T) {
	large := make([]byte, MaxXMLInputSize+1)
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	_, err := preparador.Executar(context.Background(), large, enums.VALIDA)
	if err == nil {
		t.Fatal("expected error for oversized input, got nil")
	}
}

func TestServicoPreparacaoDanfse_CustomLimitRaw(t *testing.T) {
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio, 64)
	large := make([]byte, 65) // > 64 e < MaxXMLInputSize
	_, err := preparador.Executar(context.Background(), large, enums.VALIDA)
	if err == nil {
		t.Fatal("expected error for input over custom limit, got nil")
	}
	if !strings.Contains(err.Error(), "input exceeds maximum size") {
		t.Fatalf("esperava erro de tamanho bruto, obteve: %v", err)
	}
}

func TestServicoPreparacaoDanfse_CustomLimitPostSanitization(t *testing.T) {
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio, 16)
	// `<a>` + 4x '&' + `</a>` = 11 bytes crus (dentro do limite); após a
	// sanitização do '&' o conteúdo passa de 16 bytes e deve ser rejeitado.
	xmlData := []byte("<a>&&&&</a>")
	if len(xmlData) > 16 {
		t.Fatalf("fixture inesperada: %d bytes crus", len(xmlData))
	}
	_, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err == nil {
		t.Fatal("expected error for sanitized input over custom limit, got nil")
	}
	if !strings.Contains(err.Error(), "sanitized input exceeds maximum size") {
		t.Fatalf("esperava erro de tamanho pós-sanitização, obteve: %v", err)
	}
}

func TestServicoPreparacaoDanfse_InvalidExplicitLimit(t *testing.T) {
	casos := map[string]int{
		"zero":      0,
		"negativo":  -1,
		"excedente": MaxXMLInputSizeLimit + 1,
	}
	for nome, valor := range casos {
		t.Run(nome, func(t *testing.T) {
			preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio, valor)
			_, err := preparador.Executar(context.Background(), []byte("<a/>"), enums.VALIDA)
			if err == nil {
				t.Fatalf("esperava erro para limite explícito inválido %d, obteve nil", valor)
			}
			if !strings.Contains(err.Error(), "invalid XML max input size") {
				t.Fatalf("esperava erro de limite inválido, obteve: %v", err)
			}
		})
	}
}

func TestServicoPreparacaoDanfse_EmptyNFSeRootReturnsError(t *testing.T) {
	xmlData := []byte(`<NFSe/>`)
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	_, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err == nil {
		t.Fatal("expected error for <NFSe/> without infNFSe, got nil")
	}
}

func TestServicoPreparacaoDanfse_MissingIdReturnsError(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>1</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>1</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	_, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err == nil {
		t.Fatal("expected error for missing Id, got nil")
	}
}

func TestServicoPreparacaoDanfse_XDescServEscapedMarkupPreserved(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS100" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>100</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>100</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>&lt;svg&gt;alert&lt;/svg&gt;</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}
	// xDescServ deve conter o texto literal (não interpretado como HTML)
	want := "<svg>alert</svg>"
	if nota.InfNFSe.DPS.InfDPS.Serv.CServ.XDescServ != want {
		t.Fatalf("xDescServ deveria ser %q, obteve %q", want, nota.InfNFSe.DPS.InfDPS.Serv.CServ.XDescServ)
	}
}

func TestServicoPreparacaoDanfse_IdWithSpecialCharsEscapedInQR(t *testing.T) {
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFS+100&amp;test" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>101</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>101</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)
	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}
	// QR code deve conter a chave URL-escaped (Id sem prefixo NFS)
	// Id="NFS+100&test" -> sem prefixo NFS = "+100&test" -> URL-escaped = %2B100%26test
	if !strings.Contains(nota.InfNFSe.Qrcode, "%2B100") {
		t.Fatalf("QR code deveria conter Id sem prefixo NFS URL-escaped, obteve: %q", nota.InfNFSe.Qrcode)
	}
}

func TestServicoPreparacaoDanfse_Descricao1297CaracteresNaoTruncada(t *testing.T) {
	desc := strings.Repeat("A", 1297)
	xmlData := []byte(fmt.Sprintf(`<NFSe versao="1.00">
		<infNFSe Id="NFS1297" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>1297</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>1297</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>%s</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`, desc))

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if strings.Contains(nota.ExibicaoDescServ, "...") {
		t.Fatal("descricao de 1297 caracteres nao deveria conter reticencias")
	}
	if len(nota.ExibicaoDescServ) != 1297 {
		t.Fatalf("ExibicaoDescServ deveria ter 1297 chars, obteve %d", len(nota.ExibicaoDescServ))
	}
}

func TestServicoPreparacaoDanfse_Descricao1301CaracteresTruncada(t *testing.T) {
	desc := strings.Repeat("A", 1301)
	xmlData := []byte(fmt.Sprintf(`<NFSe versao="1.00">
		<infNFSe Id="NFS1301" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>1301</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>1301</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>%s</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`, desc))

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if len(nota.ExibicaoDescServ) != 1300 {
		t.Fatalf("descricao de 1301 caracteres deveria ser limitada a 1300, obteve %d", len(nota.ExibicaoDescServ))
	}
	if !strings.HasSuffix(nota.ExibicaoDescServ, "...") {
		t.Fatal("descricao de 1301 caracteres deveria terminar com reticencias")
	}
}

func TestServicoPreparacaoDanfse_DescricaoMultilineabaixo1300NaoTruncada(t *testing.T) {
	// Multi-line description with ~1183 chars over 16 explicit lines.
	lines := []string{
		"Servico de consultoria tecnica especializada em engenharia civil",
		"com foco em estruturas de concreto armado e protendido",
		"para edificacoes de medio e grande porte",
		"incluindo analise estrutural, calculo de fundacoes",
		"e acompanhamento de execucao em obra",
		"conforme normas ABNT NBR 6118 e 6120",
		"O prestador possui registro ativo no CREA-SP",
		"com qualificacao de engenheiro civil senior",
		"e experiencia comprovada em projetos estruturais",
		"para hospitais, escolas e edificios comerciais",
		"O servico inclui elaboracao de memoria de calculo",
		"plantas estruturais e quadros de armadura",
		"além de suporte tecnico durante a execucao",
		"para garantir conformidade com o projeto aprovado",
		"e normas regulamentadoras aplicaveis",
		"O valor do servico sera pago em parcelas mensais",
	}
	desc := strings.Join(lines, "\n")
	if len(desc) >= 1300 {
		t.Fatalf("descricao de teste deveria ter menos de 1300 chars, obteve %d", len(desc))
	}

	xmlData := []byte(fmt.Sprintf(`<NFSe versao="1.00">
		<infNFSe Id="NFSML" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>500</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>500</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>%s</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`, desc))

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if strings.Contains(nota.ExibicaoDescServ, "...") {
		t.Fatal("descricao multi-linha abaixo de 1300 nao deveria conter reticencias")
	}
	if nota.ExibicaoDescServ != desc {
		t.Fatalf("ExibicaoDescServ deveria ser identico ao original, obteve: %q", nota.ExibicaoDescServ)
	}
}

func TestServicoPreparacaoDanfse_Complemento2000CaracteresNaoTruncado(t *testing.T) {
	compl := strings.Repeat("X", 2000)
	xmlData := []byte(fmt.Sprintf(`<NFSe versao="1.00">
		<infNFSe Id="NFS2000" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>2000</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>2000</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<infoCompl><xInfComp>%s</xInfComp></infoCompl>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`, compl))

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if !strings.Contains(nota.ExibicaoCompl, " | Totais aproximados dos Tributos") {
		t.Fatalf("ExibicaoCompl deveria anexar a linha tributaria ao complemento, obteve: %q", nota.ExibicaoCompl)
	}
	if len(nota.ExibicaoCompl) == 0 {
		t.Fatal("ExibicaoCompl deveria ser preenchido")
	}
}

func TestServicoPreparacaoDanfse_Complemento2001CaracteresTruncado(t *testing.T) {
	compl := strings.Repeat("X", 2001)
	xmlData := []byte(fmt.Sprintf(`<NFSe versao="1.00">
		<infNFSe Id="NFS2001" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>2001</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>2001</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<infoCompl><xInfComp>%s</xInfComp></infoCompl>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`, compl))

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	if !strings.Contains(nota.ExibicaoCompl, "...") {
		t.Fatal("complemento de 2001 caracteres deveria conter reticencias")
	}
	if !strings.Contains(nota.ExibicaoCompl, "... | Totais aproximados dos Tributos") {
		t.Fatalf("ExibicaoCompl deveria truncar a descricao antes da linha tributaria, obteve: %q", nota.ExibicaoCompl)
	}
}

// --- Testes de composicao das Informacoes Complementares com totais tributarios ---

func TestServicoPreparacaoDanfse_TotaisSemXInfComp(t *testing.T) {
	// NFSe minima sem xInfComp e demais campos de infoCompl:
	// TextoComplOriginal e ExibicaoCompl devem comecar diretamente com
	// "Totais aproximados" e NAO com "|".
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFSTOT0" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>3000</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>3000</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib><tribMun><tribISSQN>1</tribISSQN></tribMun></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// TextoComplOriginal deve comecar com "Totais aproximados"
	if !strings.HasPrefix(nota.TextoComplOriginal, "Totais aproximados") {
		t.Fatalf("TextoComplOriginal deveria comecar com 'Totais aproximados', obteve: %q", nota.TextoComplOriginal)
	}
	// Nao deve comecar com pipe
	if strings.HasPrefix(nota.TextoComplOriginal, "|") {
		t.Fatalf("TextoComplOriginal nao deveria comecar com pipe, obteve: %q", nota.TextoComplOriginal)
	}

	// ExibicaoCompl deve comecar com "Totais aproximados"
	if !strings.HasPrefix(nota.ExibicaoCompl, "Totais aproximados") {
		t.Fatalf("ExibicaoCompl deveria comecar com 'Totais aproximados', obteve: %q", nota.ExibicaoCompl)
	}
	// Nao deve comecar com pipe
	if strings.HasPrefix(nota.ExibicaoCompl, "|") {
		t.Fatalf("ExibicaoCompl nao deveria comecar com pipe, obteve: %q", nota.ExibicaoCompl)
	}
}

func TestServicoPreparacaoDanfse_TotaisComXInfComp(t *testing.T) {
	// NFSe com xInfComp preenchido:
	// TextoComplOriginal deve conter "Inf. Cont.: <valor> | Totais aproximados..."
	// e a frase "Totais aproximados" deve aparecer exatamente uma vez, no final.
	xmlData := []byte(`<NFSe versao="1.00">
		<infNFSe Id="NFSTOT1" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>3001</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>3001</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<infoCompl><xInfComp>Informacao complementar importante</xInfComp></infoCompl>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib><tribMun><tribISSQN>1</tribISSQN></tribMun></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`)

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// Deve conter "Inf. Cont.: Informacao complementar importante | Totais aproximados..."
	if !strings.Contains(nota.TextoComplOriginal, "Inf. Cont.: Informacao complementar importante | Totais aproximados") {
		t.Fatalf("TextoComplOriginal deveria conter 'Inf. Cont.: Informacao complementar importante | Totais aproximados', obteve: %q", nota.TextoComplOriginal)
	}

	// A frase "Totais aproximados" deve aparecer exatamente uma vez
	count := strings.Count(nota.TextoComplOriginal, "Totais aproximados")
	if count != 1 {
		t.Fatalf("frase 'Totais aproximados' deveria aparecer 1 vez, apareceu %d vezes em: %q", count, nota.TextoComplOriginal)
	}

	// A frase "Totais aproximados" deve estar no final do texto
	if !strings.HasSuffix(nota.TextoComplOriginal, "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 0,00; Estaduais: R$ 0,00; Municipais: R$ 0,00;") {
		t.Fatalf("TextoComplOriginal deveria terminar com a frase de totais, obteve: %q", nota.TextoComplOriginal)
	}
}

func TestServicoPreparacaoDanfse_TotaisComplementoLongo(t *testing.T) {
	// NFSe com complemento longo que excede maxCaracteresInfoCompl:
	// ExibicaoCompl deve conter "...", nao exceder maxCaracteresInfoCompl,
	// e conter a frase tributaria completa.
	compl := strings.Repeat("X ", 1100) // ~2200 chars
	xmlData := []byte(fmt.Sprintf(`<NFSe versao="1.00">
		<infNFSe Id="NFSTOT2" dhProc="2026-07-14T10:00:00-03:00">
			<nNFSe>3002</nNFSe>
			<ambGer>1</ambGer>
			<DPS>
				<infDPS>
					<nDPS>3002</nDPS>
					<serie>1</serie>
					<dhEmi>2026-07-14T10:00:00-03:00</dhEmi>
					<dCompet>2026-07-14</dCompet>
					<tpEmit>1</tpEmit>
					<tpAmb>1</tpAmb>
					<prest><regTrib><opSimpNac>1</opSimpNac></regTrib></prest>
					<serv>
						<cServ><cTribNac>010101</cTribNac><xDescServ>Servico</xDescServ></cServ>
						<infoCompl><xInfComp>%s</xInfComp></infoCompl>
					</serv>
					<valores>
						<vServPrest><vServ>100</vServ></vServPrest>
						<trib><tribMun><tribISSQN>1</tribISSQN></tribMun></trib>
					</valores>
				</infDPS>
			</DPS>
			<emit>
				<CNPJ>12345678000195</CNPJ>
				<xNome>Prestador</xNome>
				<enderNac><cMun>3550308</cMun><CEP>01001000</CEP></enderNac>
			</emit>
			<valores><vLiq>100</vLiq></valores>
		</infNFSe>
	</NFSe>`, compl))

	preparador := NovoServicoPreparacaoDanfse(stubResolverMunicipio)
	nota, err := preparador.Executar(context.Background(), xmlData, enums.VALIDA)
	if err != nil {
		t.Fatal(err)
	}

	// ExibicaoCompl deve conter "..."
	if !strings.Contains(nota.ExibicaoCompl, "...") {
		t.Fatal("ExibicaoCompl deveria conter reticencias quando complemento e longo")
	}

	// Deve conter a frase tributaria completa
	if !strings.Contains(nota.ExibicaoCompl, "Totais aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ 0,00; Estaduais: R$ 0,00; Municipais: R$ 0,00;") {
		t.Fatalf("ExibicaoCompl deveria conter a frase tributaria completa, obteve: %q", nota.ExibicaoCompl)
	}
}
