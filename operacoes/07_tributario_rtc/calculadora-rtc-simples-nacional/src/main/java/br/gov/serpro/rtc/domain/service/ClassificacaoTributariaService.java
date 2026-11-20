package br.gov.serpro.rtc.domain.service;

import java.time.LocalDate;
import java.sql.Date;
import java.util.List;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import br.gov.serpro.rtc.domain.model.dto.ClassificacaoTributariaDTO;
import br.gov.serpro.rtc.domain.service.exception.ClassificacaoTributariaNaoEncontradaException;
import lombok.RequiredArgsConstructor;

/** Suporte mínimo de consulta utilizado pela validação de nomenclatura. */
@Service
@RequiredArgsConstructor
public class ClassificacaoTributariaService {
    private final JdbcTemplate jdbcTemplate;

    public ClassificacaoTributariaDTO buscarClassificacaoTributariaCbsIbs(String codigo, LocalDate data) {
        List<ClassificacaoTributariaDTO> resultados = jdbcTemplate.query("""
                SELECT ct.CLTR_ID, ct.CLTR_CD, ct.CLTR_NOMENCLATURA, st.SITR_CD
                FROM CLASSIFICACAO_TRIBUTARIA ct
                JOIN SITUACAO_TRIBUTARIA st ON ct.CLTR_SITR_ID = st.SITR_ID
                JOIN TRIBUTO_SITUACAO_TRIBUTARIA tst ON tst.TRST_SITR_ID = st.SITR_ID
                JOIN TRIBUTO tb ON tst.TRST_TBTO_ID = tb.TBTO_ID
                WHERE ct.CLTR_CD = ? AND tb.TBTO_ID = 2
                AND ? BETWEEN ct.CLTR_INICIO_VIGENCIA AND COALESCE(ct.CLTR_FIM_VIGENCIA, ?)
                AND ? BETWEEN st.SITR_INICIO_VIGENCIA AND COALESCE(st.SITR_FIM_VIGENCIA, ?)
                AND ? BETWEEN tst.TRST_INICIO_VIGENCIA AND COALESCE(tst.TRST_FIM_VIGENCIA, ?)
                AND ? BETWEEN tb.TBTO_INICIO_VIGENCIA AND COALESCE(tb.TBTO_FIM_VIGENCIA, ?)
                """, (rs, rowNum) -> new ClassificacaoTributariaDTO(
                        rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                codigo, Date.valueOf(data), Date.valueOf(data), Date.valueOf(data), Date.valueOf(data),
                Date.valueOf(data), Date.valueOf(data), Date.valueOf(data), Date.valueOf(data));
        ClassificacaoTributariaDTO resultado = DataAccessUtils.singleResult(resultados);
        if (resultado == null) {
            throw new ClassificacaoTributariaNaoEncontradaException(codigo, "CBS e IBS", data);
        }
        return resultado;
    }
}
