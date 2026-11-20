## Exemplos

### Cálculo com observabilidade

A operação abaixo tem quatro itens: arroz da cesta básica (redução de 100%), cerveja com Imposto Seletivo (a base de CBS/IBS é derivada da base do IS + IS), serviço de educação (redução de 60%) e um item com erro proposital (NCM não vinculada à cClassTrib).

**Entrada** (`operacao.json`):

```json
{
  "id": "6194602ea71cbf9431c236de4409d920",
  "versao": "1.0.0",
  "dhFatoGerador": "2027-03-10T10:00:00-03:00",
  "municipio": 4314902,
  "uf": "RS",
  "itens": [
    {
      "numero": 1, "ncm": "10063021", "cst": "200", "cClassTrib": "200003",
      "baseCalculo": 100.00, "quantidade": 20, "unidade": "KG",
      "aliquotasNominais": { "cbs": 8.8, "ibsEstadual": 0.05, "ibsMunicipal": 0.05 }
    },
    {
      "numero": 2, "ncm": "22030000", "cst": "000", "cClassTrib": "000001",
      "quantidade": 12, "unidade": "LT",
      "impostoSeletivo": {
        "cst": "000", "cClassTrib": "000001", "baseCalculo": 150.00,
        "quantidade": 12, "unidade": "LT", "impostoInformado": 15.00
      },
      "aliquotasNominais": {
        "cbs": 8.8, "ibsEstadual": 0.05, "ibsMunicipal": 0.05,
        "impostoSeletivo": { "adValorem": 10.0 }
      }
    },
    {
      "numero": 3, "nbs": "122011200", "cst": "200", "cClassTrib": "200028",
      "baseCalculo": 1200.00,
      "aliquotasNominais": { "cbs": 8.8, "ibsEstadual": 0.05, "ibsMunicipal": 0.05 }
    },
    {
      "numero": 4, "ncm": "02013000", "cst": "200", "cClassTrib": "200035",
      "baseCalculo": 80.00,
      "aliquotasNominais": { "cbs": 8.8, "ibsEstadual": 0.05, "ibsMunicipal": 0.05 }
    }
  ]
}
```

Envio da operação para cálculo com observabilidade:

```bash
curl -X POST http://localhost:8080/api/calculadora/observabilidade/regime-geral \
  -H 'Content-Type: application/json' -d @operacao.json
```

Resposta (resumida; cada grupo traz também a `memoriaCalculo` em texto):

```json
{
  "objetos": [
    { "nObj": 1, "estadoItem": "CALCULO_SIMULADO", "tribCalc": { "IBSCBS": { "...": "..." } } },
    { "nObj": 2, "estadoItem": "CALCULO_SIMULADO", "tribCalc": { "IS": { "...": "..." }, "IBSCBS": { "...": "..." } } },
    {
      "nObj": 3,
      "estadoItem": "CALCULO_SIMULADO",
      "tribCalc": { "IBSCBS": { "CST": "200", "cClassTrib": "200028", "gIBSCBS": {
        "vBC": "1200.00",
        "gIBSUF":  { "pIBSUF": "0.05",  "gRed": { "pRedAliq": "60.00", "pAliqEfet": "0.02" }, "vIBSUF": "0.24" },
        "gIBSMun": { "pIBSMun": "0.05", "gRed": { "pRedAliq": "60.00", "pAliqEfet": "0.02" }, "vIBSMun": "0.24" },
        "vIBS": "0.48",
        "gCBS":    { "pCBS": "8.80",    "gRed": { "pRedAliq": "60.00", "pAliqEfet": "3.52" }, "vCBS": "42.24" }
      } } }
    },
    {
      "nObj": 4,
      "estadoItem": "INCONSISTENCIA_ENTRADA",
      "erros": [{
        "code": "REG-011",
        "title": "NCM não vinculada",
        "pointer": "#/itens/3/ncm",
        "detail": "NCM de código 02013000 não vinculada à Classificação Tributária de código 200035 (CBS e IBS)"
      }]
    }
  ],
  "total": { "tribCalc": {
    "ISTot": { "vIS": "15.00" },
    "IBSCBSTot": { "vBCIBSCBS": "1465.00", "gIBS": { "vIBS": "0.64" }, "gCBS": { "vCBS": "56.76" } }
  } }
}
```

