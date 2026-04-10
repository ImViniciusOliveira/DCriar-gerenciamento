package com.dcriar.exception.custom;

import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Exceção lançada quando a exclusão de um lote é bloqueada porque o próprio lote
 * ou algum descendente da sua árvore de retalhos ainda possui alteração ativa.
 */
@Getter
public class ExclusaoLoteBloqueadaException extends RuntimeException {

    private final String codigoBloqueio;
    private final Long loteRaizId;
    private final String identificadorPublicoRaiz;
    private final List<ItemBloqueioLote> itensBloqueados;

    private ExclusaoLoteBloqueadaException(
            String mensagem,
            String codigoBloqueio,
            Long loteRaizId,
            String identificadorPublicoRaiz,
            List<ItemBloqueioLote> itensBloqueados
    ) {
        super(mensagem);
        this.codigoBloqueio = codigoBloqueio;
        this.loteRaizId = loteRaizId;
        this.identificadorPublicoRaiz = identificadorPublicoRaiz;
        this.itensBloqueados = List.copyOf(itensBloqueados);
    }

    public static ExclusaoLoteBloqueadaException arvoreComAlteracoesAtivas(ContextoExclusaoLoteBloqueada contexto) {
        int quantidadeItensBloqueados = contexto.itensBloqueados().size();
        String sufixoQuantidade = quantidadeItensBloqueados == 1
                ? "Há 1 item com alteração ativa na árvore vinculada."
                : String.format("Há %s itens com alteração ativa na árvore vinculada.", HumanNumberDisplayFormatter.formatCount(quantidadeItensBloqueados));

        return new ExclusaoLoteBloqueadaException(
                String.format(
                        "Não é possível excluir o lote %s porque a árvore de retalhos ainda possui alterações ativas. %s",
                        contexto.identificadorPublicoRaiz(),
                        sufixoQuantidade
                ),
                "ARVORE_COM_ALTERACOES_ATIVAS",
                contexto.loteRaizId(),
                contexto.identificadorPublicoRaiz(),
                contexto.itensBloqueados()
        );
    }

    public static ExclusaoLoteBloqueadaException retalhoNaoPodeSerExcluidoManualmente(ContextoExclusaoLoteBloqueada contexto) {
        int quantidadeItensRelacionados = contexto.itensBloqueados().size();
        String sufixoQuantidade = quantidadeItensRelacionados == 1
                ? "Há 1 item relacionado que precisa ser tratado pela árvore de origem."
                : String.format("Há %s itens relacionados que precisam ser tratados pela árvore de origem.", HumanNumberDisplayFormatter.formatCount(quantidadeItensRelacionados));

        return new ExclusaoLoteBloqueadaException(
                String.format(
                        "Não é possível excluir manualmente o retalho %s. Retalhos representam material real e devem ser tratados por perda/descarte ou pela reversão completa da árvore que os originou. %s",
                        contexto.identificadorPublicoRaiz(),
                        sufixoQuantidade
                ),
                "RETALHO_EXCLUSAO_MANUAL_NAO_PERMITIDA",
                contexto.loteRaizId(),
                contexto.identificadorPublicoRaiz(),
                contexto.itensBloqueados()
        );
    }

    public static ExclusaoLoteBloqueadaException loteComVinculosPersistidos(ContextoExclusaoLoteBloqueada contexto) {
        int quantidadeItensRelacionados = contexto.itensBloqueados().size();
        String sufixoQuantidade = quantidadeItensRelacionados == 1
                ? "Há 1 item com vínculo persistido na árvore ou no histórico do lote."
                : String.format("Há %s itens com vínculos persistidos na árvore ou no histórico do lote.", HumanNumberDisplayFormatter.formatCount(quantidadeItensRelacionados));

        return new ExclusaoLoteBloqueadaException(
                String.format(
                        "Não é possível excluir o lote %s porque ele ainda possui vínculos ativos ou histórico persistido associado. %s",
                        contexto.identificadorPublicoRaiz(),
                        sufixoQuantidade
                ),
                "LOTE_COM_VINCULOS_PERSISTIDOS",
                contexto.loteRaizId(),
                contexto.identificadorPublicoRaiz(),
                contexto.itensBloqueados()
        );
    }

    public String formatarIdentificadoresBloqueados() {
        return itensBloqueados.stream()
                .map(ItemBloqueioLote::identificadorPublicoLote)
                .collect(Collectors.joining(", "));
    }

    private static String formatarDetalheUsoAtivo(TipoMovimentacao tipoAlteracaoAtiva, Long ordemConsumidoraAtivaId) {
        if (tipoAlteracaoAtiva == null) {
            return "o saldo atual do lote ainda difere do saldo originalmente registrado";
        }

        return switch (tipoAlteracaoAtiva) {
            case SAIDA_PRODUCAO -> ordemConsumidoraAtivaId != null
                    ? String.format("consumo ainda ativo pela Ordem de Produção #%d", ordemConsumidoraAtivaId)
                    : "consumo ainda ativo no lote";
            case PERDA_DESCARTE -> "há perda/descarte ativo registrado no lote";
            case AJUSTE_INVENTARIO -> "há ajuste de inventário ativo registrado no lote";
            default -> "há alteração ativa registrada no lote";
        };
    }

    public record ContextoExclusaoLoteBloqueada(
            Long loteRaizId,
            String identificadorPublicoRaiz,
            List<ItemBloqueioLote> itensBloqueados
    ) {
    }

    public record ItemBloqueioLote(
            Long loteId,
            String identificadorPublicoLote,
            List<CadeiaRetalhoItem> cadeiaRetalhos,
            List<Long> ordensRelacionadasIds,
            TipoMovimentacao tipoAlteracaoAtiva,
            Long ordemConsumidoraAtivaId
    ) {
        public String resumoDetalhado() {
            return String.format(
                    "[%s | Cadeia: %s | Ordens relacionadas: %s | Motivo: %s]",
                    identificadorPublicoLote,
                    formatarCadeiaRetalhos(),
                    formatarOrdensRelacionadas(),
                    formatarDetalheUsoAtivo(tipoAlteracaoAtiva, ordemConsumidoraAtivaId)
            );
        }

        public String formatarCadeiaRetalhos() {
            return cadeiaRetalhos.stream()
                    .map(CadeiaRetalhoItem::descricao)
                    .collect(Collectors.joining(" -> "));
        }

        public String formatarOrdensRelacionadas() {
            if (ordensRelacionadasIds.isEmpty()) {
                return "nenhuma";
            }

            return ordensRelacionadasIds.stream()
                    .map(id -> "OP #" + id)
                    .collect(Collectors.joining(", "));
        }

        public String formatarMotivo() {
            return formatarDetalheUsoAtivo(tipoAlteracaoAtiva, ordemConsumidoraAtivaId);
        }
    }

    public record CadeiaRetalhoItem(
            Long loteId,
            String identificadorPublicoLote,
            Long ordemDeProducaoOrigemId
    ) {
        public String descricao() {
            return ordemDeProducaoOrigemId == null
                    ? String.format("%s (lote raiz)", identificadorPublicoLote)
                    : String.format("%s (OP #%d)", identificadorPublicoLote, ordemDeProducaoOrigemId);
        }
    }
}
