package com.dcriar.exception.custom;

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

    private ExclusaoLoteBloqueadaException(String mensagem) {
        super(mensagem);
    }

    public static ExclusaoLoteBloqueadaException arvoreComAlteracoesAtivas(ContextoExclusaoLoteBloqueada contexto) {
        return new ExclusaoLoteBloqueadaException(
                String.format(
                        "Não é possível excluir o lote #%d porque a árvore de retalhos ainda possui alterações ativas. " +
                                "Itens que exigem ação antes da exclusão: %s.",
                        contexto.loteRaizId(),
                        formatarItensBloqueados(contexto.itensBloqueados())
                )
        );
    }

    public static ExclusaoLoteBloqueadaException retalhoNaoPodeSerExcluidoManualmente(ContextoExclusaoLoteBloqueada contexto) {
        return new ExclusaoLoteBloqueadaException(
                String.format(
                        "Não é possível excluir manualmente o retalho #%d. " +
                                "Retalhos representam material real e devem ser tratados por perda/descarte, " +
                                "ou removidos apenas pela reversão completa da árvore que os originou. " +
                                "Subárvore relacionada: %s.",
                        contexto.loteRaizId(),
                        formatarItensBloqueados(contexto.itensBloqueados())
                )
        );
    }

    private static String formatarItensBloqueados(List<ItemBloqueioLote> itensBloqueados) {
        return itensBloqueados.stream()
                .map(item -> String.format(
                        "[Lote #%d | Cadeia: %s | Ordens relacionadas: %s | Motivo: %s]",
                        item.loteId(),
                        formatarCadeiaRetalhos(item.cadeiaRetalhos()),
                        formatarOrdensRelacionadas(item.ordensRelacionadasIds()),
                        formatarDetalheUsoAtivo(item.tipoAlteracaoAtiva(), item.ordemConsumidoraAtivaId())
                ))
                .collect(Collectors.joining("; "));
    }

    private static String formatarCadeiaRetalhos(List<CadeiaRetalhoItem> cadeiaRetalhos) {
        return cadeiaRetalhos.stream()
                .map(item -> item.ordemDeProducaoOrigemId() == null
                        ? String.format("Lote raiz #%d", item.loteId())
                        : String.format("Retalho #%d (OP #%d)", item.loteId(), item.ordemDeProducaoOrigemId()))
                .collect(Collectors.joining(" -> "));
    }

    private static String formatarOrdensRelacionadas(List<Long> ordensRelacionadasIds) {
        if (ordensRelacionadasIds.isEmpty()) {
            return "nenhuma";
        }

        return ordensRelacionadasIds.stream()
                .map(id -> "OP #" + id)
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
            List<ItemBloqueioLote> itensBloqueados
    ) {
    }

    public record ItemBloqueioLote(
            Long loteId,
            List<CadeiaRetalhoItem> cadeiaRetalhos,
            List<Long> ordensRelacionadasIds,
            TipoMovimentacao tipoAlteracaoAtiva,
            Long ordemConsumidoraAtivaId
    ) {
    }

    public record CadeiaRetalhoItem(
            Long loteId,
            Long ordemDeProducaoOrigemId
    ) {
    }
}