- A resposta é HTTP 200 mesmo com itens em erro. Por isso, verifique o `estadoItem` de cada item: `CALCULADO`, `CALCULO_SIMULADO`, `INCONSISTENCIA_ENTRADA`, `NAO_IMPLEMENTADO` ou `FALHA_TECNICA`.
- Os totais somam apenas os itens calculados.
- Enviada para `POST /regime-geral`, a mesma operação é rejeitada inteira com HTTP 422 por causa do item 4.
- A partir de 2027, `aliquotasNominais` é obrigatório; antes disso, é proibido. Os cálculos com alíquotas informadas saem como `CALCULO_SIMULADO`, e o header `x-warning-dados-simulados` indica o motivo.
- O campo `dataHoraEmissao` está obsoleto; use `dhFatoGerador`.

### Base de cálculo unificada (NF-e/NFC-e)

Os componentes ficam no mesmo nível dos demais campos, e não dentro de um objeto aninhado. `tipoDocumento` aceita `55` (NF-e) ou `65` (NFC-e), e `natureza` aceita `bem` ou `servico`.

**Entrada** (`base.json`):

```json
{
  "anoFatoGerador": 2027,
  "tipoDocumento": 55,
  "natureza": "bem",
  "vProd": 1000.00,
  "vFrete": 50.00,
  "vSeg": 20.00,
  "vOutro": 10.00,
  "vDesc": 50.00,
  "vIS": 30.00,
  "vPIS": 9.25,
  "vCOFINS": 46.25,
  "vICMS": 180.00,
  "vFCP": 10.00
}
```

Envio da entrada para cálculo da base unificada:

```bash
curl -X POST http://localhost:8080/api/calculadora/base-calculo/base-calculo-unificada \
  -H 'Content-Type: application/json' -d @base.json
```

Resposta (os avisos indicam campos ignorados no ano informado):

```json
{
  "baseRG": 870.00000000,
  "baseIS": 840.00000000,
  "baseSN": 1060.00000000,
  "avisos": [
    "Campo vPIS não compõe as bases para o ano 2027 (vigência 2026–2026) e foi ignorado no cálculo",
    "Campo vCOFINS não compõe as bases para o ano 2027 (vigência 2026–2026) e foi ignorado no cálculo"
  ]
}
```

A API aplica a vigência de cada componente ao ano informado. Em 2027, por exemplo, ICMS e FCP ainda são deduzidos da base do regime geral (`baseRG`), mas PIS e COFINS deixam de existir e são ignorados, com aviso.

### Consultas

Consultas aos dados abertos (`B` é a URL base da API):

```bash
B=http://localhost:8080/api/calculadora/dados-abertos
curl "$B/classificacoes-tributarias/cbs-ibs/class-trib/200028?data=2027-01-01"
curl "$B/fundamentacoes-legais?data=2027-01-01"
curl "$B/ncm?ncm=22030000&data=2027-01-01"
curl "$B/aliquota-uf?codigoUf=43&data=2026-06-01"
```

As alíquotas de referência estão cadastradas apenas para 2026. A partir de 2027, a consulta retorna 404, e as alíquotas devem ser informadas em `aliquotasNominais`.

### Geração e validação de XML

Geração e validação do XML do grupo de tributação a partir da resposta de `POST /regime-geral`:

```bash
C=http://localhost:8080/api/calculadora
# resultado.json: resposta de POST /regime-geral (não a da observabilidade)
curl -X POST "$C/xml/generate?tipo=nfe" -H 'Content-Type: application/json' -d @resultado.json -o grupo.xml
curl -X POST "$C/xml/validate?tipo=nfe&subtipo=grupo" -H 'Content-Type: application/xml' --data-binary @grupo.xml
```

A validação retorna `true` quando o XML é válido; caso contrário, retorna HTTP 422 com a linha e a coluna do erro. O parâmetro `subtipo` aceita `grupo` (apenas os grupos de tributação) ou `nota` (documento completo). `GET /xml/generate` e `GET /xml/validate` listam os tipos de documento aceitos.
